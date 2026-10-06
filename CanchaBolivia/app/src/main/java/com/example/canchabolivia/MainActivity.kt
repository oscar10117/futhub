package com.example.canchabolivia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

// =====================================================
// MAINACTIVITY.KT
// Aquí está toda la interfaz (lo que se ve en pantalla).
// Cada @Composable es un pedazo de pantalla. La lógica y los
// datos vienen del ViewModel (vm).
// =====================================================

// ---------- COLORES FUTHUB ----------
val NaranjaFH = Color(0xFFED9628)       // color principal
val NaranjaClaroFH = Color(0xFFFFE2B8)  // fondo de las etiquetas positivas
val NegroFH = Color(0xFF000000)
val BlancoFH = Color(0xFFFFFFFF)
val FondoFH = Color(0xFFF4F4F4)         // fondo general de la app
val GrisFH = Color(0xFF666666)          // textos secundarios
val GrisClaroFH = Color(0xFFE7E7E7)     // fondo de las etiquetas negativas

// La actividad es el punto de entrada de la app
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // la app ocupa toda la pantalla, también detrás de las barras del sistema
        setContent {
            // Define el esquema de colores de toda la app
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = NaranjaFH,
                    onPrimary = NegroFH,
                    secondary = NegroFH,
                    onSecondary = BlancoFH,
                    background = FondoFH,
                    onBackground = NegroFH,
                    surface = BlancoFH,
                    onSurface = NegroFH,
                    outline = NaranjaFH
                )
            ) {
                CanchaApp()
            }
        }
    }
}

// Decide qué pantalla mostrar: login si nadie inició sesión, o la app principal
@Composable
fun CanchaApp(vm: CanchaViewModel = viewModel()) {
    if (vm.usuarioLogueado == null) {
        LoginPantalla(vm)
    } else {
        Principal(vm)
    }
}

// ---------- PANTALLA PRINCIPAL ----------
// Tiene la cabecera negra, el contenido de la pestaña elegida y la barra de abajo
@Composable
private fun Principal(vm: CanchaViewModel) {
    // Snackbar = el mensajito que aparece abajo (ej. "Resultado guardado")
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // avisar() muestra un mensaje en el snackbar. Se pasa a las pantallas que lo necesitan.
    val avisar: (String) -> Unit = { mensaje ->
        scope.launch { snack.showSnackbar(mensaje) }
    }

    // Pestaña actual: 0 Inicio, 1 Fichas, 2 Equipos, 3 Torneo
    var pagina by rememberSaveable { mutableIntStateOf(0) }

    val menu = listOf("Inicio", "Fichas", "Equipos", "Torneo")
    val iconos = listOf("◉", "＋", "▤", "☆")
    // Título grande de la cabecera para cada pestaña
    val titulos = listOf(
        "Todo listo para jugar",
        "Tu revisión, sin filas",
        "Equipos y jugadores",
        "El torneo en tus manos"
    )

    Scaffold(
        containerColor = FondoFH,
        snackbarHost = { SnackbarHost(snack) },
        // Barra de navegación negra de abajo
        bottomBar = {
            NavigationBar(containerColor = NegroFH) {
                menu.forEachIndexed { i, nombre ->
                    NavigationBarItem(
                        selected = pagina == i,
                        onClick = { pagina = i },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NegroFH,
                            selectedTextColor = NaranjaFH,
                            indicatorColor = NaranjaFH,
                            unselectedIconColor = BlancoFH,
                            unselectedTextColor = BlancoFH
                        ),
                        icon = { Text(iconos[i], fontSize = 23.sp) },
                        label = { Text(nombre) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Cabecera negra: logo, botón de cerrar sesión y título de la pestaña
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(NegroFH)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("FUTHUB", color = NaranjaFH, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    TextButton(onClick = { vm.cerrarSesion() }) {
                        Text("Cerrar sesión", color = BlancoFH, fontSize = 12.sp)
                    }
                }
                Text(titulos[pagina], color = BlancoFH, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Fútbol organizado. Simple y rápido.", color = Color(0xFFBBBBBB), fontSize = 12.sp)
            }

            // Contenido de la pestaña elegida (con scroll vertical).
            // weight(1f) hace que ocupe todo el espacio que sobra.
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (pagina) {
                    0 -> Inicio(vm) { pagina = it } // el botón de Inicio puede cambiar de pestaña
                    1 -> Fichas(vm, avisar)
                    2 -> Equipos(vm)
                    3 -> Torneos(vm, avisar)
                }
            }
        }
    }
}

// ---------- COMPONENTES REUTILIZABLES ----------

// Título con un texto de detalle opcional abajo
@Composable
private fun Titulo(texto: String, detalle: String? = null) {
    Column {
        Text(texto, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = NegroFH)
        if (detalle != null) {
            Text(detalle, color = GrisFH, fontSize = 13.sp)
        }
    }
}

// Tarjeta blanca con esquinas redondeadas y sombra, donde va el contenido
@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BlancoFH),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            content = content
        )
    }
}

