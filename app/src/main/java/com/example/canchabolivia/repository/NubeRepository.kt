package com.example.canchabolivia.repository

import android.util.Log
import com.example.canchabolivia.Equipo
import com.example.canchabolivia.FichaMedica
import com.example.canchabolivia.Jugador
import com.example.canchabolivia.Partido
import com.example.canchabolivia.Tarjeta
import com.example.canchabolivia.Torneo
import com.example.canchabolivia.api.FutHubNubeApi
import com.example.canchabolivia.dto.DatosDto
import com.example.canchabolivia.mapper.DatosNube
import com.example.canchabolivia.mapper.aCuerpo
import com.example.canchabolivia.mapper.aModelo
import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.coroutines.CancellationException

// Única puerta hacia SQLite Cloud. Cada método = una Edge Function
// (ver la carpeta sqlitecloud/funciones). Si cambias el nombre de una
// función en el panel, solo hay que cambiarlo aquí.
class NubeRepository(private val api: FutHubNubeApi) {

    private val gson = Gson()

    suspend fun cargarDatos(): Result<DatosNube> = intentar {
        gson.fromJson(llamar("cargar_datos"), DatosDto::class.java).aModelo()
    }

    suspend fun guardarEquipo(equipo: Equipo, jugadores: List<Jugador>): Result<Unit> = intentar<Unit> {
        llamar("guardar_equipo", mapOf("equipo" to equipo.aCuerpo(), "jugadores" to jugadores.map { it.aCuerpo() }))
    }

    suspend fun eliminarEquipo(equipoId: Int): Result<Unit> = intentar<Unit> {
        llamar("eliminar_equipo", mapOf("equipo_id" to equipoId))
    }

    suspend fun guardarFicha(ficha: FichaMedica): Result<Unit> = intentar<Unit> {
        llamar("guardar_ficha", ficha.aCuerpo())
    }

    suspend fun guardarTorneo(torneo: Torneo): Result<Unit> = intentar<Unit> {
        llamar("guardar_torneo", torneo.aCuerpo())
    }

    suspend fun guardarPartidos(partidos: List<Partido>): Result<Unit> = intentar<Unit> {
        llamar("guardar_partidos", mapOf("partidos" to partidos.map { it.aCuerpo() }))
    }

    suspend fun guardarTarjeta(tarjeta: Tarjeta): Result<Unit> = intentar<Unit> {
        llamar("guardar_tarjeta", tarjeta.aCuerpo())
    }

    // Las funciones responden { "data": ... }; aquí se saca lo que va dentro.
    private suspend fun llamar(funcion: String, cuerpo: Map<String, Any?> = emptyMap()): JsonElement {
        var json = api.llamar(funcion, cuerpo)
        while (json.isJsonObject && json.asJsonObject.has("data")) {
            json = json.asJsonObject.get("data")
        }
        return json
    }

    private suspend fun <T> intentar(bloque: suspend () -> T): Result<T> = try {
        Result.success(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Error al hablar con SQLite Cloud", e)
        Result.failure(e)
    }

    companion object {
        const val TAG = "FutHubNube"
    }
}
