package com.example.canchabolivia.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Query(
        "SELECT * FROM usuarios WHERE correo = :correo COLLATE NOCASE AND " +
        "claveHash = :claveHash LIMIT 1")
    suspend fun buscar(correo: String, claveHash: String): UsuarioEntity?


    @Query("SELECT *FROM usuarios WHERE id = :id")
    suspend fun buscarPorId(id: Int): UsuarioEntity?
}