// Etiqueta pequeña de estado: naranja claro si es positivo, gris si no
@Composable
private fun Estado(texto: String, positivo: Boolean = true) {
    Text(
        texto,
        modifier = Modifier
            .background(
                if (positivo) NaranjaClaroFH else GrisClaroFH,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = NegroFH,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
}

// ---------- INICIO ----------
// "abrir" es una función para cambiar a otra pestaña (se usa en los botones)
@Composable
private fun Inicio(vm: CanchaViewModel, abrir: (Int) -> Unit) {
    Titulo("Hola, ${vm.usuarioLogueado?.nombre}", "Copa local · La Paz, Bolivia")

    // Tarjeta de bienvenida
    Panel {
        Text(
            "EL PARTIDO EMPIEZA ANTES DE LA CANCHA",
            fontSize = 11.sp,
            color = GrisFH,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Menos filas.\nMás fútbol.",
            fontSize = 32.sp,
            lineHeight = 35.sp,
            fontWeight = FontWeight.Bold,
            color = NegroFH
        )
        Text("Reserva tu revisión y consulta tu habilitación para el torneo.")
        Button(onClick = { abrir(1) }, modifier = Modifier.fillMaxWidth()) {
            Text("Reservar ficha médica", fontWeight = FontWeight.Bold)
        }
    }

    // Dos tarjetas con números: equipos inscritos y jugadores habilitados
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = NaranjaFH)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "${vm.torneo.equipoIds.size}",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = NegroFH
                )
                Text("Equipos inscritos", color = NegroFH)
            }
        }

        Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = NegroFH)) {
            Column(Modifier.padding(16.dp)) {
                // Cuenta los jugadores de equipos inscritos que ya están habilitados
                val habilitados = vm.jugadores.count {
                    it.equipoId in vm.torneo.equipoIds && vm.habilitado(it.id)
                }
                Text("$habilitados", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = NaranjaFH)
                Text("Habilitados", color = BlancoFH)
            }
        }
    }

    // Resumen del torneo con accesos rápidos
    Panel {
        Titulo(vm.torneo.nombre, "Todos contra todos")
        Text("${vm.partidos.size} partidos en el fixture")
        OutlinedButton(onClick = { abrir(3) }, modifier = Modifier.fillMaxWidth()) {
            Text("Ver torneo")
        }
        TextButton(onClick = { abrir(2) }) {
            Text("Consultar jugadores por equipo →")
        }
    }
}

// ---------- FICHAS MÉDICAS ----------
// Cada rol ve algo distinto en esta pestaña
@Composable
private fun Fichas(vm: CanchaViewModel, avisar: (String) -> Unit) {
    when (vm.rol) {
        // El jugador reserva y ve sus propias fichas
        Rol.JUGADOR -> {
            ReservaFicha(vm, avisar)
            Titulo("Mis fichas")
            ListaFichas(vm)
        }

        // El médico ve las solicitudes de todos
        Rol.MEDICO -> {
            Titulo("Solicitudes médicas")
            ListaFichas(vm)
        }

        // Árbitro y organizador no usan esta pestaña
        else -> Panel {
            Titulo("Agenda médica")
            Text("Las fichas médicas las gestionan el jugador y el médico.")
        }
    }
}

