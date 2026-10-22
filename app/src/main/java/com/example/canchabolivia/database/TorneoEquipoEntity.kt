package com.example.canchabolivia.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

// Tabla intermedia torneo <-> equipos (muchos a muchos): qué equipos están inscritos.
// Si se borra un equipo, su inscripción desaparece sola por el CASCADE.
@Entity(
    tableName = "torneo_equipos",
    primaryKeys = ["torneoId", "equipoId"],
    foreignKeys = [
        ForeignKey(
            entity = TorneoEntity::class,
            parentColumns = ["id"],
            childColumns = ["torneoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EquipoEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("equipoId")]
)
data class TorneoEquipoEntity(
    val torneoId: Int,
    val equipoId: Int,
    // Orden de inscripción: el fixture sin sorteo empareja en este orden.
    val posicion: Int
)
