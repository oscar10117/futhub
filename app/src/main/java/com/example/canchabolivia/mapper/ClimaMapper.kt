package com.example.canchabolivia.mapper

import com.example.canchabolivia.database.ClimaEntity
import com.example.canchabolivia.dto.ClimaResponseDto
import com.example.canchabolivia.model.Clima

// DTO (API) a Entity (Room). Devuelve null si la API no trajo los datos completos.
fun ClimaResponseDto.aEntity(): ClimaEntity? {
    val c = actual ?: return null
    return ClimaEntity(
        temperatura = c.temperatura ?: return null,
        humedad = c.humedad ?: return null,
        viento = c.viento ?: return null,
        hora = c.hora ?: return null
    )
}

// Entity (Room) a modelo limpio para la UI.
fun ClimaEntity.aModelo(desdeCache: Boolean) = Clima(
    temperatura = temperatura,
    humedad = humedad,
    viento = viento,
    hora = hora,
    desdeCache = desdeCache
)
