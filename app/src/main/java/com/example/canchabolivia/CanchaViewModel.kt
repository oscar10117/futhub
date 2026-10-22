package com.example.canchabolivia

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.canchabolivia.database.FutHubDatabase
import com.example.canchabolivia.repository.EquiposRepository
import com.example.canchabolivia.repository.FichasRepository
import com.example.canchabolivia.repository.TorneoRepository
import kotlinx.coroutines.launch

// Equipos, jugadores, torneo, partidos, tarjetas y fichas viven en Room
// (ver init al final). Las listas de aquí son solo lo que pinta la pantalla:
// cada cambio se escribe en la base y Room devuelve la lista actualizada.
class CanchaViewModel(app: Application) : AndroidViewModel(app) {

    // --- BASE DE DATOS ---
    private val db = FutHubDatabase.obtener(app)
    private val equiposRepo = EquiposRepository(db.equipoDao())
    private val torneoRepo = TorneoRepository(db.torneoDao(), db.partidoDao())
    private val fichasRepo = FichasRepository(db.fichaDao())

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
    var equipos by mutableStateOf(listOf<Equipo>())
        private set

    var jugadores by mutableStateOf(listOf<Jugador>())
        private set

    val jugadorActualId = 11

    fun agregarEquipo(nombre: String, sigla: String) {
        viewModelScope.launch { equiposRepo.agregar(nombre, sigla.uppercase()) }
    }

