package com.example.canchabolivia.modelodiagrama

import java.time.LocalDate
import java.time.LocalTime

// Espejo en código del diagrama de clases UML del sistema (clases, campos y
// firmas de método exactamente como en el diagrama). Es documentación: cada
// cuerpo queda en TODO() porque esta app usa un modelo simplificado para la
// UI (ver Modelo.kt y CanchaViewModel.kt en el paquete de arriba).

abstract class Persona {
    @JvmField protected var ci: String = ""
    @JvmField protected var nombreCompleto: String = ""
    protected var email: String = ""
    private var contrasena: String = ""
    private var sesionActiva: Boolean = false

    fun iniciarSesion(email: String, contrasena: String): Boolean { TODO() }
    fun cerrarSesion() { TODO() }
    fun cambiarContrasena(actual: String, nueva: String): Boolean { TODO() }
    fun getCi(): String { TODO() }
    fun getNombreCompleto(): String { TODO() }
}

class Organizador : Persona() {
    fun crearTorneo(nombre: String): Torneo { TODO() }
    fun cancelarTorneo(torneo: Torneo, motivo: String) { TODO() }
    fun inscribirEquipo(torneo: Torneo, equipo: Equipo): InscripcionEquipo { TODO() }
    fun retirarEquipo(torneo: Torneo, equipo: Equipo, motivo: String) { TODO() }
    fun programarPartido(torneo: Torneo, local: Equipo, visitante: Equipo, fecha: LocalDate): Partido { TODO() }
    fun cancelarPartido(partido: Partido, motivo: String) { TODO() }
    fun reprogramarPartido(partido: Partido, fecha: LocalDate) { TODO() }
    fun asignarArbitro(partido: Partido, arbitro: Arbitro) { TODO() }
    fun iniciarTorneo(torneo: Torneo) { TODO() }
    fun levantarSuspension(jugador: Jugador) { TODO() }
}

class Jugador : Persona() {
    private var aptoMedico: Boolean = false
    private var suspendido: Boolean = false
    private var amarillas: Int = 0
    private var rojas: Int = 0

    fun inscribirse(equipo: Equipo, torneo: Torneo): InscripcionJugador { TODO() }
    fun agendarCita(medico: Medico, fecha: LocalDate, hora: LocalTime): Cita { TODO() }
    fun registrarAmarilla() { TODO() }
    fun registrarRoja() { TODO() }
    fun setAptoMedico(apto: Boolean) { TODO() }
    fun setSuspendido(valor: Boolean) { TODO() }
    fun estaHabilitado(): Boolean { TODO() }
}

class Arbitro : Persona() {
    fun dirigir(partido: Partido): ArbitroPartido { TODO() }
    fun agregarTarjeta(partido: Partido, jugador: Jugador, tipo: String, minuto: Int): Tarjeta { TODO() }
    fun registrarResultado(partido: Partido, golesLocal: Int, golesVisitante: Int) { TODO() }
    fun registrarPenales(partido: Partido, penalesLocal: Int, penalesVisitante: Int) { TODO() }
}

class Medico : Persona() {
    private var especialidad: String = ""

    fun getEspecialidad(): String { TODO() }
    fun verCitasDelDia(fecha: LocalDate): List<Cita> { TODO() }
    fun atender(cita: Cita, observaciones: String) { TODO() }
    fun habilitarJugador(jugador: Jugador) { TODO() }
    fun inhabilitarJugador(jugador: Jugador, motivo: String) { TODO() }
    fun reprogramarCita(cita: Cita, fecha: LocalDate, hora: LocalTime) { TODO() }
    fun cancelarCita(cita: Cita, motivo: String) { TODO() }
    fun verHistorial(jugador: Jugador): List<Cita> { TODO() }
}

class Torneo {
    private var idTorneo: Int = 0
    private var nombreTorneo: String = ""
    private var estado: String = ""
    private var formato: String = ""
    private var cantidadGrupos: Int = 0
    private var clasificadosPorGrupo: Int = 0
    private var ciOrganizador: String = ""

