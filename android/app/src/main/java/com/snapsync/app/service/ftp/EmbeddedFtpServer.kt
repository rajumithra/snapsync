package com.snapsync.app.service.ftp

import android.util.Log
import java.io.File
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class EmbeddedFtpServer(
    private val port: Int = 2121,
    private val storageDir: File,
    private val username: String = "snapsync",
    private val password: String = "snapsync",
    private val localIp: String = "192.168.43.1"
) {
    private val TAG = "EmbeddedFtpServer"
    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val threadPool = Executors.newCachedThreadPool()
    private var listener: FtpEventListener? = null

    fun setListener(listener: FtpEventListener) {
        this.listener = listener
    }

    fun isServerRunning(): Boolean = isRunning.get()

    fun start() {
        if (isRunning.get()) {
            Log.w(TAG, "FTP Server is already running")
            return
        }

        threadPool.execute {
            try {
                serverSocket = ServerSocket(port)
                isRunning.set(true)
                Log.i(TAG, "SnapSync FTP Server listening on port $port (IP: $localIp)")
                listener?.onServerStarted(localIp, port)

                while (isRunning.get() && serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        threadPool.execute(
                            FtpSessionHandler(
                                controlSocket = clientSocket,
                                storageDir = storageDir,
                                expectedUser = username,
                                expectedPass = password,
                                localIp = localIp,
                                listener = listener
                            )
                        )
                    } catch (e: Exception) {
                        if (isRunning.get()) {
                            Log.e(TAG, "Accept error: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start FTP server: ${e.message}")
                listener?.onServerError(e.message ?: "Server startup error")
                stop()
            }
        }
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return
        Log.i(TAG, "Stopping SnapSync FTP Server...")

        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serverSocket = null
        listener?.onServerStopped()
    }
}

