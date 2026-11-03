package com.example.canchabolivia.mapper

import com.example.canchabolivia.Equipo
import com.example.canchabolivia.Jugador
import com.example.canchabolivia.database.EquipoEntity
import com.example.canchabolivia.database.JugadorEntity

// Entity (Room) a modelo de la app, y al revés para lo que la app modifica.
fun EquipoEntity.aModelo() = Equipo(id = id, nombre = nombre, sigla = sigla)

fun Equipo.aEntity() = EquipoEntity(id = id, nombre = nombre, sigla = sigla)

fun JugadorEntity.aModelo() = Jugador(id = id, nombre = nombre, equipoId = equipoId, dorsal = dorsal)

fun Jugador.aEntity() = JugadorEntity(id = id, nombre = nombre, equipoId = equipoId, dorsal = dorsal)
