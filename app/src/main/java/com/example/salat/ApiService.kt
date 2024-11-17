package com.example.salat

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query




interface PrayerTimesAPI {

    @GET("timings")
    fun getPrayerTimes(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): Call<PrayerTimesResponse>

}