    // Borrar un equipo también limpia todo lo que dependía de él: jugadores,
    // su lugar en el torneo, sus partidos, fichas y tarjetas. Ya no se hace a
    // mano: lo hacen las claves foráneas con ON DELETE CASCADE de la base.
    fun eliminarEquipo(equipoId: Int) {
        viewModelScope.launch { equiposRepo.eliminar(equipoId) }
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
        private set

    fun disponible(horarioId: Int): Boolean {
        return fichas.none { it.horarioId == horarioId && it.estado != EstadoFicha.RECHAZADA }
    }

    fun habilitado(jugadorId: Int): Boolean {
        return fichas.any { it.jugadorId == jugadorId && it.estado == EstadoFicha.APTO }
    }

    fun reservar(horarioId: Int): String {
        // La ficha apunta al jugador con clave foránea: si su equipo se borró,
        // la base rechazaría la ficha, así que se avisa antes.
        if (jugadores.none { it.id == jugadorActualId }) return "Tu equipo fue eliminado: no puedes reservar"
        viewModelScope.launch { fichasRepo.agregar(FichaMedica(0, jugadorActualId, horarioId)) }
        return "Solicitud enviada correctamente"
    }

    // Se guarda una copia con el nuevo estado (nunca se muta la ficha):
    // Room emite la lista nueva y la pantalla del médico se actualiza.
    fun responder(fichaId: Int, aceptar: Boolean) {
        cambiarEstado(fichaId, if (aceptar) EstadoFicha.ACEPTADA else EstadoFicha.RECHAZADA)
    }

    fun evaluar(fichaId: Int, apto: Boolean) {
        cambiarEstado(fichaId, if (apto) EstadoFicha.APTO else EstadoFicha.NO_APTO)
    }

    private fun cambiarEstado(fichaId: Int, estado: EstadoFicha) {
        val ficha = fichas.firstOrNull { it.id == fichaId } ?: return
        viewModelScope.launch { fichasRepo.actualizar(ficha.copy(estado = estado)) }
    }

    // --- TORNEO Y PARTIDOS ---
    var torneo by mutableStateOf(Torneo("", Formato.LIGA, mutableListOf()))
        private set

    var partidos by mutableStateOf(listOf<Partido>())
        private set

    fun guardarTorneo(nombre: String, formato: Formato, equipoIds: List<Int>) {
        viewModelScope.launch { torneoRepo.guardarTorneo(nombre, formato, equipoIds) }
    }

    fun generar(sortear: Boolean): String {
        val ids = if (sortear) torneo.equipoIds.shuffled() else torneo.equipoIds

        val fixture = when (torneo.formato) {
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
        viewModelScope.launch { torneoRepo.reemplazarFixture(fixture) }

        return "Fixture generado"
    }

    // Nunca se muta un Partido existente: se guarda una copia con el cambio.
    fun registrarResultado(partidoId: Int, golesLocal: Int, golesVisitante: Int, ganadorPenalesId: Int? = null) {
        actualizarPartidos(
            partidos.filter { it.id == partidoId }.map {
                it.copy(golesLocal = golesLocal, golesVisitante = golesVisitante, ganadorPenalesId = ganadorPenalesId)
            }
        )
    }

    // Cancelación por clima no apto (organizador). Solo se cancelan partidos
    // reales todavía sin resultado; se pueden reprogramar después.
    fun cancelarPartido(partidoId: Int) {
        actualizarPartidos(
            partidos.filter { it.id == partidoId && it.visitanteId != null && !it.finalizado() }
                .map { it.copy(cancelado = true) }
        )
    }

    fun reprogramarPartido(partidoId: Int) {
        actualizarPartidos(partidos.filter { it.id == partidoId }.map { it.copy(cancelado = false) })
    }

    fun cancelarPendientes(): String {
        val pendientes = partidos.filter { it.visitanteId != null && !it.finalizado() && !it.cancelado }
        if (pendientes.isEmpty()) return "No hay partidos pendientes para cancelar"
        actualizarPartidos(pendientes.map { it.copy(cancelado = true) })
        val n = pendientes.size
        return if (n == 1) "1 partido cancelado por clima" else "$n partidos cancelados por clima"
    }

    private fun actualizarPartidos(cambiados: List<Partido>) {
        if (cambiados.isEmpty()) return
        viewModelScope.launch { torneoRepo.actualizarPartidos(cambiados) }
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

        if (partidosUltimaRonda.any { it.cancelado }) return "Hay partidos cancelados: reprográmalos para avanzar"

        val haySinJugar = partidosUltimaRonda.any { it.visitanteId != null && !it.finalizado() }
        if (haySinJugar) return "Faltan resultados de la ronda $ultimaRonda"

        val ganadores = partidosUltimaRonda.map {
            it.ganadorId() ?: return "Hay un empate sin definir por penales en la ronda $ultimaRonda"
        }
        if (ganadores.size == 1) return "El torneo ya tiene campeón"

        val idInicial = partidos.maxOf { it.id } + 1
        val ronda = MotorTorneo.siguienteRondaEliminacion(ganadores, ultimaRonda + 1, idInicial)
        viewModelScope.launch { torneoRepo.agregarPartidos(ronda) }
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
        private set

    // El id de la tarjeta nueva lo pone Room (llega en 0).
    fun agregarTarjeta(partidoId: Int, jugadorId: Int, tipo: TipoTarjeta, minuto: Int) {
        viewModelScope.launch { torneoRepo.agregarTarjeta(Tarjeta(0, partidoId, jugadorId, tipo, minuto)) }
    }

    fun tarjetasDe(partidoId: Int): List<Tarjeta> = tarjetas.filter { it.partidoId == partidoId }

    // --- CARGA DESDE ROOM ---
    // Va al final para que todas las propiedades de arriba ya existan.
    // Cada Flow vuelve a emitir cuando su tabla cambia, así la pantalla
    // siempre muestra lo que está guardado.
    init {
        viewModelScope.launch { equiposRepo.equipos.collect { equipos = it } }
        viewModelScope.launch { equiposRepo.jugadores.collect { jugadores = it } }
        viewModelScope.launch { torneoRepo.torneo.collect { torneo = it } }
        viewModelScope.launch { torneoRepo.partidos.collect { partidos = it } }
        viewModelScope.launch { torneoRepo.tarjetas.collect { tarjetas = it } }
        viewModelScope.launch { fichasRepo.fichas.collect { fichas = it } }
    }
}
