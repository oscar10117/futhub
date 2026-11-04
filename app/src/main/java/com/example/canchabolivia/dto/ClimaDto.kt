package com.example.canchabolivia.dto

import com.google.gson.annotations.SerializedName

// Copia fiel del JSON de Open-Meteo (/v1/forecast?current=...).
// [GUIA DOCENTE - DTO CLIMA] Estos campos representan el JSON que devuelve la API.
// Si agregas una variable del clima, incorporala aqui y actualiza el mapper.
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
