package com.example.canchabolivia.dto

import com.google.gson.annotations.SerializedName

// Copia fiel del JSON de Open-Meteo (/v1/forecast?current=...).
data class ClimaResponseDto(
    @SerializedName("latitude") val latitud: Double?,
    @SerializedName("longitude") val longitud: Double?,
    @SerializedName("current") val actual: ClimaActualDto?
)

data class ClimaActualDto(
    @SerializedName("time") val hora: String?,
    @SerializedName("temperature_2m") val temperatura: Double?,
    @SerializedName("relative_humidity_2m") val humedad: Int?,
    @SerializedName("wind_speed_10m") val viento: Double?
)
