package com.example.canchabolivia.mapper

import com.example.canchabolivia.FichaMedica
import com.example.canchabolivia.database.FichaEntity

// Entity (Room) a modelo de la app, y al revés.
fun FichaEntity.aModelo() =
    FichaMedica(id = id, jugadorId = jugadorId, horarioId = horarioId, estado = estado)

fun FichaMedica.aEntity() =
    FichaEntity(id = id, jugadorId = jugadorId, horarioId = horarioId, estado = estado)
