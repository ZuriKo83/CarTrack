package com.zuri.cartrack

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class CarBluetoothReceiver : BroadcastReceiver() {

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onReceive(context: Context, intent: Intent) {

        Log.d("CarBluetooth", "Bluetooth event received: ${intent.action}")

        if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) return

        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        } ?: return

        val deviceName = device.name ?: return

        val savedName = context
            .getSharedPreferences("cartrack_prefs", Context.MODE_PRIVATE)
            .getString("car_bluetooth_name", "")
            ?.trim()
            ?: ""

        if (savedName.isEmpty()) return

        if (!deviceName.contains(savedName, ignoreCase = true)) return

        showCarConnectedNotification(context, deviceName)
    }

    private fun isCarBluetooth(name: String): Boolean {
        return true
    }

    private fun showCarConnectedNotification(context: Context, deviceName: String) {
        val channelId = "car_bluetooth_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "차량 연결 알림",
                NotificationManager.IMPORTANCE_HIGH
            )

            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("auto_start", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("차량 블루투스 연결됨")
            .setContentText("$deviceName 연결됨. CarTrack를 시작할까요?")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(1001, notification)
        }
    }
}