package com.example.salat




data class PrayerTimesResponse(
    val status: String,
    val data: PrayerTimesData
)

data class PrayerTimesData(
    val timings: Timings
)

data class Timings(
    val Fajr: String?,
    val Dhuhr: String?,
    val Asr: String?,
    val Maghrib: String?,
    val Isha: String?
)
