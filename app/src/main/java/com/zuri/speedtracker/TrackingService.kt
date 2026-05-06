package com.zuri.cartrack

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class TrackingService : Service() {

    private lateinit var locationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null
    private var totalDistance = 0f
    private var maxSpeed = 0f
    private var startTime = 0L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val speedKmh = location.speed * 3.6f

            lastLocation?.let {
                totalDistance += it.distanceTo(location)
            }

            if (speedKmh > maxSpeed) {
                maxSpeed = speedKmh
            }

            lastLocation = location

            TrackingState.currentSpeed.floatValue = speedKmh
            TrackingState.totalDistance.floatValue = totalDistance
            TrackingState.maxSpeed.floatValue = maxSpeed

            val elapsed = (System.currentTimeMillis() - startTime) / 1000L
            val averageSpeed = if (elapsed > 0) {
                (totalDistance / elapsed) * 3.6f
            } else {
                0f
            }

            TrackingState.averageSpeed.floatValue = averageSpeed
            TrackingState.elapsedSeconds.longValue = elapsed

            println(
                "속도=${"%.1f".format(speedKmh)}km/h, 거리=${"%.1f".format(totalDistance)}m, 최고속도=${"%.1f".format(maxSpeed)}km/h"
            )

            TrackingState.points.add(
                TripPoint(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    speedKmh = speedKmh,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        startTime = System.currentTimeMillis()
        createNotificationChannel()
        startForeground(1, createNotification())
        startLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L
        )
            .setMinUpdateIntervalMillis(1000L)
            .build()

        try {
            locationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "tracking_channel")
            .setContentTitle("CarTrack")
            .setContentText("주행 기록 중")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "tracking_channel",
                "주행 기록",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

