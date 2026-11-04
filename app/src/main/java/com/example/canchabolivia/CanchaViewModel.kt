package com.example.canchabolivia

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.canchabolivia.api.FutHubNubeApi
import com.example.canchabolivia.repository.NubeRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CanchaViewModel : ViewModel() {

    // --- SESIÓN (Firebase Authentication) ---
    // Firebase guarda el correo y la contraseña. El rol elegido al crear la
    // cuenta se guarda en su displayName, así queda ligado a la cuenta.
    // Las respuestas llegan por alTerminar: null si salió bien, o el error.
    // [GUIA DOCENTE - FIREBASE] Aqui empieza el login de Firebase Authentication.
    // Crear cuenta, iniciarSesion y cerrarSesion estan en este mismo ViewModel.
    // El displayName del cliente NO es una autorizacion segura de roles en produccion.
    private val auth = FirebaseAuth.getInstance()

    // Si ya había una sesión abierta, se entra directo sin pedir la clave.
    var usuarioLogueado by mutableStateOf(auth.currentUser?.aUsuario())
        private set

    // Cada quien solo ve la pantalla de su propio rol.
    val rol: Rol get() = usuarioLogueado?.rol ?: Rol.JUGADOR

    fun iniciarSesion(correo: String, clave: String, alTerminar: (String?) -> Unit) {
        if (correo.isBlank() || clave.isBlank()) return alTerminar("Escribe tu correo y contraseña")

        auth.signInWithEmailAndPassword(correo.trim(), clave.trim())
            .addOnSuccessListener { entrar(it.user, alTerminar) }
            .addOnFailureListener { alTerminar(mensajeDeError(it)) }
    }

    fun crearCuenta(correo: String, clave: String, rol: Rol, alTerminar: (String?) -> Unit) {
        if (correo.isBlank() || clave.isBlank()) return alTerminar("Escribe tu correo y contraseña")
        if (clave.trim().length < 6) return alTerminar("La contraseña debe tener al menos 6 caracteres")

        auth.createUserWithEmailAndPassword(correo.trim(), clave.trim())
            .addOnSuccessListener { resultado ->
                val usuario = resultado.user!!
                val conRol = UserProfileChangeRequest.Builder().setDisplayName(rol.name).build()
                usuario.updateProfile(conRol)
                    .addOnSuccessListener { entrar(usuario, alTerminar) }
                    .addOnFailureListener {
                        usuario.delete() // sin rol la cuenta quedaría a medias
                        alTerminar("No se pudo guardar el rol, intenta de nuevo")
                    }
            }
            .addOnFailureListener { alTerminar(mensajeDeError(it)) }
    }

    fun cerrarSesion() {
        auth.signOut()
        usuarioLogueado = null
    }

    private fun entrar(usuario: FirebaseUser?, alTerminar: (String?) -> Unit) {
        usuarioLogueado = usuario?.aUsuario()
        alTerminar(if (usuarioLogueado == null) "No se pudo entrar, intenta de nuevo" else null)
    }

    // "oscar.lopez@gmail.com" → nombre "Oscar Lopez", con el rol guardado en la cuenta.
    private fun FirebaseUser.aUsuario(): Usuario {
        val correo = email.orEmpty()
        val nombre = correo.substringBefore("@")
            .split('.', '_', '-')
            .filter { it.isNotEmpty() }
            .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        val rol = Rol.entries.find { it.name == displayName } ?: Rol.JUGADOR
        return Usuario(uid.hashCode(), nombre, correo, "", rol)
    }

    private fun mensajeDeError(e: Exception): String = when (e) {
        is FirebaseNetworkException -> "Sin conexión a internet"
        is FirebaseAuthUserCollisionException -> "Ese correo ya tiene una cuenta"
        is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil"
        is FirebaseAuthInvalidUserException -> "Esa cuenta no existe"
        is FirebaseAuthInvalidCredentialsException -> when (e.errorCode) {
            "ERROR_WRONG_PASSWORD" -> "Contraseña incorrecta"
            "ERROR_INVALID_EMAIL" -> "El correo no es válido"
            // Con la protección contra enumeración de correos activada,
            // Firebase no dice si falló el correo o la contraseña.
            else -> "Correo o contraseña incorrectos"
        }
        else -> "Algo salió mal, intenta de nuevo"
    }

    // --- DATOS EN SQLITE CLOUD ---
    // El login es de Firebase; los datos del torneo viven en SQLite Cloud.
    // Las listas de abajo son el punto de partida (y lo que se usa sin internet).
    // Al abrir la app se reemplazan con lo que hay en la nube, y cada cambio
    // se sube con la Edge Function que corresponde (ver NubeRepository).
    // [GUIA DOCENTE - SQLITE CLOUD] Aqui se construye el Repository remoto.
    // La URL y clave salen de sqlitecloud.properties y BuildConfig; sin datos, usa ejemplos.
    private val nube: NubeRepository? =
        if (FutHubNubeApi.configurada) NubeRepository(FutHubNubeApi.instancia) else null

    // Las subidas van de una en una y en orden, para que un cambio viejo
    // nunca pise a uno más nuevo.
    private val colaNube = Mutex()

    var estadoNube by mutableStateOf(
        if (nube == null) "Base de datos: sin configurar (datos de ejemplo)" else "Base de datos: conectando..."
    )
        private set

    private fun enNube(accion: String, subir: suspend NubeRepository.() -> Result<Unit>) {
        val repo = nube ?: return
        viewModelScope.launch {
            colaNube.withLock {
                repo.subir().onFailure { estadoNube = "No se pudo $accion en la nube" }
            }
        }
    }

    // Trae todo de SQLite Cloud. Se asignan los campos internos (_torneo,
    // _partidos) para no volver a subir lo que se acaba de bajar.
    // [GUIA DOCENTE - SINCRONIZACION] Aqui se leen los datos de la nube a la app.
    // MEJORA: este metodo tambien responde al boton Comprobar conexion del login.
    fun recargarDesdeNube() {
        val repo = nube ?: run {
            estadoNube = "Base de datos: sin configurar (datos de ejemplo)"
            return
        }
        // GUIA DOCENTE [MEJORA]: cada pulsacion muestra que se esta comprobando la nube.
        estadoNube = "Base de datos: comprobando conexion..."
        viewModelScope.launch {
            repo.cargarDatos()
                .onSuccess { datos ->
                    equipos = datos.equipos
                    jugadores = datos.jugadores
                    fichas = datos.fichas
                    datos.torneo?.let { _torneo = it }
                    _partidos = datos.partidos
                    tarjetas = datos.tarjetas
                    estadoNube = "Base de datos: conectada"
                }
                .onFailure { estadoNube = "Base de datos: sin conexión (datos de ejemplo)" }
        }
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
        val nuevo = Equipo(nuevoId, nombre, sigla.uppercase())
        val nuevosJugadores = listOf(
            Jugador(nuevoId * 10 + 1, "Jugador 1", nuevoId, 10),
            Jugador(nuevoId * 10 + 2, "Jugador 2", nuevoId, 7)
        )
        equipos = equipos + nuevo
        jugadores = jugadores + nuevosJugadores
        enNube("guardar el equipo") { guardarEquipo(nuevo, nuevosJugadores) }
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
        enNube("eliminar el equipo") { eliminarEquipo(equipoId) }
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
        val nueva = FichaMedica((fichas.maxOfOrNull { it.id } ?: 0) + 1, jugadorActualId, horarioId)
        fichas = fichas + nueva
        enNube("guardar la ficha") { guardarFicha(nueva) }
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
        subirFicha(fichaId)
    }

    fun evaluar(fichaId: Int, apto: Boolean) {
        fichas = fichas.map { ficha ->
            if (ficha.id == fichaId) {
                ficha.copy(estado = if (apto) EstadoFicha.APTO else EstadoFicha.NO_APTO)
            } else ficha
        }
        subirFicha(fichaId)
    }

    private fun subirFicha(fichaId: Int) {
        val ficha = fichas.firstOrNull { it.id == fichaId } ?: return
        enNube("guardar la ficha") { guardarFicha(ficha) }
    }

    // --- TORNEO Y PARTIDOS ---
    // Cada vez que se asigna torneo o partidos (generar, resultados, cancelar,
    // avanzar...), el setter lo sube solo a SQLite Cloud.
    private var _torneo by mutableStateOf(Torneo("Copa La Paz", Formato.LIGA, mutableListOf(1, 2, 4, 5)))
    var torneo: Torneo
        get() = _torneo
        set(valor) {
            _torneo = valor
            enNube("guardar el torneo") { guardarTorneo(valor) }
        }

    private var _partidos by mutableStateOf(listOf<Partido>())
    var partidos: List<Partido>
        get() = _partidos
        set(valor) {
            _partidos = valor
            enNube("guardar los partidos") { guardarPartidos(valor) }
        }

    fun generar(sortear: Boolean): String {
        val ids = if (sortear) torneo.equipoIds.shuffled() else torneo.equipoIds
        if (ids.size < 2) return "Elige al menos 2 equipos en Configurar"
        if (torneo.formato == Formato.GRUPOS && ids.size < 4) return "La fase de grupos necesita al menos 4 equipos"

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
            // Tipo Mundial: primero solo la fase de grupos; la eliminación
            // se arma con avanzar() cuando terminan los grupos.
            Formato.GRUPOS -> MotorTorneo.armarGrupos(ids, idInicial = 1)
        }

        return "Fixture generado"
    }

    // Borra el fixture (y sus tarjetas) para poder configurar y jugar otro
    // torneo. Los equipos y jugadores se mantienen.
    fun nuevoTorneo(): String {
        tarjetas = emptyList()
        partidos = emptyList()
        return "Listo: configura el nuevo torneo"
    }

    // Partidos de eliminación directa (en GRUPOS, los que no son de un grupo).
    private val eliminatoria: List<Partido> get() = partidos.filter { it.grupo == null }

    // Un empate necesita penales solo en partidos de eliminación.
    fun necesitaPenales(partido: Partido): Boolean =
        partido.visitanteId != null && partido.grupo == null && torneo.formato != Formato.LIGA

    // Nunca se muta un Partido existente: siempre se reemplaza por una
    // copia dentro de la lista (igual que con las fichas médicas).
    fun registrarResultado(partidoId: Int, golesLocal: Int, golesVisitante: Int, ganadorPenalesId: Int? = null) {
        partidos = partidos.map { partido ->
            if (partido.id == partidoId) {
                partido.copy(golesLocal = golesLocal, golesVisitante = golesVisitante, ganadorPenalesId = ganadorPenalesId)
            } else partido
        }
    }

    // Cancelación por clima no apto (organizador). Solo se cancelan partidos
    // reales todavía sin resultado; se pueden reprogramar después.
    fun cancelarPartido(partidoId: Int) {
        partidos = partidos.map {
            if (it.id == partidoId && it.visitanteId != null && !it.finalizado()) it.copy(cancelado = true) else it
        }
    }

    fun reprogramarPartido(partidoId: Int) {
        partidos = partidos.map { if (it.id == partidoId) it.copy(cancelado = false) else it }
    }

    fun cancelarPendientes(): String {
        val n = partidos.count { it.visitanteId != null && !it.finalizado() && !it.cancelado }
        if (n == 0) return "No hay partidos pendientes para cancelar"
        partidos = partidos.map {
            if (it.visitanteId != null && !it.finalizado()) it.copy(cancelado = true) else it
        }
        return if (n == 1) "1 partido cancelado por clima" else "$n partidos cancelados por clima"
    }

    // Toma los resultados de la última ronda de eliminación directa, saca
    // a los ganadores y arma la ronda siguiente. Si algún partido de la
    // última ronda todavía no tiene resultado (o quedó empatado sin
    // penales), avisa en vez de avanzar con datos incompletos.
    fun avanzar(): String {
        if (torneo.formato == Formato.LIGA) return "Esta acción es solo para torneos con eliminación"
        if (partidos.isEmpty()) return "Primero genera el fixture"

        // Tipo Mundial: si todavía no hay eliminatoria, se cierran los grupos
        // y pasan los 2 primeros de cada uno.
        if (torneo.formato == Formato.GRUPOS && eliminatoria.isEmpty()) {
            if (partidos.any { it.cancelado }) return "Hay partidos cancelados: reprográmalos para avanzar"
            if (partidos.any { !it.finalizado() }) return "Faltan resultados de la fase de grupos"
            val clasificados = MotorTorneo.clasificados(partidos)
            val idInicial = partidos.maxOf { it.id } + 1
            partidos = partidos + MotorTorneo.siguienteRondaEliminacion(clasificados, ronda = 2, idInicial = idInicial)
            return "Fase de grupos cerrada: empieza la eliminación directa"
        }

        val ultimaRonda = eliminatoria.maxOf { it.ronda }
        val partidosUltimaRonda = eliminatoria.filter { it.ronda == ultimaRonda }

        if (partidosUltimaRonda.any { it.cancelado }) return "Hay partidos cancelados: reprográmalos para avanzar"

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
            Formato.ELIMINACION, Formato.GRUPOS -> {
                if (eliminatoria.isEmpty()) return null
                val ultimaRonda = eliminatoria.maxOf { it.ronda }
                val partidosUltimaRonda = eliminatoria.filter { it.ronda == ultimaRonda }
                if (partidosUltimaRonda.size != 1) return null
                partidosUltimaRonda.single().ganadorId()?.let { equipo(it).nombre }
            }
        }
    }

    // --- TARJETAS (árbitro) ---
    var tarjetas by mutableStateOf(listOf<Tarjeta>())

    fun agregarTarjeta(partidoId: Int, jugadorId: Int, tipo: TipoTarjeta, minuto: Int) {
        val nuevaId = (tarjetas.maxOfOrNull { it.id } ?: 0) + 1
        val nueva = Tarjeta(nuevaId, partidoId, jugadorId, tipo, minuto)
        tarjetas = tarjetas + nueva
        enNube("guardar la tarjeta") { guardarTarjeta(nueva) }
    }

    fun tarjetasDe(partidoId: Int): List<Tarjeta> = tarjetas.filter { it.partidoId == partidoId }

    // Va al final a propósito: así todas las listas de arriba ya existen
    // cuando llegan los datos de la nube.
    init {
        recargarDesdeNube()
    }
}
