package com.example.canchabolivia.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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
}