// Formulario para que el jugador elija un horario y solicite su ficha
@Composable
private fun ReservaFicha(vm: CanchaViewModel, avisar: (String) -> Unit) {
    // Horario elegido (-1 = ninguno todavía)
    var horarioId by rememberSaveable { mutableIntStateOf(-1) }
    val habilitado = vm.habilitado(vm.jugadorActualId)

    Titulo(
        "Reserva una ficha",
        "Jugador: ${vm.jugador(vm.jugadorActualId).nombre} · ${vm.torneo.nombre}"
    )

    Estado(if (habilitado) "Habilitado para jugar" else "Aún no habilitado", habilitado)

    Panel {
        Text("Dra. Ana Rojas", fontWeight = FontWeight.Bold)
        Text("Revisión presencial · Centro médico de la asociación", fontSize = 13.sp)

        // Se agrupan los horarios por fecha y cada fecha muestra sus horas como botones
        vm.horarios.groupBy { it.fecha }.forEach { (fecha, horas) ->
            Text(fecha, fontWeight = FontWeight.SemiBold)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                horas.forEach { h ->
                    FilterChip(
                        selected = horarioId == h.id,
                        onClick = { horarioId = h.id },
                        enabled = vm.disponible(h.id), // si ya está reservado, se desactiva
                        label = { Text(h.hora) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NaranjaFH,
                            selectedLabelColor = NegroFH
                        )
                    )
                }
            }
        }

        // El botón solo funciona si hay un horario elegido, está libre y el jugador aún no es apto
        Button(
            onClick = {
                avisar(vm.reservar(horarioId))
                horarioId = -1
            },
            enabled = horarioId != -1 && vm.disponible(horarioId) && !habilitado,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Solicitar ficha")
        }

        Text(
            "La reserva queda pendiente hasta que el médico la acepte.",
            fontSize = 12.sp,
            color = GrisFH
        )
    }
}

// Lista de fichas: el médico ve todas, el jugador solo las suyas
@Composable
private fun ListaFichas(vm: CanchaViewModel) {
    val visibles = vm.fichas.filter {
        vm.rol == Rol.MEDICO || it.jugadorId == vm.jugadorActualId
    }

    if (visibles.isEmpty()) {
        Panel { Text("Todavía no hay solicitudes.") }
    }

    // reversed() para mostrar primero las más recientes
    visibles.reversed().forEach { ficha ->
        val horario = vm.horarios.first { it.id == ficha.horarioId }

        Panel {
            Text(
                "Ficha #${ficha.id} · ${vm.jugador(ficha.jugadorId).nombre}",
                fontWeight = FontWeight.Bold
            )
            Text("${horario.fecha} · ${horario.hora}")
            Estado(
                ficha.estado.name,
                ficha.estado == EstadoFicha.ACEPTADA || ficha.estado == EstadoFicha.APTO
            )

            // El médico acepta o rechaza las solicitudes pendientes
            if (vm.rol == Rol.MEDICO && ficha.estado == EstadoFicha.PENDIENTE) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.cambiarEstado(ficha, EstadoFicha.ACEPTADA) }) {
                        Text("Aceptar")
                    }
                    OutlinedButton(onClick = { vm.cambiarEstado(ficha, EstadoFicha.RECHAZADA) }) {
                        Text("Rechazar")
                    }
                }
            }

            // Después de la revisión presencial, el médico marca si el jugador es apto
            if (vm.rol == Rol.MEDICO && ficha.estado == EstadoFicha.ACEPTADA) {
                Text("Después de la revisión presencial:", fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.cambiarEstado(ficha, EstadoFicha.APTO) }) {
                        Text("Apto")
                    }
                    OutlinedButton(onClick = { vm.cambiarEstado(ficha, EstadoFicha.NO_APTO) }) {
                        Text("No apto")
                    }
                }
            }
        }
    }
}

