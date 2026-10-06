package com.example.canchabolivia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Título de sección, con un detalle opcional debajo.
@Composable
fun Titulo(texto: String, detalle: String? = null) {
    Column {
        Text(texto, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Verde)
        if (detalle != null) Text(detalle, color = Color(0xFF62746D), fontSize = 13.sp)
    }
}

// Tarjeta blanca estándar que envuelve el contenido de cada bloque.
@Composable
fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp), content = content)
    }
}

// Etiqueta de estado (verde = positivo, ámbar = atención).
@Composable
fun Estado(texto: String, positivo: Boolean = true) {
    val fondo = if (positivo) Color(0xFFE7F2DD) else Color(0xFFFFEAC9)
    val color = if (positivo) Verde else Color(0xFF85551C)
    Text(
        texto,
        modifier = Modifier.background(fondo, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )
}
