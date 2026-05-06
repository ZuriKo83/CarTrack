package com.zuri.cartrack

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.text.toFloat

object TripStorage {
    private const val FILE_NAME = "trip_records.json"

    fun save(context: Context) {
        val pointsArray = JSONArray()

        TrackingState.points.forEach { point ->
            pointsArray.put(
                JSONObject().apply {
                    put("latitude", point.latitude)
                    put("longitude", point.longitude)
                    put("speedKmh", point.speedKmh)
                    put("timestamp", point.timestamp)
                }
            )
        }

        val record = JSONObject().apply {
            put("date", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(Date()))
            put("maxSpeed", TrackingState.maxSpeed.floatValue)
            put("averageSpeed", TrackingState.averageSpeed.floatValue)
            put("distanceMeters", TrackingState.totalDistance.floatValue)
            put("durationSeconds", TrackingState.elapsedSeconds.longValue)
            put("points", pointsArray)
        }

        val array = loadJsonArray(context)
        array.put(record)

        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
            it.write(array.toString().toByteArray())
        }
    }

    fun load(context: Context): List<TripRecord> {
        val array = loadJsonArray(context)
        val list = mutableListOf<TripRecord>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val pointsJson = obj.optJSONArray("points") ?: JSONArray()
            val points = mutableListOf<TripPoint>()

            for (j in 0 until pointsJson.length()) {
                val pointObj = pointsJson.getJSONObject(j)

                points.add(
                    TripPoint(
                        latitude = pointObj.getDouble("latitude"),
                        longitude = pointObj.getDouble("longitude"),
                        speedKmh = pointObj.getDouble("speedKmh").toFloat(),
                        timestamp = pointObj.getLong("timestamp")
                    )
                )
            }

            list.add(
                TripRecord(
                    date = obj.getString("date"),
                    maxSpeed = obj.getDouble("maxSpeed").toFloat(),
                    averageSpeed = obj.getDouble("averageSpeed").toFloat(),
                    distanceMeters = obj.getDouble("distanceMeters").toFloat(),
                    durationSeconds = obj.getLong("durationSeconds"),
                    points = points
                )
            )
        }

        return list.reversed()
    }

    private fun loadJsonArray(context: Context): JSONArray {
        return try {
            val text = context.openFileInput(FILE_NAME).bufferedReader().use { it.readText() }
            JSONArray(text)
        } catch (e: Exception) {
            JSONArray()
        }
    }

    fun deleteRecord(context: Context, target: TripRecord) {
        val array = loadJsonArray(context)
        val newArray = JSONArray()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)

            val same =
                obj.getString("date") == target.date &&
                        obj.getDouble("maxSpeed").toFloat() == target.maxSpeed &&
                        obj.getDouble("averageSpeed").toFloat() == target.averageSpeed &&
                        obj.getDouble("distanceMeters").toFloat() == target.distanceMeters &&
                        obj.getLong("durationSeconds") == target.durationSeconds

            if (!same) {
                newArray.put(obj)
            }
        }

        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
            it.write(newArray.toString().toByteArray())
        }
    }

    fun clearAll(context: Context) {
        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
            it.write("[]".toByteArray())
        }
    }
}