// ---------- EQUIPOS ----------
// Muestra los jugadores de un equipo y si están habilitados
@Composable
private fun Equipos(vm: CanchaViewModel) {
    var equipoId by rememberSaveable { mutableIntStateOf(1) }          // equipo seleccionado
    var soloHabilitados by rememberSaveable { mutableStateOf(false) }  // filtro del checkbox

    Titulo("Planteles", "Habilitación para ${vm.torneo.nombre}")

    // Una etiqueta por equipo para elegir cuál ver
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        vm.equipos.forEach { equipo ->
            FilterChip(
                selected = equipoId == equipo.id,
                onClick = { equipoId = equipo.id },
                label = { Text(equipo.sigla) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NaranjaFH,
                    selectedLabelColor = NegroFH
                )
            )
        }
    }

    Panel {
        Titulo(vm.equipo(equipoId).nombre)

        val inscrito = equipoId in vm.torneo.equipoIds
        Estado(if (inscrito) "Equipo inscrito" else "No inscrito en este torneo", inscrito)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = soloHabilitados, onCheckedChange = { soloHabilitados = it })
            Text("Mostrar solo habilitados")
        }

        // Jugadores del equipo; si el checkbox está marcado, solo los habilitados
        val lista = vm.jugadores.filter {
            it.equipoId == equipoId && (!soloHabilitados || vm.habilitado(it.id))
        }

        if (lista.isEmpty()) {
            Text("No hay jugadores habilitados en este plantel.")
        }

        lista.forEach { jugador ->
            HorizontalDivider(color = GrisClaroFH)
            // padStart(2, '0') muestra el dorsal con dos dígitos (ej. 07)
            Text(
                "${jugador.dorsal.toString().padStart(2, '0')}   ${jugador.nombre}",
                fontWeight = FontWeight.Bold
            )
            Estado(
                if (vm.habilitado(jugador.id)) "Habilitado" else "No habilitado",
                vm.habilitado(jugador.id)
            )
        }
    }

    Text(
        "El árbitro consulta el estado de habilitación. No se muestran diagnósticos médicos.",
        fontSize = 12.sp,
        color = GrisFH
    )
}