    fun inscribirEquipo(equipo: Equipo): InscripcionEquipo { TODO() }
    fun cambiarEstado(estado: String) { TODO() }
    fun generarFixtureLiga(): List<Partido> { TODO() }
    fun sortearGrupos(): List<Grupo> { TODO() }
    fun generarFixtureGrupos(): List<Partido> { TODO() }
    fun generarLlaves(): List<Partido> { TODO() }
    fun avanzarRonda(): List<Partido> { TODO() }
    fun calcularTabla(): List<Posicion> { TODO() }
    fun listarPartidos(): List<Partido> { TODO() }
    fun getCampeon(): Equipo { TODO() }
}

class Grupo {
    private var idGrupo: Int = 0
    private var idTorneo: Int = 0
    private var nombre: String = ""

    fun agregarEquipo(equipo: Equipo) { TODO() }
    fun getTabla(): List<Posicion> { TODO() }
    fun getClasificados(cantidad: Int): List<Equipo> { TODO() }
}

class Posicion {
    private var idTorneo: Int = 0
    private var idGrupo: Int = 0
    private var idEquipo: Int = 0
    private var jugados: Int = 0
    private var ganados: Int = 0
    private var empatados: Int = 0
    private var perdidos: Int = 0
    private var golesFavor: Int = 0
    private var golesContra: Int = 0
    private var puntos: Int = 0

    fun actualizar(golesFavor: Int, golesContra: Int) { TODO() }
    fun diferenciaGoles(): Int { TODO() }
}

class Equipo {
    private var idEquipo: Int = 0
    private var nombre: String = ""

    fun inscribirJugador(jugador: Jugador, torneo: Torneo): InscripcionJugador { TODO() }
    fun listarJugadores(torneo: Torneo): List<Jugador> { TODO() }
}

class InscripcionEquipo {
    private var idTorneo: Int = 0
    private var idEquipo: Int = 0

    fun estaVigente(): Boolean { TODO() }
}

class InscripcionJugador {
    private var ciJugador: String = ""
    private var idEquipo: Int = 0
    private var idTorneo: Int = 0

    fun estaVigente(): Boolean { TODO() }
}

class Partido {
    private var idPartido: Int = 0
    private var idTorneo: Int = 0
    private var idGrupo: Int? = null
    private var fase: String = ""
    private var nroFecha: Int = 0
    private lateinit var fechaPartido: LocalDate
    private var golesLocal: Int = 0
    private var golesVisitante: Int = 0
    private var penalesLocal: Int = 0
    private var penalesVisitante: Int = 0
    private var estado: String = ""
    private var motivoCancelacion: String? = null
    private var idEquipoLocal: Int = 0
    private var idEquipoVisitante: Int = 0

    fun registrarResultado(golesLocal: Int, golesVisitante: Int) { TODO() }
    fun registrarPenales(penalesLocal: Int, penalesVisitante: Int) { TODO() }
    fun esEmpate(): Boolean { TODO() }
    fun getGanador(): Equipo { TODO() }
    fun cancelar(motivo: String) { TODO() }
    fun reprogramar(fecha: LocalDate) { TODO() }
    fun estaCancelado(): Boolean { TODO() }
}

class Tarjeta {
    private var idTarjeta: Int = 0
    private var idPartido: Int = 0
    private var ciJugador: String = ""
    private var ciArbitro: String = ""
    private var tipo: String = ""
    private var minuto: Int = 0

    fun esRoja(): Boolean { TODO() }
}

class ArbitroPartido {
    private var idPartido: Int = 0
    private var ciArbitro: String = ""
}

class Cita {
    private var idCita: Int = 0
    private lateinit var fechaCita: LocalDate
    private lateinit var horaCita: LocalTime
    private var estado: String = ""
    private var observaciones: String = ""
    private var ciMedico: String = ""
    private var ciJugador: String = ""

    fun confirmar() { TODO() }
    fun cancelar(motivo: String) { TODO() }
    fun reprogramar(fecha: LocalDate, hora: LocalTime) { TODO() }
    fun finalizar(observaciones: String) { TODO() }
}
