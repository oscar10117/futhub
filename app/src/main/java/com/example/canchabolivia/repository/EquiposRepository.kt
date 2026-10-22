package com.example.canchabolivia.repository

import com.example.canchabolivia.Equipo
import com.example.canchabolivia.Jugador
import com.example.canchabolivia.database.EquipoDao
import com.example.canchabolivia.database.EquipoEntity
import com.example.canchabolivia.database.JugadorEntity
import com.example.canchabolivia.mapper.aModelo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Equipos y jugadores guardados en el teléfono: sobreviven al cerrar la app.
class EquiposRepository(private val dao: EquipoDao) {

    val equipos: Flow<List<Equipo>> =
        dao.observarEquipos().map { lista -> lista.map { it.aModelo() } }

    val jugadores: Flow<List<Jugador>> =
        dao.observarJugadores().map { lista -> lista.map { it.aModelo() } }

    // Todo equipo nuevo arranca con dos jugadores de ejemplo (los ids los pone Room).
    suspend fun agregar(nombre: String, sigla: String) = dao.crearConJugadores(
        EquipoEntity(nombre = nombre, sigla = sigla),
        listOf(
            JugadorEntity(nombre = "Jugador 1", equipoId = 0, dorsal = 10),
            JugadorEntity(nombre = "Jugador 2", equipoId = 0, dorsal = 7)
        )
    )

    suspend fun eliminar(equipoId: Int) = dao.eliminarEquipo(equipoId)
}
