package com.example.canchabolivia.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.example.canchabolivia.Rol

@Entity(tableName = "usuarios", indices = [Index(value = ["correo"], unique = true)])


data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre:String,
    val correo: String,
    val claveHash: String,
    val rol: Rol
)





