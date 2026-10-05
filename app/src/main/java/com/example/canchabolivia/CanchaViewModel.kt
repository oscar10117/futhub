package com.example.canchabolivia

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CanchaViewModel : ViewModel() {
    val usuarios = listOf(Usuario(1,"Juan Pérez",Rol.JUGADOR), Usuario(2,"Dra. Ana Rojas",Rol.MEDICO),
        Usuario(3,"Luis Flores",Rol.ARBITRO),Usuario(4,"Organización",Rol.ORGANIZADOR))
    val equipos = listOf(Equipo(1,"Bolívar","BOL"),Equipo(2,"The Strongest","STR"),Equipo(3,"Always Ready","ALW"),
        Equipo(4,"Wilstermann","WIL"),Equipo(5,"Oriente Petrolero","ORI"),Equipo(6,"Blooming","BLO"),
        Equipo(7,"Nacional Potosí","NAC"),Equipo(8,"Aurora","AUR"),Equipo(9,"Real Tomayapo","TOM"),Equipo(10,"Universitario","UNI"))
    val jugadores = equipos.flatMap { equipo ->
        listOf(Jugador(equipo.id*10+1,if(equipo.id==1) "Juan Pérez" else "Carlos ${equipo.sigla}",7,equipo.id),
            Jugador(equipo.id*10+2,"Diego ${equipo.sigla}",10,equipo.id),Jugador(equipo.id*10+3,"Mateo ${equipo.sigla}",1,equipo.id))
    }
    val jugadorActualId = 11
    val horarios = (1..3).flatMap { dia -> listOf("09:00","09:30","10:00","10:30").mapIndexed { i,hora ->
        HorarioMedico(dia*10+i,2,LocalDate.now().plusDays(dia.toLong()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),hora)
    } }
    var fichas by mutableStateOf(listOf<FichaMedica>()); private set
    var habilitaciones by mutableStateOf(jugadores.filter { it.id != jugadorActualId && it.dorsal != 1 }.map {
        Habilitacion(it.id,it.id,2,1,true)
    }); private set
    var torneo by mutableStateOf(Torneo(1,"Copa La Paz",4,Formato.LIGA,equipos.take(4).map { it.id })); private set
    var partidos by mutableStateOf(listOf<Partido>()); private set
    var rol by mutableStateOf(Rol.JUGADOR)
    fun equipo(id: Int) = equipos.first { it.id == id }
    fun jugador(id: Int) = jugadores.first { it.id == id }
    fun habilitado(id: Int) = habilitaciones.any { it.jugadorId == id && it.torneoId == torneo.id && it.apto }
    fun disponible(id: Int) = fichas.none { it.horarioId == id && it.estado != EstadoFicha.RECHAZADA }
    fun reservar(horario: Int): String {
        if (rol != Rol.JUGADOR) return "Selecciona el perfil Jugador."
        if (horarios.none { it.id == horario }) return "Selecciona un horario válido."
        if (habilitado(jugadorActualId)) return "Ya estás habilitado para este torneo."
        if (!disponible(horario)) return "Este horario ya está reservado."
        if (fichas.any { it.jugadorId == jugadorActualId && it.torneoId == torneo.id && it.estado in listOf(EstadoFicha.PENDIENTE,EstadoFicha.ACEPTADA) }) return "Ya tienes una ficha en proceso."
        fichas = fichas + FichaMedica((fichas.maxOfOrNull { it.id } ?: 0)+1,jugadorActualId,horario,torneo.id)
        return "Solicitud enviada. Espera la aceptación del médico."
    }
    fun responder(ficha: FichaMedica, aceptar: Boolean) {
        if (rol != Rol.MEDICO || ficha.estado != EstadoFicha.PENDIENTE) return
        fichas = fichas.map { if(it.id==ficha.id) it.copy(estado=if(aceptar) EstadoFicha.ACEPTADA else EstadoFicha.RECHAZADA) else it }
    }
    fun evaluar(ficha: FichaMedica, apto: Boolean) {
        if (rol != Rol.MEDICO || ficha.estado != EstadoFicha.ACEPTADA) return
        habilitaciones = habilitaciones.filterNot { it.jugadorId==ficha.jugadorId && it.torneoId==ficha.torneoId } +
            Habilitacion((habilitaciones.maxOfOrNull { it.id } ?: 0)+1,ficha.jugadorId,2,ficha.torneoId,apto)
        fichas = fichas.map { if(it.id==ficha.id) it.copy(estado=EstadoFicha.ATENDIDA) else it }
    }
    fun configurar(nombre: String, formato: Formato, ids: List<Int>): String {
        if (rol != Rol.ORGANIZADOR) return "Solo el organizador puede modificar el torneo."
        if (partidos.isNotEmpty()) return "La configuración se bloquea al generar el fixture."
        if (nombre.isBlank() || ids.size < 2) return "Escribe un nombre y selecciona al menos dos equipos."
        torneo = torneo.copy(nombre=nombre.trim(),formato=formato,equipoIds=ids.distinct())
        return "Configuración guardada."
    }
    fun generar(azar: Boolean): String {
        if (rol != Rol.ORGANIZADOR || partidos.isNotEmpty()) return "El fixture ya está generado o no tienes este perfil."
        val orden = if(azar) torneo.equipoIds.shuffled() else torneo.equipoIds
        partidos = if(torneo.formato==Formato.LIGA) MotorTorneo.liga(torneo,orden,3) else MotorTorneo.eliminacion(torneo,orden,3)
        return "Fixture generado. Cambia al perfil Árbitro para registrar resultados."
    }
    fun resultado(id: Int, local: Int, visita: Int, penales: Int?): String {
        if(rol != Rol.ARBITRO) return "Selecciona el perfil Árbitro."
        val partido = partidos.firstOrNull { it.id==id } ?: return "Partido inexistente."
        if(partido.finalizado()) return "Este resultado ya fue confirmado."
        if(local !in 0..99 || visita !in 0..99) return "Los goles deben estar entre 0 y 99."
        if(torneo.formato==Formato.ELIMINACION && local==visita && penales !in listOf(partido.localId,partido.visitanteId)) return "Selecciona quién ganó por penales."
        partidos = partidos.map { if(it.id==id) it.copy(golesLocal=local,golesVisitante=visita,ganadorPenalesId=if(local==visita) penales else null) else it }
        return "Resultado confirmado."
    }
    fun avanzar(): String {
        if(rol != Rol.ORGANIZADOR || torneo.formato!=Formato.ELIMINACION || partidos.isEmpty()) return "No hay una ronda para avanzar."
        val ronda = partidos.filter { it.ronda==partidos.maxOf { p -> p.ronda } }
        if(ronda.size==1) return "La final ya está definida."
        if(ronda.any { !it.finalizado() || it.ganador()==null }) return "Falta confirmar resultados de esta ronda."
        partidos = partidos + MotorTorneo.siguienteRonda(ronda,partidos.maxOf { it.id }+1)
        return "Siguiente ronda creada."
    }
    fun campeon(): String? {
        if(partidos.isEmpty() || partidos.any { !it.finalizado() }) return null
        if(torneo.formato==Formato.ELIMINACION) {
            val ultima = partidos.filter { it.ronda==partidos.maxOf { p -> p.ronda } }
            return if(ultima.size==1) ultima[0].ganador()?.let { equipo(it).nombre } else null
        }
        val tabla = MotorTorneo.tabla(torneo,partidos)
        val a=tabla[0]; val b=tabla[1]
        return if(a.puntos==b.puntos && a.diferencia==b.diferencia && a.golesFavor==b.golesFavor) "Empate: se necesita desempate adicional" else equipo(a.equipoId).nombre
    }
}
