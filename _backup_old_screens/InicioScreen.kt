package com.example.canchabolivia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Inicio(vm: CanchaViewModel, abrir: (Int) -> Unit) {
    Titulo("Hola, ${vm.usuarios.first { it.rol == vm.rol }.nombre}", "Copa local · La Paz, Bolivia")
    Panel {
        Text("EL PARTIDO EMPIEZA ANTES DE LA CANCHA", fontSize = 11.sp, color = Color(0xFF62746D), fontWeight = FontWeight.Bold)
        Text("Menos filas.\nMás fútbol.", fontSize = 32.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold, color = Verde)
        Text("Reserva tu revisión y consulta tu habilitación para el torneo.")
        Button(onClick = { abrir(1) }, modifier = Modifier.fillMaxWidth()) { Text("Reservar ficha médica") }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val habilitados = vm.jugadores.count { it.equipoId in vm.torneo.equipoIds && vm.habilitado(it.id) }
        Estadistica(Modifier.weight(1f), Lima, "${vm.torneo.equipoIds.size}", "Equipos inscritos")
        Estadistica(Modifier.weight(1f), Color(0xFFE2ECE7), "$habilitados", "Habilitados")
    }
    Panel {
        Titulo(vm.torneo.nombre, vm.torneo.formato.titulo)
        Text("${vm.partidos.size} partidos en el fixture")
        OutlinedButton(onClick = { abrir(3) }, modifier = Modifier.fillMaxWidth()) { Text("Ver torneo") }
        TextButton(onClick = { abrir(2) }) { Text("Consultar jugadores por equipo →") }
    }
}

@Composable
private fun Estadistica(modifier: Modifier, color: Color, numero: String, etiqueta: String) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(16.dp)) {
            Text(numero, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(etiqueta)
        }
    }
}
