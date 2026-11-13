package com.snapsync.app.service.ftp

import java.io.File

interface FtpEventListener {
    fun onServerStarted(ip: String, port: Int)
    fun onServerStopped()
    fun onClientConnected(clientIp: String)
    fun onClientDisconnected(clientIp: String)
    fun onTransferStarted(filename: String, clientIp: String)
    fun onTransferCompleted(file: File, filename: String, clientIp: String, durationMs: Long)
    fun onTransferFailed(filename: String, reason: String)
    fun onServerError(error: String)
}

