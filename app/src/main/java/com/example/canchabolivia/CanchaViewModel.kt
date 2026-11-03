package com.example.canchabolivia

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.canchabolivia.api.FutHubNubeApi
import com.example.canchabolivia.database.FutHubDatabase
import com.example.canchabolivia.database.TorneoEntity
import com.example.canchabolivia.mapper.aEntity
import com.example.canchabolivia.repository.EquiposRepository
import com.example.canchabolivia.repository.FichasRepository
import com.example.canchabolivia.repository.NubeRepository
import com.example.canchabolivia.repository.TorneoRepository
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

// Equipos, jugadores, torneo, partidos, tarjetas y fichas viven en Room
// (ver init al final). Las listas de aquí son solo lo que pinta la pantalla:
// cada cambio se escribe en la base y Room devuelve la lista actualizada.
// Después el mismo cambio se sube a SQLite Cloud (ver enNube).
class CanchaViewModel(app: Application) : AndroidViewModel(app) {

    // --- BASE DE DATOS ---
    private val db = FutHubDatabase.obtener(app)
    private val equiposRepo = EquiposRepository(db.equipoDao())
    private val torneoRepo = TorneoRepository(db.torneoDao(), db.partidoDao())
    private val fichasRepo = FichasRepository(db.fichaDao())

