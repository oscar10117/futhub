package com.example.canchabolivia

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Fichas(vm: CanchaViewModel, avisar: (String) -> Unit) {
    when (vm.rol) {
        Rol.JUGADOR -> ReservaDeFicha(vm, avisar)
        Rol.MEDICO -> {}
        else -> {
            Panel { Titulo("Agenda médica"); Text("Cambia al perfil Jugador para reservar o al perfil Médico para atender solicitudes.") }
            return
        }
    }
    ListaDeFichas(vm)
}

@Composable
private fun ReservaDeFicha(vm: CanchaViewModel, avisar: (String) -> Unit) {
    val habilitado = vm.habilitado(vm.jugadorActualId)
    Titulo("Reserva una ficha", "Jugador: ${vm.jugador(vm.jugadorActualId).nombre} · ${vm.torneo.nombre}")
    Estado(if (habilitado) "Habilitado para jugar" else "Aún no habilitado", habilitado)

    var horarioId by rememberSaveable { mutableIntStateOf(-1) }
    Panel {
        Text("Dra. Ana Rojas", fontWeight = FontWeight.Bold)
        Text("Revisión presencial · Centro médico de la asociación", fontSize = 13.sp)
        vm.horarios.groupBy { it.fecha }.forEach { (fecha, horarios) ->
            Text(fecha, fontWeight = FontWeight.SemiBold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                horarios.forEach { h ->
                    FilterChip(selected = horarioId == h.id, onClick = { horarioId = h.id }, enabled = vm.disponible(h.id), label = { Text(h.hora) })
                }
            }
        }
        val puedeReservar = horarioId != -1 && vm.disponible(horarioId) && !habilitado
        Button(onClick = { avisar(vm.reservar(horarioId)) }, enabled = puedeReservar, modifier = Modifier.fillMaxWidth()) { Text("Solicitar ficha") }
        Text("La reserva queda pendiente hasta que el médico la acepte.", fontSize = 12.sp)
    }
}

@Composable
private fun ListaDeFichas(vm: CanchaViewModel) {
    Titulo(if (vm.rol == Rol.MEDICO) "Solicitudes médicas" else "Mis fichas")
    val visibles = vm.fichas.filter { vm.rol == Rol.MEDICO || it.jugadorId == vm.jugadorActualId }
    if (visibles.isEmpty()) {
        Panel { Text("Todavía no hay solicitudes. Reserva una ficha desde el perfil Jugador.") }
        return
    }
    visibles.reversed().forEach { ficha -> TarjetaDeFicha(vm, ficha) }
}

@Composable
private fun TarjetaDeFicha(vm: CanchaViewModel, ficha: FichaMedica) {
    val horario = vm.horarios.first { it.id == ficha.horarioId }
    Panel {
        Text("Ficha #${ficha.id} · ${vm.jugador(ficha.jugadorId).nombre}", fontWeight = FontWeight.Bold)
        Text("${horario.fecha} · ${horario.hora}")
        Estado(ficha.estado.name, ficha.estado in listOf(EstadoFicha.ACEPTADA, EstadoFicha.ATENDIDA))

        if (vm.rol == Rol.MEDICO && ficha.estado == EstadoFicha.PENDIENTE) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.responder(ficha, true) }) { Text("Aceptar") }
                OutlinedButton(onClick = { vm.responder(ficha, false) }) { Text("Rechazar") }
            }
        }
        if (vm.rol == Rol.MEDICO && ficha.estado == EstadoFicha.ACEPTADA) {
            Text("Después de la revisión presencial:", fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.evaluar(ficha, true) }) { Text("Apto") }
                OutlinedButton(onClick = { vm.evaluar(ficha, false) }) { Text("No apto") }
            }
        }
    }
}
