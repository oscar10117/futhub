package com.example.canchabolivia.repository

import com.example.canchabolivia.api.OpenMeteoApi
import com.example.canchabolivia.database.ClimaDao
import com.example.canchabolivia.mapper.aEntity
import com.example.canchabolivia.mapper.aModelo
import com.example.canchabolivia.model.Clima
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ClimaRepository(
    private val api: OpenMeteoApi,
    private val dao: ClimaDao
) {

    // Lo guardado en el teléfono (null si nunca se ha descargado).
    val guardado: Flow<Clima?> = dao.observar().map { it?.aModelo(desdeCache = true) }

    // Pide el clima actual a la API y lo guarda en Room. Si falla devuelve
    // Result.failure y la pantalla sigue mostrando la última lectura guardada.
    suspend fun actualizar(): Result<Clima> = try {
        val respuesta = api.clima(OpenMeteoApi.LATITUD_LA_PAZ, OpenMeteoApi.LONGITUD_LA_PAZ)
        val entity = respuesta.aEntity() ?: error("Respuesta incompleta del servicio del clima")
        dao.guardar(entity)
        Result.success(entity.aModelo(desdeCache = false))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