// ---------- TORNEO ----------
@Composable
private fun Torneos(vm: CanchaViewModel, avisar: (String) -> Unit) {
    // Partido al que se le está cargando el resultado (null = ningún diálogo abierto)
    var resultado by remember { mutableStateOf<Partido?>(null) }

    Titulo(vm.torneo.nombre, "Todos contra todos")

    // Resumen del torneo y botón para generar el fixture
    Panel {
        Text(
            "${vm.torneo.equipoIds.size} equipos · ${vm.partidos.size} partidos",
            fontWeight = FontWeight.Bold
        )
        // Siglas de los equipos separadas por "·"
        Text(
            vm.torneo.equipoIds.joinToString(" · ") { vm.equipo(it).sigla },
            fontSize = 13.sp
        )

        // Solo si aún no hay partidos: el organizador puede generarlos
        if (vm.partidos.isEmpty()) {
            if (vm.rol == Rol.ORGANIZADOR) {
                Button(onClick = { avisar(vm.generar()) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Sortear y generar fixture")
                }
            } else {
                Text("El organizador debe generar los partidos.", fontSize = 13.sp)
            }
        }
    }

    // Tabla y partidos solo aparecen cuando ya existe el fixture
    if (vm.partidos.isNotEmpty()) {
        Panel {
            Titulo("Tabla de posiciones")

            // Encabezado de la tabla (PJ = partidos jugados, DG = diferencia de goles, PTS = puntos)
            Row(Modifier.fillMaxWidth()) {
                Text("Equipo", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("PJ", Modifier.width(40.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("DG", Modifier.width(40.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("PTS", Modifier.width(36.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            // Una fila por equipo, ya ordenada por puntos
            vm.tabla().forEachIndexed { i, p ->
                Row(Modifier.fillMaxWidth()) {
                    Text("${i + 1}. ${vm.equipo(p.equipoId).sigla}", Modifier.weight(1f))
                    Text("${p.jugados}", Modifier.width(40.dp))
                    Text("${p.diferencia}", Modifier.width(40.dp))
                    Text("${p.puntos}", Modifier.width(36.dp), fontWeight = FontWeight.Bold)
                }
            }

            Text(
                "Victoria 3 · Empate 1 · Derrota 0\nDesempate: diferencia de goles y goles a favor.",
                fontSize = 11.sp
            )
        }

        Titulo("Partidos")

        // Una tarjeta por partido
        vm.partidos.forEach { partido ->
            Panel {
                Text(
                    "${vm.equipo(partido.localId).nombre}  vs  ${vm.equipo(partido.visitanteId).nombre}",
                    fontWeight = FontWeight.Bold
                )

                // Si ya se jugó muestra el marcador, si no, la etiqueta "Pendiente"
                if (partido.finalizado()) {
                    Text(
                        "${partido.golesLocal} — ${partido.golesVisitante}",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = NaranjaFH
                    )
                } else {
                    Estado("Pendiente", false)
                }

                // Solo el árbitro y el organizador pueden cargar resultados
                if (vm.rol == Rol.ARBITRO || vm.rol == Rol.ORGANIZADOR) {
                    OutlinedButton(onClick = { resultado = partido }) {
                        Text(if (partido.finalizado()) "Modificar resultado" else "Cargar resultado")
                    }
                }
            }
        }
    }

    // Si hay un partido elegido, se muestra el diálogo para escribir los goles
    resultado?.let { partido ->
        ResultadoDialogo(
            partido = partido,
            cerrar = { resultado = null },
            guardar = { gl, gv ->
                vm.guardarResultado(partido, gl, gv)
                avisar("Resultado guardado")
            }
        )
    }
}

// Ventana emergente para escribir los goles de local y visitante
@Composable
private fun ResultadoDialogo(
    partido: Partido,
    cerrar: () -> Unit,
    guardar: (Int, Int) -> Unit
) {
    // Los goles se guardan como texto mientras se escribe; si ya había resultado, se muestra
    var gl by rememberSaveable { mutableStateOf(partido.golesLocal?.toString() ?: "0") }
    var gv by rememberSaveable { mutableStateOf(partido.golesVisitante?.toString() ?: "0") }

    AlertDialog(
        onDismissRequest = cerrar, // tocar fuera de la ventana la cierra
        title = { Text("Registrar resultado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = gl,
                    onValueChange = { gl = it },
                    label = { Text("Goles Local") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = gv,
                    onValueChange = { gv = it },
                    label = { Text("Goles Visitante") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                // toIntOrNull convierte el texto a número; si está vacío o inválido usa 0
                guardar(gl.toIntOrNull() ?: 0, gv.toIntOrNull() ?: 0)
                cerrar()
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = cerrar) { Text("Cancelar") }
        }
    )
}

// ---------- LOGIN ----------
@Composable
private fun LoginPantalla(vm: CanchaViewModel) {
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") } // mensaje si el login falla

    // Fondo negro con la tarjeta de login centrada
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFH)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = BlancoFH)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("FUTHUB", color = NaranjaFH, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                Text("Iniciar Sesión", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = NegroFH)

                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = { Text("Correo electrónico") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = clave,
                    onValueChange = { clave = it },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                // El mensaje de error solo se ve si hay uno
                if (error.isNotEmpty()) {
                    Text(error, color = Color(0xFFB00020), fontSize = 13.sp)
                }

                // iniciarSesion devuelve null si salió bien (entonces el error queda vacío)
                Button(
                    onClick = { error = vm.iniciarSesion(correo, clave) ?: "" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ingresar", fontWeight = FontWeight.Bold)
                }

                Text(
                    "Cuentas demo (clave: 123):\n• jugador@futhub.com\n• medico@futhub.com\n• arbitro@futhub.com\n• admin@futhub.com",
                    fontSize = 11.sp,
                    color = GrisFH
                )
            }
        }
    }
}