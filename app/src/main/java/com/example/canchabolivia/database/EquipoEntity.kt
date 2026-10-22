package com.example.canchabolivia.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Tabla de equipos. El id es autoincremental: un id borrado nunca se reutiliza,
// así un partido viejo no termina apuntando a un equipo nuevo.
@Entity(tableName = "equipos")
data class EquipoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val sigla: String
)
