package com.example.canchabolivia.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClimaDao {

    @Query("SELECT * FROM clima WHERE id = 1")
    fun observar(): Flow<ClimaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(clima: ClimaEntity)
}
