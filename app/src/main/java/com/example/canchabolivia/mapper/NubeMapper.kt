package com.example.canchabolivia.mapper

import com.example.canchabolivia.EstadoFicha
import com.example.canchabolivia.Equipo
import com.example.canchabolivia.FichaMedica
import com.example.canchabolivia.Formato
import com.example.canchabolivia.Jugador
import com.example.canchabolivia.Partido
import com.example.canchabolivia.Tarjeta
import com.example.canchabolivia.TipoTarjeta
import com.example.canchabolivia.Torneo
import com.example.canchabolivia.dto.DatosDto
import com.example.canchabolivia.dto.EquipoDto
import com.example.canchabolivia.dto.FichaDto
import com.example.canchabolivia.dto.JugadorDto
import com.example.canchabolivia.dto.PartidoDto
import com.example.canchabolivia.dto.TarjetaDto
import com.example.canchabolivia.dto.TorneoDto

// Todo lo que trae cargar_datos, ya convertido al modelo de la app.
// [GUIA DOCENTE - MAPPER 2] Convierte JSON remoto a modelos de dominio.
// Para otra tabla: agregar DTO, conversion aModelo y aCuerpo cuando se escriba.
data class DatosNube(
    val equipos: List<Equipo>,
    val jugadores: List<Jugador>,
    val fichas: List<FichaMedica>,
    val torneo: Torneo?,
    val partidos: List<Partido>,
    val tarjetas: List<Tarjeta>
)

// ---------- DTO (nube) -> modelo (app). Devuelven null si falta algún dato. ----------

private inline fun <reified E : Enum<E>> enumDe(texto: String?): E? =
    texto?.let { t -> enumValues<E>().firstOrNull { it.name == t } }

fun EquipoDto.aModelo(): Equipo? {
    return Equipo(
        id = id ?: return null,
        nombre = nombre ?: return null,
        sigla = sigla ?: return null
    )
}

fun JugadorDto.aModelo(): Jugador? {
    return Jugador(
        id = id ?: return null,
        nombre = nombre ?: return null,
        equipoId = equipoId ?: return null,
        dorsal = dorsal ?: 0
    )
}

fun FichaDto.aModelo(): FichaMedica? {
    return FichaMedica(
        id = id ?: return null,
        jugadorId = jugadorId ?: return null,
        horarioId = horarioId ?: return null,
        estado = enumDe<EstadoFicha>(estado) ?: EstadoFicha.PENDIENTE
    )
}

fun TorneoDto.aModelo(): Torneo? {
    return Torneo(
        nombre = nombre ?: return null,
        formato = enumDe<Formato>(formato) ?: return null,
        equipoIds = (equipoIds ?: emptyList()).toMutableList()
    )
}

fun PartidoDto.aModelo(): Partido? {
    return Partido(
        id = id ?: return null,
        ronda = ronda ?: return null,
        localId = localId ?: return null,
        visitanteId = visitanteId,
        golesLocal = golesLocal,
        golesVisitante = golesVisitante,
        ganadorPenalesId = ganadorPenalesId,
        cancelado = (cancelado ?: 0) != 0,
        grupo = grupo
    )
}

fun TarjetaDto.aModelo(): Tarjeta? {
    return Tarjeta(
        id = id ?: return null,
        partidoId = partidoId ?: return null,
        jugadorId = jugadorId ?: return null,
        tipo = enumDe<TipoTarjeta>(tipo) ?: return null,
        minuto = minuto ?: 0
    )
}

fun DatosDto.aModelo() = DatosNube(
    equipos = equipos.orEmpty().mapNotNull { it.aModelo() },
    jugadores = jugadores.orEmpty().mapNotNull { it.aModelo() },
    fichas = fichas.orEmpty().mapNotNull { it.aModelo() },
    torneo = torneo?.aModelo(),
    partidos = partidos.orEmpty().mapNotNull { it.aModelo() },
    tarjetas = tarjetas.orEmpty().mapNotNull { it.aModelo() }
)

// ---------- modelo (app) -> cuerpo JSON para las funciones ----------
// Las llaves son los nombres de las columnas en SQLite Cloud.

fun Equipo.aCuerpo(): Map<String, Any?> = mapOf("id" to id, "nombre" to nombre, "sigla" to sigla)

fun Jugador.aCuerpo(): Map<String, Any?> =
    mapOf("id" to id, "nombre" to nombre, "equipo_id" to equipoId, "dorsal" to dorsal)

fun FichaMedica.aCuerpo(): Map<String, Any?> =
    mapOf("id" to id, "jugador_id" to jugadorId, "horario_id" to horarioId, "estado" to estado.name)

fun Torneo.aCuerpo(): Map<String, Any?> =
    mapOf("nombre" to nombre, "formato" to formato.name, "equipo_ids" to equipoIds.toList())

fun Partido.aCuerpo(): Map<String, Any?> = mapOf(
    "id" to id,
    "ronda" to ronda,
    "local_id" to localId,
    "visitante_id" to visitanteId,
    "goles_local" to golesLocal,
    "goles_visitante" to golesVisitante,
    "ganador_penales_id" to ganadorPenalesId,
    "cancelado" to if (cancelado) 1 else 0,
    "grupo" to grupo
)

fun Tarjeta.aCuerpo(): Map<String, Any?> = mapOf(
    "id" to id,
    "partido_id" to partidoId,
    "jugador_id" to jugadorId,
    "tipo" to tipo.name,
    "minuto" to minuto
)
