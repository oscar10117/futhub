package com.example.canchabolivia.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PartidoDao {

    @Query("SELECT * FROM partidos ORDER BY id")
    abstract fun observarPartidos(): Flow<List<PartidoEntity>>

    @Query("SELECT * FROM tarjetas ORDER BY id")
    abstract fun observarTarjetas(): Flow<List<TarjetaEntity>>

    @Query("DELETE FROM partidos")
    protected abstract suspend fun borrarPartidos()

    // IGNORE: si se toca dos veces "avanzar ronda", la segunda no duplica partidos.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertarPartidos(partidos: List<PartidoEntity>)

    // Un fixture nuevo reemplaza al anterior (sus tarjetas se van por CASCADE).
    @Transaction
    open suspend fun reemplazarFixture(partidos: List<PartidoEntity>) {
        borrarPartidos()
        insertarPartidos(partidos)
    }

    // Resultado, cancelación y reprogramación: se actualiza la fila completa.
    @Update
    abstract suspend fun actualizarPartidos(partidos: List<PartidoEntity>)

    @Insert
    abstract suspend fun insertarTarjeta(tarjeta: TarjetaEntity)
}
