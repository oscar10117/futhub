package com.example.canchabolivia.database

import androidx.room.Dao
import androidx.room.Query

@Dao
interface UsuarioDao {

    @Query("SELECT * FROM usuarios WHERE correo = :correo COLLATE NOCASE AND " +
            "claveHash = :claveHash LIMIT 1")
    suspend fun buscar(correo: String, claveHash: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun porId(id: Int): UsuarioEntity?
}
