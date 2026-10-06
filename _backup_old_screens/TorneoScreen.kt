package com.example.canchabolivia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Torneos(vm: CanchaViewModel, avisar: (String) -> Unit) {
    var editando by rememberSaveable { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<Partido?>(null) }

    if (editando) {
        Configurar(vm, { editando = false }, avisar)
        return
    }

    Titulo(vm.torneo.nombre, vm.torneo.formato.titulo)
    ResumenDelTorneo(vm, avisar) { editando = true }
    if (vm.torneo.formato == Formato.LIGA) TablaDePosiciones(vm)

    vm.partidos.groupBy { it.ronda }.forEach { (ronda, partidos) ->
        Titulo(etiquetaDeRonda(vm.torneo.formato, ronda, partidos.size))
        partidos.forEach { partido -> TarjetaDePartido(vm, partido) { resultado = partido } }
    }

    resultado?.let { partido -> ResultadoDialogo(vm, partido, { resultado = null }, avisar) }
}

private fun etiquetaDeRonda(formato: Formato, ronda: Int, partidosEnRonda: Int): String {
    if (formato == Formato.LIGA) return "Fecha $ronda"
    return when (partidosEnRonda) {
        1 -> "Final"
        2 -> "Semifinales"
        4 -> "Cuartos de final"
        8 -> "Octavos de final"
        else -> "Ronda $ronda"
    }
}

@Composable
private fun ResumenDelTorneo(vm: CanchaViewModel, avisar: (String) -> Unit, configurar: () -> Unit) {
    Panel {
        Text("${vm.torneo.equipoIds.size} equipos · ${vm.partidos.size} partidos", fontWeight = FontWeight.Bold)
        Text(vm.torneo.equipoIds.joinToString(" · ") { vm.equipo(it).sigla }, fontSize = 13.sp)

        if (vm.rol == Rol.ORGANIZADOR && vm.partidos.isEmpty()) {
            OutlinedButton(onClick = configurar, modifier = Modifier.fillMaxWidth()) { Text("Configurar torneo y equipos") }
            Button(onClick = { avisar(vm.generar(true)) }, modifier = Modifier.fillMaxWidth()) { Text("Sortear y generar fixture") }
            TextButton(onClick = { avisar(vm.generar(false)) }) { Text("Generar con el orden elegido") }
        }
        if (vm.partidos.isEmpty()) Text("El organizador debe generar los partidos.", fontSize = 13.sp)
        if (vm.rol == Rol.ORGANIZADOR && vm.torneo.formato == Formato.ELIMINACION && vm.partidos.isNotEmpty() && vm.campeon() == null) {
            Button(onClick = { avisar(vm.avanzar()) }) { Text("Crear siguiente ronda") }
        }
        vm.campeon()?.let { Text("★ $it", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Verde) }
    }
}

