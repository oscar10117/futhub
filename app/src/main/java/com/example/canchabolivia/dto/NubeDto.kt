package com.example.canchabolivia.dto

import com.google.gson.annotations.SerializedName

// Copia fiel del JSON que devuelven las Edge Functions (columnas de las tablas).
// Todo es nullable: si falta un dato, el mapper descarta esa fila en vez de romper la app.

// [GUIA DOCENTE - DTO NUBE] Datos tal como llegan desde las Edge Functions.
// Para una tabla/campo nuevo, ajustar el DTO y NubeMapper.
data class EquipoDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("sigla") val sigla: String?
)

data class JugadorDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("equipo_id") val equipoId: Int?,
    @SerializedName("dorsal") val dorsal: Int?
)

data class FichaDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("jugador_id") val jugadorId: Int?,
    @SerializedName("horario_id") val horarioId: Int?,
    @SerializedName("estado") val estado: String?
)

data class TorneoDto(
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("formato") val formato: String?,
    @SerializedName("equipo_ids") val equipoIds: List<Int>?
)

data class PartidoDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("ronda") val ronda: Int?,
    @SerializedName("local_id") val localId: Int?,
    @SerializedName("visitante_id") val visitanteId: Int?,
    @SerializedName("goles_local") val golesLocal: Int?,
    @SerializedName("goles_visitante") val golesVisitante: Int?,
    @SerializedName("ganador_penales_id") val ganadorPenalesId: Int?,
    @SerializedName("cancelado") val cancelado: Int?,
    @SerializedName("grupo") val grupo: Int?
)

data class TarjetaDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("partido_id") val partidoId: Int?,
    @SerializedName("jugador_id") val jugadorId: Int?,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("minuto") val minuto: Int?
)

// Respuesta de la función cargar_datos.
data class DatosDto(
    @SerializedName("equipos") val equipos: List<EquipoDto>?,
    @SerializedName("jugadores") val jugadores: List<JugadorDto>?,
    @SerializedName("fichas") val fichas: List<FichaDto>?,
    @SerializedName("torneo") val torneo: TorneoDto?,
    @SerializedName("partidos") val partidos: List<PartidoDto>?,
    @SerializedName("tarjetas") val tarjetas: List<TarjetaDto>?
)
