package com.example.canchabolivia.repository

import com.example.canchabolivia.Formato
import com.example.canchabolivia.Partido
import com.example.canchabolivia.Tarjeta
import com.example.canchabolivia.Torneo
import com.example.canchabolivia.database.PartidoDao
import com.example.canchabolivia.database.TorneoDao
import com.example.canchabolivia.database.TorneoEntity
import com.example.canchabolivia.mapper.aEntity
import com.example.canchabolivia.mapper.aModelo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

// Torneo, fixture, resultados y tarjetas guardados en el teléfono.
class TorneoRepository(
    private val torneoDao: TorneoDao,
    private val partidoDao: PartidoDao
) {

    // La fila del torneo + sus inscritos (tabla intermedia) forman el modelo Torneo.
    val torneo: Flow<Torneo> = combine(
        torneoDao.observarTorneo().filterNotNull(),
        torneoDao.observarEquipoIds()
    ) { torneo, equipoIds -> torneo.aModelo(equipoIds) }

    val partidos: Flow<List<Partido>> =
        partidoDao.observarPartidos().map { lista -> lista.map { it.aModelo() } }

    val tarjetas: Flow<List<Tarjeta>> =
        partidoDao.observarTarjetas().map { lista -> lista.map { it.aModelo() } }

    suspend fun guardarTorneo(nombre: String, formato: Formato, equipoIds: List<Int>) =
        torneoDao.guardar(TorneoEntity(nombre = nombre, formato = formato), equipoIds)

    suspend fun reemplazarFixture(partidos: List<Partido>) =
        partidoDao.reemplazarFixture(partidos.map { it.aEntity() })

    suspend fun agregarPartidos(partidos: List<Partido>) =
        partidoDao.insertarPartidos(partidos.map { it.aEntity() })

    suspend fun actualizarPartidos(partidos: List<Partido>) =
        partidoDao.actualizarPartidos(partidos.map { it.aEntity() })

    suspend fun agregarTarjeta(tarjeta: Tarjeta) = partidoDao.insertarTarjeta(tarjeta.aEntity())
}
