package com.example.canchabolivia

// El rol distingue las vistas de demostración; no reemplaza una autenticación real.
enum class Rol(val titulo: String) { JUGADOR("Jugador"), MEDICO("Médico"), ARBITRO("Árbitro"), ORGANIZADOR("Organizador") }
enum class EstadoFicha { PENDIENTE, ACEPTADA, RECHAZADA, ATENDIDA }
enum class Formato(val titulo: String) { LIGA("Todos contra todos"), ELIMINACION("Eliminación directa") }
data class Usuario(val id: Int, val nombre: String, val rol: Rol)
data class Equipo(val id: Int, val nombre: String, val sigla: String)
data class Jugador(val id: Int, val nombre: String, val dorsal: Int, val equipoId: Int)
data class HorarioMedico(val id: Int, val medicoId: Int, val fecha: String, val hora: String)
data class FichaMedica(val id: Int, val jugadorId: Int, val horarioId: Int, val torneoId: Int,
    val estado: EstadoFicha = EstadoFicha.PENDIENTE)
// La habilitación es específica de un torneo y la emite un médico.
data class Habilitacion(val id: Int, val jugadorId: Int, val medicoId: Int, val torneoId: Int, val apto: Boolean)
data class Torneo(val id: Int, val nombre: String, val organizadorId: Int, val formato: Formato, val equipoIds: List<Int>)
data class Partido(val id: Int, val torneoId: Int, val ronda: Int, val localId: Int, val visitanteId: Int?,
    val arbitroId: Int, val golesLocal: Int? = null, val golesVisitante: Int? = null, val ganadorPenalesId: Int? = null) {
    fun finalizado() = visitanteId == null || (golesLocal != null && golesVisitante != null)
    fun ganador(): Int? = when {
        visitanteId == null -> localId
        golesLocal == null || golesVisitante == null -> null
        golesLocal > golesVisitante -> localId
        golesVisitante > golesLocal -> visitanteId
        else -> ganadorPenalesId
    }
}
data class Posicion(val equipoId: Int, val jugados: Int, val ganados: Int, val empatados: Int,
    val perdidos: Int, val golesFavor: Int, val golesContra: Int) {
    val puntos: Int get() = ganados * 3 + empatados
    val diferencia: Int get() = golesFavor - golesContra
}

object MotorTorneo {
    // Método del círculo: cada pareja se enfrenta una vez; un null representa descanso.
    fun liga(torneo: Torneo, orden: List<Int>, arbitro: Int): List<Partido> {
        require(orden.size >= 2 && orden.toSet() == torneo.equipoIds.toSet() && orden.distinct().size == orden.size)
        val rueda = orden.map { it as Int? }.toMutableList()
        if (rueda.size % 2 != 0) rueda.add(null)
        val partidos = mutableListOf<Partido>()
        repeat(rueda.size - 1) { ronda ->
            for (i in 0 until rueda.size / 2) {
                val a = rueda[i]; val b = rueda[rueda.lastIndex - i]
                if (a != null && b != null) partidos.add(Partido(partidos.size + 1, torneo.id, ronda + 1,
                    if (ronda % 2 == 0) a else b, if (ronda % 2 == 0) b else a, arbitro))
            }
            rueda.add(1, rueda.removeAt(rueda.lastIndex))
        }
        return partidos
    }
    // Se completa la llave a la siguiente potencia de dos, con pases libres.
    fun eliminacion(torneo: Torneo, orden: List<Int>, arbitro: Int): List<Partido> {
        require(orden.size >= 2 && orden.toSet() == torneo.equipoIds.toSet() && orden.distinct().size == orden.size)
        var plazas = 2
        while (plazas < orden.size) plazas *= 2
        val libres = plazas - orden.size
        val partidos = mutableListOf<Partido>()
        var indice = 0
        repeat(libres) { partidos.add(Partido(partidos.size + 1, torneo.id, 1, orden[indice++], null, arbitro)) }
        while (indice < orden.size) {
            partidos.add(Partido(partidos.size + 1, torneo.id, 1, orden[indice], orden[indice + 1], arbitro))
            indice += 2
        }
        return partidos
    }
    fun siguienteRonda(actuales: List<Partido>, siguienteId: Int): List<Partido> {
        require(actuales.size > 1 && actuales.all { it.finalizado() && it.ganador() != null })
        return actuales.map { it.ganador()!! }.chunked(2).mapIndexed { i, pareja ->
            Partido(siguienteId + i, actuales.first().torneoId, actuales.first().ronda + 1,
                pareja[0], pareja[1], actuales.first().arbitroId)
        }
    }
    fun tabla(torneo: Torneo, partidos: List<Partido>): List<Posicion> = torneo.equipoIds.map { equipo ->
        val jugados = partidos.filter { it.torneoId == torneo.id && it.visitanteId != null && it.finalizado() && (it.localId == equipo || it.visitanteId == equipo) }
        var gf = 0; var gc = 0; var g = 0; var e = 0; var p = 0
        jugados.forEach {
            val a = if (it.localId == equipo) it.golesLocal!! else it.golesVisitante!!
            val b = if (it.localId == equipo) it.golesVisitante!! else it.golesLocal!!
            gf += a; gc += b
            if (a > b) g++ else if (a == b) e++ else p++
        }
        Posicion(equipo, jugados.size, g, e, p, gf, gc)
    }.sortedWith(compareByDescending<Posicion> { it.puntos }.thenByDescending { it.diferencia }.thenByDescending { it.golesFavor }.thenBy { it.equipoId })
}
