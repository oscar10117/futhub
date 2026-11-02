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
    ELIMINACION("Eliminación directa"),
    GRUPOS("Grupos + eliminación (tipo Mundial)")
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
    val ganadorPenalesId: Int? = null,
    val cancelado: Boolean = false,
    // Solo en formato GRUPOS: 0 = Grupo A, 1 = Grupo B... null = partido de eliminación.
    val grupo: Int? = null
) {
    fun finalizado(): Boolean = golesLocal != null && golesVisitante != null

    // Pase libre (sin visitante) clasifica directo. Si hay empate en el
    // marcador, decide el ganador de penales; sin penales cargados todavía
    // no hay ganador (avanzar() debe esperar a que se definan).
    fun ganadorId(): Int? = when {
        cancelado -> null
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
    fun tabla(torneo: Torneo, partidos: List<Partido>): List<Posicion> = tabla(torneo.equipoIds, partidos)

    // --- FASE DE GRUPOS (formato tipo Mundial) ---
    fun letraGrupo(grupo: Int): String = ('A' + grupo).toString()

    // Cuántos grupos: de 4 a 7 equipos, 2 grupos; desde 8, grupos de 4 (como el Mundial).
    fun cantidadGrupos(equipos: Int): Int = if (equipos < 8) 2 else equipos / 4

    // Reparte los equipos en grupos (1ro al A, 2do al B, ...) y arma un
    // todos contra todos dentro de cada grupo. Todos estos partidos son ronda 1.
    fun armarGrupos(equipoIds: List<Int>, idInicial: Int): List<Partido> {
        val cantidad = cantidadGrupos(equipoIds.size)
        val partidos = mutableListOf<Partido>()
        var siguienteId = idInicial
        for (g in 0 until cantidad) {
            val delGrupo = equipoIds.filterIndexed { i, _ -> i % cantidad == g }
            for (i in delGrupo.indices) {
                for (j in i + 1 until delGrupo.size) {
                    partidos.add(Partido(siguienteId++, 1, delGrupo[i], delGrupo[j], grupo = g))
                }
            }
        }
        return partidos
    }

    // Equipos de un grupo, sacados de sus partidos (no hace falta guardarlos aparte).
    fun equiposDelGrupo(partidos: List<Partido>, grupo: Int): List<Int> =
        partidos.filter { it.grupo == grupo }
            .flatMap { listOfNotNull(it.localId, it.visitanteId) }
            .distinct()

    // Los 2 primeros de cada grupo, ordenados para cruzarse como en el Mundial:
    // 1ro A vs 2do B, 1ro B vs 2do A, 1ro C vs 2do D... (si sobra un grupo, su 1ro vs su 2do).
    fun clasificados(partidos: List<Partido>): List<Int> {
        val grupos = partidos.mapNotNull { it.grupo }.distinct().sorted()
        val podio = grupos.map { g ->
            tabla(equiposDelGrupo(partidos, g), partidos.filter { it.grupo == g }).take(2).map { it.equipoId }
        }
        val orden = mutableListOf<Int>()
        var i = 0
        while (i < podio.size) {
            if (i + 1 < podio.size) {
                val a = podio[i]
                val b = podio[i + 1]
                orden += listOfNotNull(a.getOrNull(0), b.getOrNull(1), b.getOrNull(0), a.getOrNull(1))
                i += 2
            } else {
                orden += podio[i]
                i++
            }
        }
        return orden
    }

    fun tabla(equipoIds: List<Int>, partidos: List<Partido>): List<Posicion> {
        val mapa = equipoIds.associateWith { Posicion(it) }.toMutableMap()
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
