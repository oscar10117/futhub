package com.example.canchabolivia.api

import com.example.canchabolivia.dto.ClimaResponseDto
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

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

        private val cliente: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                .build()
        }

        val instancia: OpenMeteoApi by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(cliente)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenMeteoApi::class.java)
        }
    }
}
