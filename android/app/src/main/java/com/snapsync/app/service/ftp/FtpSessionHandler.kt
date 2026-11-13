package com.snapsync.app.service.ftp

import android.util.Log
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.StringTokenizer

class FtpSessionHandler(
    private val controlSocket: Socket,
    private val storageDir: File,
    private val expectedUser: String,
    private val expectedPass: String,
    private val localIp: String,
    private val listener: FtpEventListener?
) : Runnable {

    private val TAG = "FtpSessionHandler"
    private var isRunning = true
    private var isAuthenticated = false
    private var currentUser: String? = null

    // Data connection fields
    private var passiveServerSocket: ServerSocket? = null
    private var activeDataHost: String? = null
    private var activeDataPort: Int = -1

    override fun run() {
        val clientIp = controlSocket.inetAddress?.hostAddress ?: "Unknown"
        listener?.onClientConnected(clientIp)
        Log.i(TAG, "Camera client connected: $clientIp")

        try {
            val reader = BufferedReader(InputStreamReader(controlSocket.getInputStream()))
            val writer = BufferedWriter(OutputStreamWriter(controlSocket.getOutputStream()))

            sendResponse(writer, "220 SnapSync Live Photo Relay FTP Server Ready")

            while (isRunning && !controlSocket.isClosed) {
                val line = reader.readLine() ?: break
                Log.d(TAG, "FTP RECV << $line")
                handleCommand(line.trim(), writer, clientIp)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in FTP control session: ${e.message}")
        } finally {
            closeSession(clientIp)
        }
    }

    private fun handleCommand(cmdLine: String, writer: BufferedWriter, clientIp: String) {
        val spaceIndex = cmdLine.indexOf(' ')
        val cmd = if (spaceIndex != -1) cmdLine.substring(0, spaceIndex).uppercase() else cmdLine.uppercase()
        val arg = if (spaceIndex != -1) cmdLine.substring(spaceIndex + 1).trim() else ""

        when (cmd) {
            "USER" -> {
                currentUser = arg
                sendResponse(writer, "331 User name okay, need password")
            }
            "PASS" -> {
                // If expectedPass is empty or matches, authenticate
                if (expectedPass.isEmpty() || arg == expectedPass || expectedUser == "anonymous") {
                    isAuthenticated = true
                    sendResponse(writer, "230 User logged in, proceed")
                } else {
                    sendResponse(writer, "530 User not logged in, incorrect password")
                }
            }
            "SYST" -> sendResponse(writer, "215 UNIX Type: L8")
            "FEAT" -> {
                writer.write("211-Features:\r\n")
                writer.write(" UTF8\r\n")
                writer.write(" EPSV\r\n")
                writer.write(" PASV\r\n")
                writer.write(" SIZE\r\n")
                writer.write(" REST STREAM\r\n")
                writer.write("211 End\r\n")
                writer.flush()
            }
            "TYPE" -> sendResponse(writer, "200 Type set to $arg")
            "PWD" -> sendResponse(writer, "257 \"/\" is current directory")
            "CWD", "CDUP" -> sendResponse(writer, "250 Directory successfully changed")
            "PASV" -> handlePasv(writer)
            "EPSV" -> handleEpsv(writer)
            "PORT" -> handlePort(arg, writer)
            "STOR" -> handleStor(arg, writer, clientIp)
            "NOOP" -> sendResponse(writer, "200 NOOP ok")
            "QUIT" -> {
                sendResponse(writer, "221 Goodbye")
                isRunning = false
            }
            else -> sendResponse(writer, "502 Command not implemented: $cmd")
        }
    }

    private fun handlePasv(writer: BufferedWriter) {
        try {
            passiveServerSocket?.close()
            // Bind ephemeral port
            passiveServerSocket = ServerSocket(0)
            val port = passiveServerSocket!!.localPort

            val ipParts = localIp.split(".")
            val p1 = port / 256
            val p2 = port % 256
            val pasvResponse = "227 Entering Passive Mode (${ipParts[0]},${ipParts[1]},${ipParts[2]},${ipParts[3]},$p1,$p2)"
            sendResponse(writer, pasvResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open passive data socket: ${e.message}")
            sendResponse(writer, "425 Can't open data connection")
        }
    }

    private fun handleEpsv(writer: BufferedWriter) {
        try {
            passiveServerSocket?.close()
            passiveServerSocket = ServerSocket(0)
            val port = passiveServerSocket!!.localPort
            sendResponse(writer, "229 Entering Extended Passive Mode (|||$port|)")
        } catch (e: Exception) {
            sendResponse(writer, "425 Can't open data connection")
        }
    }

    private fun handlePort(arg: String, writer: BufferedWriter) {
        try {
            val tokens = StringTokenizer(arg, ",")
            val h1 = tokens.nextToken()
            val h2 = tokens.nextToken()
            val h3 = tokens.nextToken()
            val h4 = tokens.nextToken()
            val p1 = tokens.nextToken().toInt()
            val p2 = tokens.nextToken().toInt()
            activeDataHost = "$h1.$h2.$h3.$h4"
            activeDataPort = (p1 * 256) + p2
            sendResponse(writer, "200 PORT command successful")
        } catch (e: Exception) {
            sendResponse(writer, "501 Syntax error in PORT parameters")
        }
    }

    private fun handleStor(filenameArg: String, writer: BufferedWriter, clientIp: String) {
        // Clean filename (strip folders like /DCIM/100MSDCF/DSC001.JPG)
        val cleanName = File(filenameArg).name
        listener?.onTransferStarted(cleanName, clientIp)

        sendResponse(writer, "150 Opening BINARY mode data connection for $cleanName")

        var dataSocket: Socket? = null
        try {
            dataSocket = if (passiveServerSocket != null) {
                passiveServerSocket!!.soTimeout = 15000
                passiveServerSocket!!.accept()
            } else if (activeDataPort != -1 && activeDataHost != null) {
                Socket(activeDataHost, activeDataPort)
            } else {
                sendResponse(writer, "425 Can't open data connection")
                listener?.onTransferFailed(cleanName, "No data connection configured")
                return
            }

            // Stream into temporary file first for atomic commit
            storageDir.mkdirs()
            val tempFile = File(storageDir, "$cleanName.tmp.${System.currentTimeMillis()}")
            val targetFile = File(storageDir, cleanName)

            val startTime = System.currentTimeMillis()
            var totalBytesRead = 0L

            dataSocket.getInputStream().use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(16384)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                    }
                    outputStream.flush()
                }
            }

            val durationMs = maxOf(1L, System.currentTimeMillis() - startTime)

            if (tempFile.exists() && tempFile.length() > 0) {
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                tempFile.renameTo(targetFile)
                sendResponse(writer, "226 Transfer complete ($totalBytesRead bytes in ${durationMs}ms)")
                Log.i(TAG, "File received successfully: $cleanName (${totalBytesRead} bytes)")
                listener?.onTransferCompleted(targetFile, cleanName, clientIp, durationMs)
            } else {
                tempFile.delete()
                sendResponse(writer, "550 Failed to save file, 0 bytes received")
                listener?.onTransferFailed(cleanName, "Zero bytes written")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error storing file: ${e.message}")
            sendResponse(writer, "426 Connection closed; transfer aborted")
            listener?.onTransferFailed(cleanName, e.message ?: "Transfer aborted")
        } finally {
            try { dataSocket?.close() } catch (e: Exception) {}
            try { passiveServerSocket?.close() } catch (e: Exception) {}
            passiveServerSocket = null
            activeDataPort = -1
        }
    }

    private fun sendResponse(writer: BufferedWriter, response: String) {
        Log.d(TAG, "FTP SEND >> $response")
        writer.write("$response\r\n")
        writer.flush()
    }

    private fun closeSession(clientIp: String) {
        isRunning = false
        try { passiveServerSocket?.close() } catch (e: Exception) {}
        try { controlSocket.close() } catch (e: Exception) {}
        listener?.onClientDisconnected(clientIp)
    }
}

