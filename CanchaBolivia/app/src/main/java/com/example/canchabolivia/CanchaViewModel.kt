package com.example.canchabolivia

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// =====================================================
// CANCHAVIEWMODEL.KT
// Aquí vive la lógica de la app y los datos que cambian.
// La pantalla (MainActivity) solo muestra lo que hay aquí
// y llama a estas funciones cuando el usuario toca un botón.
// =====================================================
class CanchaViewModel : ViewModel() {

    // ---------- LOGIN ----------

    // Lista de usuarios de prueba (clave "123" para todos).
    // Juan Pérez tiene jugadorId = 11, que es su jugador en la lista de jugadores.
    val usuarios = listOf(
        Usuario(1, "Juan Pérez", "jugador@futhub.com", "123", Rol.JUGADOR, jugadorId = 11),
        Usuario(2, "Dra. Ana Rojas", "medico@futhub.com", "123", Rol.MEDICO),
        Usuario(3, "Luis Flores", "arbitro@futhub.com", "123", Rol.ARBITRO),
        Usuario(4, "Organización La Paz", "admin@futhub.com", "123", Rol.ORGANIZADOR)
    )

    // Usuario que inició sesión (null = nadie). Con mutableStateOf, cuando cambia
    // la pantalla se redibuja sola. "private set" evita que la pantalla lo modifique
    // directamente: solo se cambia con iniciarSesion() y cerrarSesion().
    var usuarioLogueado by mutableStateOf<Usuario?>(null)
        private set

    // El rol y el jugador actual salen del usuario que inició sesión
    val rol: Rol get() = usuarioLogueado?.rol ?: Rol.JUGADOR
    val jugadorActualId: Int get() = usuarioLogueado?.jugadorId ?: 0

    // Busca un usuario con ese correo y clave.
    // Devuelve null si todo salió bien, o el mensaje de error si no lo encontró.
    fun iniciarSesion(correo: String, clave: String): String? {
        val usuario = usuarios.find {
            it.correo.equals(correo.trim(), ignoreCase = true) && it.clave == clave.trim()
        }
        usuarioLogueado = usuario
        return if (usuario == null) "Correo o contraseña incorrectos" else null
    }

    // Al poner el usuario en null, la app vuelve a la pantalla de login
    fun cerrarSesion() {
        usuarioLogueado = null
    }

    // ---------- DATOS DE EJEMPLO ----------

    val equipos = listOf(
        Equipo(1, "Bolívar", "BOL"),
        Equipo(2, "The Strongest", "STR"),
        Equipo(4, "Wilstermann", "WIL"),
        Equipo(5, "Oriente Petrolero", "OPE"),
        Equipo(7, "Nacional Potosí", "NAC"),
        Equipo(8, "Aurora", "AUR")
    )

    // Se crean 2 jugadores por equipo. El id sale de: id del equipo * 10 + 1 (o + 2).
    // Ej.: el equipo 1 tiene los jugadores 11 y 12.
    val jugadores = equipos.flatMap { equipo ->
        listOf(
            Jugador(equipo.id * 10 + 1, "Juan Pérez", equipo.id, 10),
            Jugador(equipo.id * 10 + 2, "Diego ${equipo.sigla}", equipo.id, 7)
        )
    }

    // Horarios que ofrece la doctora para las revisiones
    val horarios = listOf(
        HorarioMedico(1, "2026-05-11", "09:00"),
        HorarioMedico(2, "2026-05-11", "09:30"),
        HorarioMedico(3, "2026-05-12", "10:00")
    )

    // El torneo con los 4 equipos que participan (ids 1, 2, 4 y 5)
    val torneo = Torneo("Copa La Paz", listOf(1, 2, 4, 5))

    // ---------- ESTADO QUE CAMBIA ----------

    // Fichas médicas solicitadas. Empieza vacía y crece cuando un jugador reserva.
    var fichas by mutableStateOf(listOf<FichaMedica>())
        private set

    // Partidos del fixture. Empieza vacía hasta que el organizador la genera.
    var partidos by mutableStateOf(listOf<Partido>())
        private set

    // Funciones para buscar un equipo o jugador por su id
    fun equipo(id: Int) = equipos.first { it.id == id }
    fun jugador(id: Int) = jugadores.first { it.id == id }

    // ---------- FICHAS MÉDICAS ----------

    // Un horario está disponible si ninguna ficha lo ocupa
    // (las fichas rechazadas liberan el horario)
    fun disponible(horarioId: Int) =
        fichas.none { it.horarioId == horarioId && it.estado != EstadoFicha.RECHAZADA }

    // Un jugador está habilitado si tiene alguna ficha marcada como APTO
    fun habilitado(jugadorId: Int) =
        fichas.any { it.jugadorId == jugadorId && it.estado == EstadoFicha.APTO }

    // El jugador reserva un horario: se agrega una ficha nueva (queda PENDIENTE)
    fun reservar(horarioId: Int): String {
        fichas = fichas + FichaMedica(fichas.size + 1, jugadorActualId, horarioId)
        return "Solicitud enviada. Espera la aceptación del médico."
    }

    // Cambia el estado de una ficha (aceptar, rechazar, apto, no apto).
    // Se recorre la lista y solo la ficha elegida se reemplaza por una copia con el estado nuevo.
    fun cambiarEstado(ficha: FichaMedica, nuevo: EstadoFicha) {
        fichas = fichas.map { if (it.id == ficha.id) it.copy(estado = nuevo) else it }
    }

    // ---------- TORNEO ----------

    // Genera el fixture "todos contra todos": cada equipo juega una vez contra cada otro.
    // Primero se sortea (shuffled) el orden de los equipos.
    fun generar(): String {
        val ids = torneo.equipoIds.shuffled()
        val lista = mutableListOf<Partido>()
        for (i in ids.indices) {
            // j empieza en i + 1 para no repetir partidos ni jugar contra uno mismo
            for (j in i + 1 until ids.size) {
                lista.add(Partido(lista.size + 1, ids[i], ids[j]))
            }
        }
        partidos = lista
        return "Fixture generado"
    }

    // Guarda los goles de un partido (reemplaza el partido por una copia con el resultado)
    fun guardarResultado(partido: Partido, golesLocal: Int, golesVisitante: Int) {
        partidos = partidos.map {
            if (it.id == partido.id) it.copy(golesLocal = golesLocal, golesVisitante = golesVisitante) else it
        }
    }

    // Calcula la tabla de posiciones a partir de los partidos ya jugados
    fun tabla(): List<Posicion> {
        return torneo.equipoIds.map { id ->
            // Partidos terminados en los que participó este equipo
            val jugados = partidos.filter {
                it.finalizado() && (it.localId == id || it.visitanteId == id)
            }
            var favor = 0
            var contra = 0
            var puntos = 0

            for (p in jugados) {
                // Según jugó de local o de visitante, se elige cuáles son sus goles y cuáles los del rival
                val propios = if (p.localId == id) p.golesLocal!! else p.golesVisitante!!
                val rival = if (p.localId == id) p.golesVisitante!! else p.golesLocal!!
                favor += propios
                contra += rival
                // Victoria = 3 puntos, empate = 1, derrota = 0
                puntos += when {
                    propios > rival -> 3
                    propios == rival -> 1
                    else -> 0
                }
            }
            Posicion(id, jugados.size, favor, favor - contra, puntos)
        }.sortedWith(
            // Orden: más puntos primero; si empatan, mayor diferencia de goles; luego goles a favor
            compareByDescending<Posicion> { it.puntos }
                .thenByDescending { it.diferencia }
                .thenByDescending { it.favor }
        )
    }
}
