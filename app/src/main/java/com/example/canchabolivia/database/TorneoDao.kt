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
}
