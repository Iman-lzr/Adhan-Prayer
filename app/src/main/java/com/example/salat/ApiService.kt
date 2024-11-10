package com.example.salat

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query



interface PrayerTimesAPI {

    @GET("v1/timings")
    fun getPrayerTimes(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 2 // Méthode de calcul (2 pour ISNA, par exemple)
    ): Call<PrayerTimesResponse>
}
