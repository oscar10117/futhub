package com.example.canchabolivia

// Modelo de datos de la app (versión simplificada que usa la UI/ViewModel).
// El diagrama de clases completo (con todos los metodos del dominio) vive en
// modelodiagrama/ModeloDiagrama.kt y es solo documentacion, no se usa en tiempo de ejecucion.

enum class Rol(val titulo: String) {
    JUGADOR("Jugador"),
    MEDICO("Médico"),
    ARBITRO("Árbitro"),
    ORGANIZADOR("Organizador")
}

enum class Formato(val titulo: String) {
    LIGA("Todos contra todos"),
    ELIMINACION("Eliminación directa")
}

enum class EstadoFicha {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA,
    ATENDIDA,
    APTO,
    NO_APTO
}

enum class TipoTarjeta(val titulo: String) {
    AMARILLA("Amarilla"),
    ROJA("Roja")
}

data class Usuario(
    val id: Int,
    val nombre: String,
    val correo: String,
    val clave: String,
    val rol: Rol
)

data class Equipo(
    val id: Int,
    val nombre: String,
    val sigla: String
)

data class Jugador(
    val id: Int,
    val nombre: String,
    val equipoId: Int,
    val dorsal: Int
)

data class HorarioMedico(
    val id: Int,
    val medicoId: Int,
    val fecha: String,
    val hora: String
)

// Ojo: "estado" es `val` a propósito. Si fuera `var` y se mutara directo sobre
// el objeto, una lista nueva construida a partir de esa mutación queda "igual"
// (misma referencia de objeto, mismos campos) a ojos de Compose y la pantalla
// no se actualiza. Por eso todo cambio de estado se hace con `copy()` desde el
// ViewModel (ver responder/evaluar en CanchaViewModel).
data class FichaMedica(
    val id: Int,
    val jugadorId: Int,
    val horarioId: Int,
    val estado: EstadoFicha = EstadoFicha.PENDIENTE
)

data class Torneo(
    var nombre: String,
    var formato: Formato,
    var equipoIds: MutableList<Int>
)

// Igual que FichaMedica: los goles son `val`, se actualizan con copy() para
// que la pantalla de Torneo/Partidos refleje el resultado al instante.
data class Partido(
    val id: Int,
    val ronda: Int,
    val localId: Int,
    val visitanteId: Int?,
    val golesLocal: Int? = null,
    val golesVisitante: Int? = null,
    val ganadorPenalesId: Int? = null
) {
    fun finalizado(): Boolean = golesLocal != null && golesVisitante != null

    // Pase libre (sin visitante) clasifica directo. Si hay empate en el
    // marcador, decide el ganador de penales; sin penales cargados todavía
    // no hay ganador (avanzar() debe esperar a que se definan).
    fun ganadorId(): Int? = when {
        visitanteId == null -> localId
        !finalizado() -> null
        golesLocal!! > golesVisitante!! -> localId
        golesVisitante!! > golesLocal!! -> visitanteId
        else -> ganadorPenalesId
    }
}

data class Tarjeta(
    val id: Int,
    val partidoId: Int,
    val jugadorId: Int,
    val tipo: TipoTarjeta,
    val minuto: Int
)

data class Posicion(
    val equipoId: Int,
    var jugados: Int = 0,
    var ganados: Int = 0,
    var empatados: Int = 0,
    var perdidos: Int = 0,
    var favor: Int = 0,
    var contra: Int = 0,
    var diferencia: Int = 0,
    var puntos: Int = 0
)

object MotorTorneo {
    fun tabla(torneo: Torneo, partidos: List<Partido>): List<Posicion> {
        val mapa = torneo.equipoIds.associateWith { Posicion(it) }.toMutableMap()
        for (p in partidos.filter { it.finalizado() && it.visitanteId != null }) {
            val loc = mapa[p.localId] ?: continue
            val vis = mapa[p.visitanteId] ?: continue
            val gl = p.golesLocal!!
            val gv = p.golesVisitante!!

            loc.jugados++
            vis.jugados++
            loc.favor += gl
            loc.contra += gv
            vis.favor += gv
            vis.contra += gl
            loc.diferencia = loc.favor - loc.contra
            vis.diferencia = vis.favor - vis.contra

            when {
                gl > gv -> {
                    loc.ganados++
                    loc.puntos += 3
                    vis.perdidos++
                }
                gv > gl -> {
                    vis.ganados++
                    vis.puntos += 3
                    loc.perdidos++
                }
                else -> {
                    loc.empatados++
                    loc.puntos += 1
                    vis.empatados++
                    vis.puntos += 1
                }
            }
        }
        return mapa.values.sortedWith(
            compareByDescending<Posicion> { it.puntos }
                .thenByDescending { it.diferencia }
                .thenByDescending { it.favor }
        )
    }

    // Arma los cruces de una ronda de eliminación directa emparejando la
    // lista en orden (1ro vs 2do, 3ro vs 4to, ...). Si sobra un equipo al
    // final, le toca pase libre. Se usa tanto para la ronda 1 (equipos del
    // torneo) como para las siguientes (ganadores de la ronda anterior).
    fun siguienteRondaEliminacion(equipoIds: List<Int>, ronda: Int, idInicial: Int): List<Partido> {
        val partidos = mutableListOf<Partido>()
        var siguienteId = idInicial
        for (i in equipoIds.indices step 2) {
            partidos.add(Partido(siguienteId++, ronda, equipoIds[i], equipoIds.getOrNull(i + 1)))
        }
        return partidos
    }
}