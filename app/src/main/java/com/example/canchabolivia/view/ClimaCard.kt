package com.example.canchabolivia.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.canchabolivia.NaranjaFH
import com.example.canchabolivia.model.Aptitud
import com.example.canchabolivia.model.Clima
import com.example.canchabolivia.viewmodel.ClimaViewModel
import kotlin.math.roundToInt

private val FondoClima = Color(0xFF111111)
private val PanelClima = Color(0xFF1E1E1E)
private val TextoSuave = Color(0xFFBBBBBB)
private val VerdeClima = Color(0xFF4CAF50)
private val RojoClima = Color(0xFFE5484D)

private fun Aptitud.color(): Color = when (this) {
    Aptitud.IDEAL -> VerdeClima
    Aptitud.PRECAUCION -> NaranjaFH
    Aptitud.NO_RECOMENDADO -> RojoClima
}

private fun Aptitud.icono(): String = when (this) {
    Aptitud.IDEAL -> "✔"
    Aptitud.PRECAUCION -> "!"
    Aptitud.NO_RECOMENDADO -> "✖"
}

// Pantalla: tarjeta del clima en la cancha (datos de Open-Meteo vía Retrofit,
// con la última lectura guardada en Room para verla sin internet).
@Composable
fun ClimaCard(vm: ClimaViewModel = viewModel()) {
    val estado by vm.estado.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = FondoClima),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Encabezado(cargando = estado.cargando, alActualizar = vm::actualizar)

            val clima = estado.clima
            when {
                clima != null -> {
                    ContenidoClima(clima)
                    estado.error?.let { Aviso(it) }
                }
                estado.cargando -> Cargando()
                else -> SinDatos(estado.error, vm::actualizar)
            }
        }
    }
}

@Composable
private fun Encabezado(cargando: Boolean, alActualizar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("CLIMA EN LA CANCHA", color = NaranjaFH, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("La Paz, Bolivia", color = TextoSuave, fontSize = 12.sp)
        }

        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp).padding(2.dp),
                color = NaranjaFH,
                strokeWidth = 2.dp
            )
        } else {
            TextButton(onClick = alActualizar) {
                Text("↻ Actualizar", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ContenidoClima(clima: Clima) {
    val aptitud = clima.aptitud
    val colorAptitud by animateColorAsState(aptitud.color(), tween(500), label = "colorAptitud")

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                "${clima.temperatura.roundToInt()}",
                color = Color.White,
                fontSize = 64.sp,
                lineHeight = 64.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "°C",
                color = NaranjaFH,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(colorAptitud.copy(alpha = 0.18f))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(colorAptitud))
            Spacer(Modifier.width(7.dp))
            Text(
                "${aptitud.icono()}  ${aptitud.titulo}",
                color = colorAptitud,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Text(clima.consejo, color = Color.White, fontSize = 14.sp, lineHeight = 19.sp)

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Medidor(
            titulo = "HUMEDAD",
            valor = "${clima.humedad}%",
            progreso = clima.humedad / 100f,
            color = Color(0xFF5BC0EB),
            modifier = Modifier.weight(1f)
        )
        Medidor(
            titulo = "VIENTO",
            valor = "${clima.viento.roundToInt()} km/h",
            // 50 km/h ya llena la barra: más que eso no se juega.
            progreso = (clima.viento / 50.0).toFloat().coerceIn(0f, 1f),
            color = NaranjaFH,
            modifier = Modifier.weight(1f)
        )
    }

    val origen = if (clima.desdeCache) "Última lectura guardada" else "Actualizado"
    Text(
        "$origen · ${clima.horaCorta} · Open-Meteo",
        color = TextoSuave,
        fontSize = 11.sp
    )
}

@Composable
private fun Medidor(
    titulo: String,
    valor: String,
    progreso: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animado by animateFloatAsState(progreso.coerceIn(0f, 1f), tween(700), label = "medidor")

    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PanelClima)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(titulo, color = TextoSuave, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(valor, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF333333))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animado)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

@Composable
private fun Cargando() {
    Text("Consultando el clima…", color = TextoSuave, fontSize = 14.sp)
}

@Composable
private fun Aviso(texto: String) {
    Text(
        texto,
        color = NaranjaFH,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NaranjaFH.copy(alpha = 0.12f))
            .padding(10.dp)
    )
}

@Composable
private fun SinDatos(error: String?, reintentar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            error ?: "No se pudo obtener el clima.",
            color = Color.White,
            fontSize = 14.sp
        )
        TextButton(onClick = reintentar) {
            Text("Reintentar", color = NaranjaFH, fontWeight = FontWeight.Bold)
        }
    }
}
