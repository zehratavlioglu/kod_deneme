package com.lideatech.ridelink.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

class AudioDeviceManager(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    data class Route(
        val name: String,
        val isBluetoothCommunication: Boolean,
        val device: AudioDeviceInfo?,
    )

    fun bluetoothCommunicationRoute(): Route? {
        val devices = if (Build.VERSION.SDK_INT >= 31) {
            audioManager.availableCommunicationDevices
        } else {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        }
        val device = devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= 31 && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        } ?: return null
        return Route(
            name = device.productName?.toString()?.takeIf(String::isNotBlank) ?: "Bluetooth interkom",
            isBluetoothCommunication = true,
            device = device,
        )
    }

    fun currentRouteName(): String = if (Build.VERSION.SDK_INT >= 31) {
        audioManager.communicationDevice?.productName?.toString() ?: "Sistem varsayılanı"
    } else if (audioManager.isBluetoothScoOn) {
        "Bluetooth SCO"
    } else {
        "Sistem varsayılanı"
    }

    fun selectCommunicationRoute(debugPhoneAudio: Boolean): Boolean {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        val route = bluetoothCommunicationRoute()
        if (route == null) return debugPhoneAudio
        return if (Build.VERSION.SDK_INT >= 31) {
            route.device != null && audioManager.setCommunicationDevice(route.device)
        } else {
            @Suppress("DEPRECATION")
            audioManager.startBluetoothSco()
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = true
            true
        }
    }

    fun clearRoute() {
        if (Build.VERSION.SDK_INT >= 31) {
            audioManager.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            audioManager.stopBluetoothSco()
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = false
        }
        audioManager.mode = AudioManager.MODE_NORMAL
    }
}
