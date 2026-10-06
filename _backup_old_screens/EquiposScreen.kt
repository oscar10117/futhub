package com.example.canchabolivia

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Equipos(vm: CanchaViewModel) {
    var equipoId by rememberSaveable { mutableIntStateOf(1) }
    var soloHabilitados by rememberSaveable { mutableStateOf(false) }

    Titulo("Planteles", "Habilitación para ${vm.torneo.nombre}")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        vm.equipos.forEach { equipo -> FilterChip(selected = equipoId == equipo.id, onClick = { equipoId = equipo.id }, label = { Text(equipo.sigla) }) }
    }
    Panel {
        val inscrito = equipoId in vm.torneo.equipoIds
        Titulo(vm.equipo(equipoId).nombre)
        Estado(if (inscrito) "Equipo inscrito" else "No inscrito en este torneo", inscrito)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = soloHabilitados, onCheckedChange = { soloHabilitados = it })
            Text("Mostrar solo habilitados")
        }

        val lista = vm.jugadores.filter { it.equipoId == equipoId && (!soloHabilitados || vm.habilitado(it.id)) }
        if (lista.isEmpty()) Text("No hay jugadores habilitados en este plantel.")
        lista.forEach { jugador ->
            HorizontalDivider(color = Fondo)
            Text("${jugador.dorsal.toString().padStart(2, '0')}   ${jugador.nombre}", fontWeight = FontWeight.Bold)
            Estado(if (vm.habilitado(jugador.id)) "Habilitado" else "No habilitado", vm.habilitado(jugador.id))
        }
    }
    Text("El árbitro consulta el estado de habilitación. No se muestran diagnósticos médicos.", fontSize = 12.sp, color = Color(0xFF62746D))
}
