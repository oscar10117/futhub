package com.example.canchabolivia.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TorneoDao {

    @Query("SELECT * FROM torneo WHERE id = 1")
    abstract fun observarTorneo(): Flow<TorneoEntity?>

    @Query("SELECT equipoId FROM torneo_equipos WHERE torneoId = 1 ORDER BY posicion")
    abstract fun observarEquipoIds(): Flow<List<Int>>

    // Upsert y no REPLACE: REPLACE borra la fila y el CASCADE se llevaría las inscripciones.
    @Upsert
    protected abstract suspend fun guardarTorneo(torneo: TorneoEntity)

    @Query("DELETE FROM torneo_equipos WHERE torneoId = :torneoId")
    protected abstract suspend fun borrarInscritos(torneoId: Int)

    @Insert
    protected abstract suspend fun insertarInscritos(inscritos: List<TorneoEquipoEntity>)

    // Nombre, formato y lista de inscritos se guardan juntos o no se guarda nada.
    @Transaction
    open suspend fun guardar(torneo: TorneoEntity, equipoIds: List<Int>) {
        guardarTorneo(torneo)
        borrarInscritos(torneo.id)
        insertarInscritos(equipoIds.mapIndexed { i, id -> TorneoEquipoEntity(torneo.id, id, i) })
    }

    @Query("UPDATE torneo SET finalizado = 1, campeon = :campeon WHERE id = 1")
    abstract suspend fun terminar(campeon: String?)

    @Query("DELETE FROM partidos")
    protected abstract suspend fun borrarPartidos()

    @Query("UPDATE torneo SET finalizado = 0, campeon = NULL WHERE id = 1")
    protected abstract suspend fun reabrir()

    // Nuevo torneo: se borra el fixture (las tarjetas se van por CASCADE) y se
    // reabre, conservando nombre, formato e inscritos para reconfigurarlos.
    @Transaction
    open suspend fun empezarNuevo() {
        borrarPartidos()
        reabrir()
    }
}