    // --- SESIÓN (Firebase Authentication) ---
    // Firebase guarda el correo y la contraseña. El rol elegido al crear la
    // cuenta se guarda en su displayName, así queda ligado a la cuenta.
    // Las respuestas llegan por alTerminar: null si salió bien, o el error.
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
            "ERROR_INVALID_EMAIL" -> "El correo no es válido"
            // ERROR_WRONG_PASSWORD, o ERROR_INVALID_CREDENTIAL cuando está activa la
            // protección contra enumeración de correos (Firebase no dice cuál falló).
            else -> CLAVE_INCORRECTA
        }
        else -> "Algo salió mal, intenta de nuevo"
    }

    companion object {
        // La pantalla de login la compara para marcar en rojo el campo de la contraseña.
        const val CLAVE_INCORRECTA = "Contraseña incorrecta"
    }

    // --- DATOS EN SQLITE CLOUD ---
    // El login es de Firebase; los datos del torneo se comparten en SQLite Cloud.
    // Room es la copia del teléfono (y lo que se usa sin internet). Al abrir la
    // app se copia a Room lo que hay en la nube, y cada cambio que se guarda en
    // Room se sube con la Edge Function que corresponde (ver NubeRepository).
    private val nube: NubeRepository? =
        if (FutHubNubeApi.configurada) NubeRepository(FutHubNubeApi.instancia) else null

    // Las subidas (y la descarga) van de una en una y en orden, para que un
    // cambio viejo nunca pise a uno más nuevo.
    private val colaNube = Mutex()

    var estadoNube by mutableStateOf(
        if (nube == null) "Base de datos: sin configurar (datos del teléfono)" else "Base de datos: conectando..."
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

    // Trae todo de SQLite Cloud y lo copia en Room; los Flow del init se
    // encargan de pintarlo. Si no hay internet se sigue con lo del teléfono.
    fun recargarDesdeNube() {
        val repo = nube ?: return
        viewModelScope.launch {
            colaNube.withLock {
                repo.cargarDatos()
                    .onSuccess { datos ->
                        db.nubeDao().importar(
                            equipos = datos.equipos.map { it.aEntity() },
                            jugadores = datos.jugadores.map { it.aEntity() },
                            fichas = datos.fichas.map { it.aEntity() },
                            torneo = datos.torneo?.let { TorneoEntity(nombre = it.nombre, formato = it.formato) },
                            equipoIds = datos.torneo?.equipoIds.orEmpty(),
                            partidos = datos.partidos.map { it.aEntity() },
                            tarjetas = datos.tarjetas.map { it.aEntity() }
                        )
                        estadoNube = "Base de datos: conectada"
                    }
                    .onFailure { estadoNube = "Base de datos: sin conexión (datos del teléfono)" }
            }
        }
    }

    // --- EQUIPOS (el organizador puede agregar/eliminar) ---
    var equipos by mutableStateOf(listOf<Equipo>())
        private set

    var jugadores by mutableStateOf(listOf<Jugador>())
        private set

    val jugadorActualId = 11

    fun agregarEquipo(nombre: String, sigla: String) {
        viewModelScope.launch {
            val (nuevo, plantel) = equiposRepo.agregar(nombre, sigla.uppercase())
            enNube("guardar el equipo") { guardarEquipo(nuevo, plantel) }
        }
    }

    // Borrar un equipo también limpia todo lo que dependía de él: jugadores,
    // su lugar en el torneo, sus partidos, fichas y tarjetas. Ya no se hace a
    // mano: lo hacen las claves foráneas con ON DELETE CASCADE de la base.
    fun eliminarEquipo(equipoId: Int) {
        viewModelScope.launch {
            equiposRepo.eliminar(equipoId)
            enNube("eliminar el equipo") { eliminarEquipo(equipoId) }
        }
    }

    // Devuelve el error a mostrar, o null si se guardó (como iniciarSesion).
    fun editarEquipo(equipoId: Int, nombre: String, sigla: String): String? {
        if (nombre.isBlank() || sigla.isBlank()) return "Completa nombre y sigla"
        val equipo = Equipo(equipoId, nombre.trim(), sigla.trim().uppercase())
        viewModelScope.launch {
            equiposRepo.editar(equipo)
            enNube("guardar el equipo") { guardarEquipo(equipo, emptyList()) }
        }
        return null
    }

    // --- PLANTEL (el organizador agrega, edita y quita jugadores) ---
    // jugadorId null = jugador nuevo. Devuelve el error a mostrar, o null si se guardó.
    fun guardarJugador(equipoId: Int, jugadorId: Int?, nombre: String, dorsalTexto: String): String? {
        val dorsal = dorsalTexto.trim().toIntOrNull()
        if (nombre.isBlank()) return "Escribe el nombre del jugador"
        if (dorsal == null || dorsal !in 1..99) return "El dorsal debe ser un número del 1 al 99"
        if (jugadores.any { it.equipoId == equipoId && it.dorsal == dorsal && it.id != jugadorId }) {
            return "El dorsal $dorsal ya lo usa otro jugador del equipo"
        }

        val jugador = Jugador(jugadorId ?: 0, nombre.trim(), equipoId, dorsal)
        viewModelScope.launch {
            val guardado = if (jugadorId == null) equiposRepo.agregarJugador(jugador)
            else jugador.also { equiposRepo.editarJugador(it) }
            enNube("guardar el jugador") { guardarEquipo(equipo(equipoId), listOf(guardado)) }
        }
        return null
    }

    // Sus fichas médicas y tarjetas se borran con él (CASCADE en la base).
    fun quitarJugador(jugadorId: Int) {
        viewModelScope.launch {
            equiposRepo.quitarJugador(jugadorId)
            enNube("quitar el jugador") { eliminarJugador(jugadorId) }
        }
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
        viewModelScope.launch {
            val nueva = fichasRepo.agregar(FichaMedica(0, jugadorActualId, horarioId))
            enNube("guardar la ficha") { guardarFicha(nueva) }
        }
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
        val cambiada = ficha.copy(estado = estado)
        viewModelScope.launch {
            fichasRepo.actualizar(cambiada)
            enNube("guardar la ficha") { guardarFicha(cambiada) }
        }
    }

    // --- TORNEO Y PARTIDOS ---
    // Terminar el torneo solo se guarda en el teléfono: la nube no tiene
    // esas columnas (y al recargar desde la nube se conserva).
    var torneo by mutableStateOf(Torneo("", Formato.LIGA, mutableListOf()))
        private set

    var partidos by mutableStateOf(listOf<Partido>())
        private set

    // Un torneo terminado queda congelado: las funciones que cambian el
    // fixture, resultados o tarjetas no hacen nada (la pantalla además
    // esconde esos botones).
    fun guardarTorneo(nombre: String, formato: Formato, equipoIds: List<Int>) {
        if (torneo.finalizado) return
        viewModelScope.launch {
            torneoRepo.guardarTorneo(nombre, formato, equipoIds)
            enNube("guardar el torneo") { guardarTorneo(Torneo(nombre, formato, equipoIds.toMutableList())) }
        }
    }

    // Cierra el torneo y deja guardado al campeón. Si todavía faltaban
    // partidos: en liga gana el que va primero en la tabla; en eliminación
    // directa sin final definida, termina sin campeón.
    fun terminarTorneo(): String {
        if (torneo.finalizado) return "El torneo ya está terminado"
        if (partidos.isEmpty()) return "Primero genera el fixture"

        val campeon = campeonAlTerminar()
        viewModelScope.launch { torneoRepo.terminar(campeon) }
        return if (campeon != null) "Torneo terminado · Campeón: $campeon" else "Torneo terminado sin campeón"
    }

    // Campeón que quedaría si se termina ahora (para el aviso de confirmación).
    fun campeonAlTerminar(): String? = campeon() ?: campeonPorTabla()

    private fun campeonPorTabla(): String? {
        if (torneo.formato != Formato.LIGA || partidos.none { it.finalizado() }) return null
        return MotorTorneo.tabla(torneo, partidos).firstOrNull()?.let { equipo(it.equipoId).nombre }
    }

    // Borra el fixture del torneo terminado y lo reabre para configurarlo de nuevo.
    fun empezarNuevoTorneo(): String {
        if (!torneo.finalizado) return "Primero termina el torneo actual"
        viewModelScope.launch {
            torneoRepo.empezarNuevo()
            enNube("borrar el fixture") { guardarPartidos(emptyList()) }
        }
        return "Nuevo torneo listo para configurar"
    }

    fun generar(sortear: Boolean): String {
        if (torneo.finalizado) return "El torneo ya terminó"
        val ids = if (sortear) torneo.equipoIds.shuffled() else torneo.equipoIds
        if (ids.size < 2) return "Elige al menos 2 equipos en Configurar"
        if (torneo.formato == Formato.GRUPOS && ids.size < 4) return "La fase de grupos necesita al menos 4 equipos"

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
            // Tipo Mundial: primero solo la fase de grupos; la eliminación
            // se arma con avanzar() cuando terminan los grupos.
            Formato.GRUPOS -> MotorTorneo.armarGrupos(ids, idInicial = 1)
        }
        viewModelScope.launch {
            torneoRepo.reemplazarFixture(fixture)
            subirFixture(fixture)
        }

        return "Fixture generado"
    }

    // Partidos de eliminación directa (en GRUPOS, los que no son de un grupo).
    private val eliminatoria: List<Partido> get() = partidos.filter { it.grupo == null }

    // Un empate necesita penales solo en partidos de eliminación.
    fun necesitaPenales(partido: Partido): Boolean =
        partido.visitanteId != null && partido.grupo == null && torneo.formato != Formato.LIGA

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
        if (torneo.finalizado) return "El torneo ya terminó"
        val pendientes = partidos.filter { it.visitanteId != null && !it.finalizado() && !it.cancelado }
        if (pendientes.isEmpty()) return "No hay partidos pendientes para cancelar"
        actualizarPartidos(pendientes.map { it.copy(cancelado = true) })
        val n = pendientes.size
        return if (n == 1) "1 partido cancelado por clima" else "$n partidos cancelados por clima"
    }

    private fun actualizarPartidos(cambiados: List<Partido>) {
        if (cambiados.isEmpty() || torneo.finalizado) return
        val porId = cambiados.associateBy { it.id }
        val fixture = partidos.map { porId[it.id] ?: it }
        viewModelScope.launch {
            torneoRepo.actualizarPartidos(cambiados)
            subirFixture(fixture)
        }
    }

    private fun agregarRonda(ronda: List<Partido>) {
        val fixture = partidos + ronda
        viewModelScope.launch {
            torneoRepo.agregarPartidos(ronda)
            subirFixture(fixture)
        }
    }

    // guardar_partidos de la nube reemplaza el fixture entero, así que
    // siempre se manda la lista completa (no solo lo que cambió).
    private fun subirFixture(fixture: List<Partido>) =
        enNube("guardar los partidos") { guardarPartidos(fixture) }

    // Toma los resultados de la última ronda de eliminación directa, saca
    // a los ganadores y arma la ronda siguiente. Si algún partido de la
    // última ronda todavía no tiene resultado (o quedó empatado sin
    // penales), avisa en vez de avanzar con datos incompletos.
    fun avanzar(): String {
        if (torneo.finalizado) return "El torneo ya terminó"
        if (torneo.formato == Formato.LIGA) return "Esta acción es solo para torneos con eliminación"
        if (partidos.isEmpty()) return "Primero genera el fixture"

        // Tipo Mundial: si todavía no hay eliminatoria, se cierran los grupos
        // y pasan los 2 primeros de cada uno.
        if (torneo.formato == Formato.GRUPOS && eliminatoria.isEmpty()) {
            if (partidos.any { it.cancelado }) return "Hay partidos cancelados: reprográmalos para avanzar"
            if (partidos.any { !it.finalizado() }) return "Faltan resultados de la fase de grupos"
            val clasificados = MotorTorneo.clasificados(partidos)
            val idInicial = partidos.maxOf { it.id } + 1
            agregarRonda(MotorTorneo.siguienteRondaEliminacion(clasificados, ronda = 2, idInicial = idInicial))
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
        val ronda = MotorTorneo.siguienteRondaEliminacion(ganadores, ultimaRonda + 1, idInicial)
        agregarRonda(ronda)
        return "Ronda ${ultimaRonda + 1} generada"
    }

    // Liga: hay campeón cuando se jugaron todos los partidos (el primero
    // de la tabla). Eliminación: hay campeón cuando la última ronda quedó
    // en un solo partido ya definido. Si el torneo se terminó, manda el
    // campeón que quedó guardado.
    fun campeon(): String? {
        if (torneo.finalizado) return torneo.campeon
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
        private set

    // El id de la tarjeta nueva lo pone Room (llega en 0).
    fun agregarTarjeta(partidoId: Int, jugadorId: Int, tipo: TipoTarjeta, minuto: Int) {
        if (torneo.finalizado) return
        viewModelScope.launch {
            val nueva = torneoRepo.agregarTarjeta(Tarjeta(0, partidoId, jugadorId, tipo, minuto))
            enNube("guardar la tarjeta") { guardarTarjeta(nueva) }
        }
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
        recargarDesdeNube()
    }
}
