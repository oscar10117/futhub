package com.example.canchabolivia.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EquipoDao {

    // Flow: Room vuelve a emitir la lista cada vez que la tabla cambia.
    @Query("SELECT * FROM equipos ORDER BY id")
    abstract fun observarEquipos(): Flow<List<EquipoEntity>>

    @Query("SELECT * FROM jugadores ORDER BY id")
    abstract fun observarJugadores(): Flow<List<JugadorEntity>>

    @Insert
    protected abstract suspend fun insertarEquipo(equipo: EquipoEntity): Long

    @Insert
    protected abstract suspend fun insertarJugadores(jugadores: List<JugadorEntity>)

    // Equipo y plantel van juntos: si algo falla no queda un equipo sin jugadores.
    @Transaction
    open suspend fun crearConJugadores(equipo: EquipoEntity, jugadores: List<JugadorEntity>) {
        val equipoId = insertarEquipo(equipo).toInt()
        insertarJugadores(jugadores.map { it.copy(equipoId = equipoId) })
    }

    // Los jugadores del equipo se borran solos por el ON DELETE CASCADE.
    @Query("DELETE FROM equipos WHERE id = :id")
    abstract suspend fun eliminarEquipo(id: Int)

    @Update
    abstract suspend fun actualizarEquipo(equipo: EquipoEntity)

    // --- Plantel (el organizador agrega, edita y quita jugadores) ---
    @Insert
    abstract suspend fun insertarJugador(jugador: JugadorEntity)

    @Update
    abstract suspend fun actualizarJugador(jugador: JugadorEntity)

    // Sus fichas médicas y tarjetas se borran solas por el ON DELETE CASCADE.
    @Query("DELETE FROM jugadores WHERE id = :id")
    abstract suspend fun eliminarJugador(id: Int)
}
