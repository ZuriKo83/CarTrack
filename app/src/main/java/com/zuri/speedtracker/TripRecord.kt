package com.zuri.cartrack

data class TripRecord(
    val date: String,
    val maxSpeed: Float,
    val averageSpeed: Float,
    val distanceMeters: Float,
    val durationSeconds: Long,
    val points: List<TripPoint> = emptyList()
)