package com.example.canchabolivia.api

import com.example.canchabolivia.dto.ClimaResponseDto
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {

    // Retrofit codifica los parámetros solo: "America/La_Paz" sale como America%2FLa_Paz.
    @GET("v1/forecast")
    suspend fun clima(
        @Query("latitude") latitud: Double,
        @Query("longitude") longitud: Double,
        @Query("current") variables: String = "temperature_2m,relative_humidity_2m,wind_speed_10m",
        @Query("timezone") zonaHoraria: String = "America/La_Paz"
    ): ClimaResponseDto

    companion object {
        private const val BASE_URL = "https://api.open-meteo.com/"

        // La Paz, Bolivia
        const val LATITUD_LA_PAZ = -16.5
        const val LONGITUD_LA_PAZ = -68.15

        val instancia: OpenMeteoApi by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenMeteoApi::class.java)
        }
    }
}
