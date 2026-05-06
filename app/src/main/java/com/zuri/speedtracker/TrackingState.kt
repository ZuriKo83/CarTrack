package com.zuri.cartrack

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf

object TrackingState {
    val currentSpeed = mutableFloatStateOf(0f)
    val totalDistance = mutableFloatStateOf(0f)
    val maxSpeed = mutableFloatStateOf(0f)
    val averageSpeed = mutableFloatStateOf(0f)
    val elapsedSeconds = mutableLongStateOf(0L)
    val points = mutableStateListOf<TripPoint>()
}