@Composable
private fun TablaDePosiciones(vm: CanchaViewModel) {
    Panel {
        Titulo("Tabla de posiciones")
        Text("Equipo           PJ    DG    PTS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        MotorTorneo.tabla(vm.torneo, vm.partidos).forEachIndexed { i, p ->
            Row(Modifier.fillMaxWidth()) {
                Text("${i + 1}. ${vm.equipo(p.equipoId).sigla}", Modifier.weight(1f))
                Text("${p.jugados}", Modifier.width(40.dp))
                Text("${p.diferencia}", Modifier.width(40.dp))
                Text("${p.puntos}", Modifier.width(32.dp), fontWeight = FontWeight.Bold)
            }
        }
        Text(
            "Victoria 3 · Empate 1 · Derrota 0\nDesempate: diferencia de goles y goles a favor. Si persiste, se requiere una definición adicional.",
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun TarjetaDePartido(vm: CanchaViewModel, partido: Partido, registrar: () -> Unit) {
    Panel {
        val visitante = partido.visitanteId?.let { vm.equipo(it).nombre } ?: "Pase libre"
        Text("${vm.equipo(partido.localId).nombre}  vs  $visitante", fontWeight = FontWeight.Bold)
        when {
            partido.visitanteId == null -> Estado("Clasifica automáticamente")
            partido.finalizado() -> {
                Text("${partido.golesLocal} — ${partido.golesVisitante}", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Verde)
                partido.ganadorPenalesId?.let { Text("Penales: ${vm.equipo(it).nombre}", fontSize = 12.sp) }
            }
            else -> {
                Estado("Por jugar", false)
                if (vm.rol == Rol.ARBITRO) Button(onClick = registrar) { Text("Registrar resultado") }
            }
        }
        Text("Árbitro: Luis Flores", fontSize = 12.sp, color = Color(0xFF62746D))
    }
}

@Composable
private fun Configurar(vm: CanchaViewModel, cerrar: () -> Unit, avisar: (String) -> Unit) {
    var nombre by remember { mutableStateOf(vm.torneo.nombre) }
    var formato by remember { mutableStateOf(vm.torneo.formato) }
    var ids by remember { mutableStateOf(vm.torneo.equipoIds) }

    Titulo("Configurar torneo", "Selecciona equipos y ajusta su orden antes del sorteo.")
    OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Formato.entries.forEach { f ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = formato == f, onClick = { formato = f })
            Text(f.titulo)
        }
    }
    Panel {
        vm.equipos.forEach { equipo ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = equipo.id in ids, onCheckedChange = { marcado -> ids = if (marcado) ids + equipo.id else ids - equipo.id })
                Text(equipo.nombre)
            }
        }
    }
    Panel {
        Text("Orden manual", fontWeight = FontWeight.Bold)
        Text("En eliminatorias se emparejan los equipos consecutivos; si faltan plazas, los primeros reciben pase libre.", fontSize = 12.sp)
        ids.forEachIndexed { indice, id ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${indice + 1}. ${vm.equipo(id).sigla}", Modifier.weight(1f))
                TextButton(enabled = indice > 0, onClick = { ids = subirEnLaLista(ids, indice) }) { Text("Subir ↑") }
            }
        }
    }
    Button(enabled = nombre.isNotBlank() && ids.size >= 2, onClick = { avisar(vm.configurar(nombre, formato, ids)); cerrar() }, modifier = Modifier.fillMaxWidth()) {
        Text("Guardar configuración")
    }
    TextButton(onClick = cerrar) { Text("Cancelar") }
}

private fun subirEnLaLista(ids: List<Int>, indice: Int): List<Int> =
    ids.toMutableList().apply { this[indice - 1] = ids[indice]; this[indice] = ids[indice - 1] }

@Composable
private fun ResultadoDialogo(vm: CanchaViewModel, partido: Partido, cerrar: () -> Unit, avisar: (String) -> Unit) {
    var local by remember { mutableStateOf("") }
    var visita by remember { mutableStateOf("") }
    var penales by remember { mutableStateOf<Int?>(null) }

    val golesLocal = local.toIntOrNull()
    val golesVisita = visita.toIntOrNull()
    val requierePenales = vm.torneo.formato == Formato.ELIMINACION && golesLocal != null && golesLocal == golesVisita

    AlertDialog(
        onDismissRequest = cerrar,
        title = { Text("Confirmar marcador") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${vm.equipo(partido.localId).nombre} vs ${vm.equipo(partido.visitanteId!!).nombre}")
                OutlinedTextField(value = local, onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) local = it }, label = { Text("Goles local") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = visita, onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) visita = it }, label = { Text("Goles visitante") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                if (requierePenales) {
                    Text("Ganador por penales:")
                    listOf(partido.localId, partido.visitanteId).filterNotNull().forEach { id ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = penales == id, onClick = { penales = id })
                            Text(vm.equipo(id).sigla)
                        }
                    }
                }
                Text("Al confirmar, el resultado queda bloqueado en esta demostración.", fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(
                enabled = golesLocal != null && golesVisita != null && (!requierePenales || penales != null),
                onClick = { avisar(vm.resultado(partido.id, golesLocal!!, golesVisita!!, if (requierePenales) penales else null)); cerrar() },
            ) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = cerrar) { Text("Cancelar") } },
    )
}
