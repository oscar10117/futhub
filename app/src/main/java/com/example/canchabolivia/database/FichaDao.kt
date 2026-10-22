package com.example.canchabolivia.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FichaDao {

    @Query("SELECT * FROM fichas ORDER BY id")
    fun observarFichas(): Flow<List<FichaEntity>>

    @Insert
    suspend fun insertar(ficha: FichaEntity)

    // El médico acepta/rechaza o evalúa: solo cambia el estado.
    @Update
    suspend fun actualizar(ficha: FichaEntity)
}
