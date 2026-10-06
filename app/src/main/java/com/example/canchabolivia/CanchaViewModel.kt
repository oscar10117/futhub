package com.example.canchabolivia

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class CanchaViewModel : ViewModel() {

    // --- AUTENTICACIÓN Y LOGIN ---
    var usuarioLogueado by mutableStateOf<Usuario?>(null)

    val usuarios = listOf(
        Usuario(1, "Juan Pérez", "jugador@futhub.com", "123", Rol.JUGADOR),
        Usuario(2, "Dra. Ana Rojas", "medico@futhub.com", "123", Rol.MEDICO),
        Usuario(3, "Luis Flores", "arbitro@futhub.com", "123", Rol.ARBITRO),
        Usuario(4, "Organización La Paz", "admin@futhub.com", "123", Rol.ORGANIZADOR)
    )

    // El rol activo siempre viene del usuario que inició sesión: ya no existe
    // un selector manual, así que cada quien solo ve y usa su propia pantalla.
    val rol: Rol get() = usuarioLogueado?.rol ?: Rol.JUGADOR

    fun iniciarSesion(correo: String, clave: String): String? {
        val encontrado = usuarios.find {
            it.correo.equals(correo.trim(), ignoreCase = true) && it.clave == clave.trim()
        }

        return if (encontrado != null) {
            usuarioLogueado = encontrado
            null
        } else {
            "Correo o contraseña incorrectos"
        }
    }

    fun cerrarSesion() {
        usuarioLogueado = null
    }

    // --- EQUIPOS (el organizador puede agregar/eliminar) ---
    var equipos by mutableStateOf(
        listOf(
            Equipo(1, "Bolívar", "BOL"),
            Equipo(2, "The Strongest", "STR"),
            Equipo(4, "Wilstermann", "WIL"),
            Equipo(5, "Oriente Petrolero", "OPE"),
            Equipo(7, "Nacional Potosí", "NAC"),
            Equipo(8, "Aurora", "AUR")
        )
    )

    var jugadores by mutableStateOf(
        equipos.flatMap { equipo ->
            listOf(
                Jugador(equipo.id * 10 + 1, "Juan Pérez", equipo.id, 10),
                Jugador(equipo.id * 10 + 2, "Diego ${equipo.sigla}", equipo.id, 7)
            )
        }
    )

    val jugadorActualId = 11

    fun agregarEquipo(nombre: String, sigla: String) {
        val nuevoId = (equipos.maxOfOrNull { it.id } ?: 0) + 1
        equipos = equipos + Equipo(nuevoId, nombre, sigla.uppercase())
        jugadores = jugadores + listOf(
            Jugador(nuevoId * 10 + 1, "Jugador 1", nuevoId, 10),
            Jugador(nuevoId * 10 + 2, "Jugador 2", nuevoId, 7)
        )
    }

    // Borrar un equipo también limpia todo lo que dependía de él (jugadores,
    // su lugar en el torneo, partidos ya programados, fichas y tarjetas) para
    // no dejar referencias colgando que después rompan la pantalla.
    fun eliminarEquipo(equipoId: Int) {
        val idsJugadoresEliminados = jugadores.filter { it.equipoId == equipoId }.map { it.id }

        equipos = equipos.filterNot { it.id == equipoId }
        jugadores = jugadores.filterNot { it.equipoId == equipoId }
        torneo = torneo.copy(equipoIds = (torneo.equipoIds - equipoId).toMutableList())
        partidos = partidos.filterNot { it.localId == equipoId || it.visitanteId == equipoId }
        fichas = fichas.filterNot { it.jugadorId in idsJugadoresEliminados }
        tarjetas = tarjetas.filterNot { it.jugadorId in idsJugadoresEliminados }
    }

    fun equipo(id: Int): Equipo = equipos.firstOrNull { it.id == id } ?: Equipo(id, "Equipo eliminado", "—")
    fun jugador(id: Int): Jugador = jugadores.firstOrNull { it.id == id } ?: Jugador(id, "Jugador eliminado", -1, 0)

    // --- FICHAS MÉDICAS ---
    val horarios = listOf(
        HorarioMedico(1, 2, "2026-05-11", "09:00"),
        HorarioMedico(2, 2, "2026-05-11", "09:30"),
        HorarioMedico(3, 2, "2026-05-12", "10:00")
    )

    var fichas by mutableStateOf(listOf<FichaMedica>())

    fun disponible(horarioId: Int): Boolean {
        return fichas.none { it.horarioId == horarioId && it.estado != EstadoFicha.RECHAZADA }
    }

    fun habilitado(jugadorId: Int): Boolean {
        return fichas.any { it.jugadorId == jugadorId && it.estado == EstadoFicha.APTO }
    }

    fun reservar(horarioId: Int): String {
        val nueva = FichaMedica(fichas.size + 1, jugadorActualId, horarioId)
        fichas = fichas + nueva
        return "Solicitud enviada correctamente"
    }

    // Reemplaza la ficha por una copia con el nuevo estado en vez de mutarla
    // in-place: así la lista cambia de verdad y la pantalla del médico se
    // actualiza al tocar "Aceptar"/"Rechazar" (antes se quedaba trancada).
    fun responder(fichaId: Int, aceptar: Boolean) {
        fichas = fichas.map { ficha ->
            if (ficha.id == fichaId) {
                ficha.copy(estado = if (aceptar) EstadoFicha.ACEPTADA else EstadoFicha.RECHAZADA)
            } else ficha
        }
    }

    fun evaluar(fichaId: Int, apto: Boolean) {
        fichas = fichas.map { ficha ->
            if (ficha.id == fichaId) {
                ficha.copy(estado = if (apto) EstadoFicha.APTO else EstadoFicha.NO_APTO)
            } else ficha
        }
    }

    // --- TORNEO Y PARTIDOS ---
    var torneo by mutableStateOf(Torneo("Copa La Paz", Formato.LIGA, mutableListOf(1, 2, 4, 5)))
    var partidos by mutableStateOf(listOf<Partido>())

    fun generar(sortear: Boolean): String {
        val ids = if (sortear) torneo.equipoIds.shuffled() else torneo.equipoIds

        partidos = when (torneo.formato) {
            // Todos contra todos: un partido por cada par de equipos.
            Formato.LIGA -> {
                val lista = mutableListOf<Partido>()
                var pId = 1
                for (i in ids.indices) {
                    for (j in i + 1 until ids.size) {
                        lista.add(Partido(pId++, 1, ids[i], ids[j]))
                    }
                }
                lista
            }
            // Eliminación directa: solo se arma la primera ronda, las
            // siguientes salen de avanzar() con los ganadores reales.
            Formato.ELIMINACION -> MotorTorneo.siguienteRondaEliminacion(ids, ronda = 1, idInicial = 1)
        }

        return "Fixture generado"
    }

    // Nunca se muta un Partido existente: siempre se reemplaza por una
    // copia dentro de la lista (igual que con las fichas médicas).
    fun registrarResultado(partidoId: Int, golesLocal: Int, golesVisitante: Int, ganadorPenalesId: Int? = null) {
        partidos = partidos.map { partido ->
            if (partido.id == partidoId) {
                partido.copy(golesLocal = golesLocal, golesVisitante = golesVisitante, ganadorPenalesId = ganadorPenalesId)
            } else partido
        }
    }

    // Toma los resultados de la última ronda de eliminación directa, saca
    // a los ganadores y arma la ronda siguiente. Si algún partido de la
    // última ronda todavía no tiene resultado (o quedó empatado sin
    // penales), avisa en vez de avanzar con datos incompletos.
    fun avanzar(): String {
        if (torneo.formato != Formato.ELIMINACION) return "Esta acción es solo para eliminación directa"
        if (partidos.isEmpty()) return "Primero genera el fixture"

        val ultimaRonda = partidos.maxOf { it.ronda }
        val partidosUltimaRonda = partidos.filter { it.ronda == ultimaRonda }

        val haySinJugar = partidosUltimaRonda.any { it.visitanteId != null && !it.finalizado() }
        if (haySinJugar) return "Faltan resultados de la ronda $ultimaRonda"

        val ganadores = partidosUltimaRonda.map {
            it.ganadorId() ?: return "Hay un empate sin definir por penales en la ronda $ultimaRonda"
        }
        if (ganadores.size == 1) return "El torneo ya tiene campeón"

        val idInicial = partidos.maxOf { it.id } + 1
        partidos = partidos + MotorTorneo.siguienteRondaEliminacion(ganadores, ultimaRonda + 1, idInicial)
        return "Ronda ${ultimaRonda + 1} generada"
    }

    // Liga: hay campeón cuando se jugaron todos los partidos (el primero
    // de la tabla). Eliminación: hay campeón cuando la última ronda quedó
    // en un solo partido ya definido.
    fun campeon(): String? {
        if (partidos.isEmpty()) return null

        return when (torneo.formato) {
            Formato.LIGA -> {
                if (!partidos.all { it.finalizado() }) return null
                MotorTorneo.tabla(torneo, partidos).firstOrNull()?.let { equipo(it.equipoId).nombre }
            }
            Formato.ELIMINACION -> {
                val ultimaRonda = partidos.maxOf { it.ronda }
                val partidosUltimaRonda = partidos.filter { it.ronda == ultimaRonda }
                if (partidosUltimaRonda.size != 1) return null
                partidosUltimaRonda.single().ganadorId()?.let { equipo(it).nombre }
            }
        }
    }

    // --- TARJETAS (árbitro) ---
    var tarjetas by mutableStateOf(listOf<Tarjeta>())

    fun agregarTarjeta(partidoId: Int, jugadorId: Int, tipo: TipoTarjeta, minuto: Int) {
        val nuevaId = (tarjetas.maxOfOrNull { it.id } ?: 0) + 1
        tarjetas = tarjetas + Tarjeta(nuevaId, partidoId, jugadorId, tipo, minuto)
    }

    fun tarjetasDe(partidoId: Int): List<Tarjeta> = tarjetas.filter { it.partidoId == partidoId }
}
