package com.example.canchabolivia.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.canchabolivia.Rol

// Tabla de usuarios para iniciar sesión. La contraseña nunca se guarda en
// texto plano: aquí va su hash SHA-256 (ver Seguridad.hash). El correo es único.
@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["correo"], unique = true)]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val correo: String,
    val claveHash: String,
    val rol: Rol
)
