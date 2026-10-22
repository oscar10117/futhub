package com.example.canchabolivia.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Tabla de partidos con su resultado. El id no es autoincremental porque lo
// arma MotorTorneo al generar el fixture y al avanzar de ronda.
// Al borrar un equipo se borran sus partidos (y con ellos, sus tarjetas).
@Entity(
    tableName = "partidos",
    foreignKeys = [
        ForeignKey(
            entity = EquipoEntity::class,
            parentColumns = ["id"],
            childColumns = ["localId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EquipoEntity::class,
            parentColumns = ["id"],
            childColumns = ["visitanteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("localId"), Index("visitanteId")]
)
data class PartidoEntity(
    @PrimaryKey val id: Int,
    val ronda: Int,
    val localId: Int,
    // null = pase libre
    val visitanteId: Int?,
    val golesLocal: Int?,
    val golesVisitante: Int?,
    val ganadorPenalesId: Int?,
    val cancelado: Boolean
)
