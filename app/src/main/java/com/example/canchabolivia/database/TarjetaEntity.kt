package com.example.canchabolivia.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.canchabolivia.TipoTarjeta

// Tarjetas que pone el árbitro. Dependen del partido y del jugador:
// si cualquiera de los dos se borra, la tarjeta también.
@Entity(
    tableName = "tarjetas",
    foreignKeys = [
        ForeignKey(
            entity = PartidoEntity::class,
            parentColumns = ["id"],
            childColumns = ["partidoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = JugadorEntity::class,
            parentColumns = ["id"],
            childColumns = ["jugadorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("partidoId"), Index("jugadorId")]
)
data class TarjetaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val partidoId: Int,
    val jugadorId: Int,
    val tipo: TipoTarjeta,
    val minuto: Int
)
