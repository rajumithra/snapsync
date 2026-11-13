package com.snapsync.app.service.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.os.Build
import android.util.Log

class UsbCameraManager(
    private val context: Context,
    private val onDeviceConnected: (UsbDevice) -> Unit,
    private val onDeviceDisconnected: (UsbDevice) -> Unit
) {
    private val TAG = "UsbCameraManager"
    private val ACTION_USB_PERMISSION = "com.snapsync.app.USB_PERMISSION"
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    device?.let { handleDeviceAttached(it) }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    device?.let { onDeviceDisconnected(it) }
                }
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.let { onDeviceConnected(it) }
                        } else {
                            Log.w(TAG, "USB Permission denied for device: ${device?.deviceName}")
                        }
                    }
                }
            }
        }
    }

    fun register() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
        scanForConnectedDevices()
    }

    fun unregister() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }

    fun scanForConnectedDevices() {
        val deviceList = usbManager.deviceList
        for ((_, device) in deviceList) {
            if (isCameraDevice(device)) {
                handleDeviceAttached(device)
            }
        }
    }

    private fun handleDeviceAttached(device: UsbDevice) {
        if (!usbManager.hasPermission(device)) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PERMISSION), flags
            )
            usbManager.requestPermission(device, permissionIntent)
        } else {
            onDeviceConnected(device)
        }
    }

    private fun isCameraDevice(device: UsbDevice): Boolean {
        // USB Class 6 = Image / PTP
        if (device.deviceClass == UsbConstants.USB_CLASS_STILL_IMAGE) return true
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_STILL_IMAGE) {
                return true
            }
        }
        return false
    }
}

