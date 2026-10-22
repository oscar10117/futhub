package com.example.canchabolivia.mapper

import com.example.canchabolivia.Partido
import com.example.canchabolivia.Tarjeta
import com.example.canchabolivia.Torneo
import com.example.canchabolivia.database.PartidoEntity
import com.example.canchabolivia.database.TarjetaEntity
import com.example.canchabolivia.database.TorneoEntity

// Entity (Room) a modelo de la app, y al revés para lo que la app modifica.
fun TorneoEntity.aModelo(equipoIds: List<Int>) =
    Torneo(nombre = nombre, formato = formato, equipoIds = equipoIds.toMutableList())

fun PartidoEntity.aModelo() = Partido(
    id = id,
    ronda = ronda,
    localId = localId,
    visitanteId = visitanteId,
    golesLocal = golesLocal,
    golesVisitante = golesVisitante,
    ganadorPenalesId = ganadorPenalesId,
    cancelado = cancelado
)

fun Partido.aEntity() = PartidoEntity(
    id = id,
    ronda = ronda,
    localId = localId,
    visitanteId = visitanteId,
    golesLocal = golesLocal,
    golesVisitante = golesVisitante,
    ganadorPenalesId = ganadorPenalesId,
    cancelado = cancelado
)

fun TarjetaEntity.aModelo() =
    Tarjeta(id = id, partidoId = partidoId, jugadorId = jugadorId, tipo = tipo, minuto = minuto)

fun Tarjeta.aEntity() =
    TarjetaEntity(id = id, partidoId = partidoId, jugadorId = jugadorId, tipo = tipo, minuto = minuto)
