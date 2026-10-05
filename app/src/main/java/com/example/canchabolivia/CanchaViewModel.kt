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

    fun iniciarSesion(correo: String, clave: String): String? {
        val encontrado = usuarios.find {
            it.correo.equals(correo.trim(), ignoreCase = true) && it.clave == clave.trim()
        }

        return if (encontrado != null) {
            usuarioLogueado = encontrado
            this.rol = encontrado.rol
            null
        } else {
            "Correo o contraseña incorrectos"
        }
    }

    fun cerrarSesion() {
        usuarioLogueado = null
    }

    // --- ESTADO GENERAL DE LA APP ---
    var rol by mutableStateOf(Rol.JUGADOR)

    val equipos = listOf(
        Equipo(1, "Bolívar", "BOL"),
        Equipo(2, "The Strongest", "STR"),
        Equipo(4, "Wilstermann", "WIL"),
        Equipo(5, "Oriente Petrolero", "OPE"),
        Equipo(7, "Nacional Potosí", "NAC"),
        Equipo(8, "Aurora", "AUR")
    )

    val jugadores = equipos.flatMap { equipo ->
        listOf(
            Jugador(equipo.id * 10 + 1, "Juan Pérez", equipo.id, 10),
            Jugador(equipo.id * 10 + 2, "Diego ${equipo.sigla}", equipo.id, 7)
        )
    }

    val jugadorActualId = 11

    val horarios = listOf(
        HorarioMedico(1, 2, "2026-05-11", "09:00"),
        HorarioMedico(2, 2, "2026-05-11", "09:30"),
        HorarioMedico(3, 2, "2026-05-12", "10:00")
    )

    var fichas by mutableStateOf(listOf<FichaMedica>())
    var torneo by mutableStateOf(Torneo("Copa La Paz", Formato.LIGA, mutableListOf(1, 2, 4, 5)))
    var partidos by mutableStateOf(listOf<Partido>())

    fun equipo(id: Int): Equipo = equipos.first { it.id == id }
    fun jugador(id: Int): Jugador = jugadores.first { it.id == id }

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

    fun responder(ficha: FichaMedica, aceptar: Boolean) {
        ficha.estado = if (aceptar) EstadoFicha.ACEPTADA else EstadoFicha.RECHAZADA
        fichas = fichas.toList()
    }

    fun evaluar(ficha: FichaMedica, apto: Boolean) {
        ficha.estado = if (apto) EstadoFicha.APTO else EstadoFicha.NO_APTO
        fichas = fichas.toList()
    }

    fun generar(sortear: Boolean): String {
        val ids = if (sortear) torneo.equipoIds.shuffled() else torneo.equipoIds
        val lista = mutableListOf<Partido>()
        var pId = 1
        for (i in ids.indices) {
            for (j in i + 1 until ids.size) {
                lista.add(Partido(pId++, 1, ids[i], ids[j]))
            }
        }
        partidos = lista
        return "Fixture generado"
    }

    fun avanzar(): String {
        return "Siguiente ronda configurada"
    }

    fun campeon(): String? {
        return null
    }
}