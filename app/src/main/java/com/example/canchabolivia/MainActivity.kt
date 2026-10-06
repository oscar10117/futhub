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

// COLORES FUTHUB
val NaranjaFH = Color(0xFFED9628)
val NaranjaClaroFH = Color(0xFFFFE2B8)
val NegroFH = Color(0xFF000000)
val BlancoFH = Color(0xFFFFFFFF)
val FondoFH = Color(0xFFF4F4F4)
val GrisFH = Color(0xFF666666)
val GrisClaroFH = Color(0xFFE7E7E7)
val RojoFH = Color(0xFFB00020)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
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

// Una pestaña de la barra inferior: título, símbolo y la pantalla que muestra.
// Qué pestañas existen depende del rol (ver pestanasPara), así que cada rol
// solo ve las herramientas que le corresponden.
private data class PestanaNav(
    val titulo: String,
    val icono: String,
    val subtitulo: String,
    val contenido: @Composable () -> Unit
)

@Composable
private fun pestanasPara(
    vm: CanchaViewModel,
    avisar: (String) -> Unit,
    abrir: (Int) -> Unit
): List<PestanaNav> = when (vm.rol) {
    Rol.JUGADOR -> listOf(
        PestanaNav("Inicio", "◉", "Todo listo para jugar") { Inicio(vm, abrir) },
        PestanaNav("Fichas", "＋", "Tu revisión, sin filas") { FichasJugador(vm, avisar) },
        PestanaNav("Equipos", "▤", "Equipos y jugadores") { Equipos(vm) },
        PestanaNav("Torneo", "☆", "El torneo en tus manos") { Torneos(vm, avisar) }
    )

    Rol.MEDICO -> listOf(
        PestanaNav("Inicio", "◉", "Todo listo para atender") { Inicio(vm, abrir) },
        PestanaNav("Agenda", "⚕", "Atiende tus citas del día") { AgendaMedico(vm) }
    )

    Rol.ARBITRO -> listOf(
        PestanaNav("Inicio", "◉", "Todo listo para dirigir") { Inicio(vm, abrir) },
        PestanaNav("Partidos", "⚑", "Resultados y tarjetas") { PartidosArbitro(vm, avisar) }
    )

    Rol.ORGANIZADOR -> listOf(
        PestanaNav("Inicio", "◉", "El torneo en tus manos") { Inicio(vm, abrir) },
        PestanaNav("Equipos", "▤", "Agrega o elimina equipos") { EquiposAdmin(vm, avisar) },
        PestanaNav("Torneo", "☆", "Formato, fixture y tabla") { Torneos(vm, avisar) }
    )
}

@Composable
fun CanchaApp(vm: CanchaViewModel = viewModel()) {

    val snack = remember {
        SnackbarHostState()
    }

    val scope = rememberCoroutineScope()

    val avisar: (String) -> Unit = { mensaje ->
        scope.launch {
            snack.showSnackbar(mensaje)
        }
        Unit
    }

    // SI NO HAY SESIÓN INICIADA, MOSTRAR PANTALLA DE LOGIN
    if (vm.usuarioLogueado == null) {
        LoginPantalla(vm, avisar)
        return
    }

    // La pestaña activa se reinicia a "Inicio" cada vez que cambia el rol
    // (por ejemplo al cerrar sesión y entrar con otra cuenta), para no
    // quedar apuntando a un índice que ese rol ya no tiene.
    var pagina by rememberSaveable(vm.rol) {
        mutableIntStateOf(0)
    }

    val tabs = pestanasPara(vm, avisar) { pagina = it }
    val paginaActual = pagina.coerceIn(0, tabs.lastIndex)

    Scaffold(
        containerColor = FondoFH,
        snackbarHost = {
            SnackbarHost(snack)
        },
        bottomBar = {
            NavigationBar(
                containerColor = NegroFH
            ) {
                tabs.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = paginaActual == i,
                        onClick = {
                            pagina = i
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NegroFH,
                            selectedTextColor = NaranjaFH,
                            indicatorColor = NaranjaFH,
                            unselectedIconColor = BlancoFH,
                            unselectedTextColor = BlancoFH
                        ),
                        icon = {
                            Text(tab.icono, fontSize = 23.sp)
                        },
                        label = {
                            Text(tab.titulo)
                        }
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

            // CABECERA NEGRA
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
                    Column {
                        Text(
                            "FUTHUB",
                            color = NaranjaFH,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            vm.rol.titulo,
                            color = Color(0xFFBBBBBB),
                            fontSize = 11.sp
                        )
                    }

                    TextButton(
                        onClick = { vm.cerrarSesion() }
                    ) {
                        Text("Cerrar sesión", color = BlancoFH, fontSize = 12.sp)
                    }
                }

                Text(
                    tabs[paginaActual].subtitulo,
                    color = BlancoFH,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                Text(
                    "Fútbol organizado. Simple y rápido.",
                    color = Color(0xFFBBBBBB),
                    fontSize = 12.sp
                )
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(horizontal = 20.dp)
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                tabs[paginaActual].contenido()

                Spacer(
                    Modifier.height(16.dp)
                )
            }
        }
    }
}

@Composable
private fun Titulo(
    texto: String,
    detalle: String? = null
) {
    Column {
        Text(
            texto,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = NegroFH
        )

        if (detalle != null) {
            Text(
                detalle,
                color = GrisFH,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun Panel(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = BlancoFH
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            content = content
        )
    }
}

@Composable
private fun Estado(
    texto: String,
    positivo: Boolean = true
) {
    Text(
        texto,
        modifier = Modifier
            .background(
                if (positivo)
                    NaranjaClaroFH
                else
                    GrisClaroFH,
                RoundedCornerShape(8.dp)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 5.dp
            ),
        color = NegroFH,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
}

// Pantalla de inicio: el mensaje y el botón principal cambian según el rol,
// cada quien ve la acción que más usa (reservar, atender, dirigir o
// configurar) en vez de una pantalla genérica para todos.
@Composable
private fun Inicio(
    vm: CanchaViewModel,
    abrir: (Int) -> Unit
) {
    Titulo(
        "Hola, ${vm.usuarioLogueado?.nombre ?: ""}",
        "Copa local · La Paz, Bolivia"
    )

    val (mensaje, textoBoton, destino) = when (vm.rol) {
        Rol.JUGADOR -> Triple(
            "Reserva tu revisión y consulta tu habilitación para el torneo.",
            "Reservar ficha médica",
            1
        )
        Rol.MEDICO -> Triple(
            "Revisa las solicitudes de los jugadores y decide quién queda habilitado.",
            "Ver agenda del día",
            1
        )
        Rol.ARBITRO -> Triple(
            "Carga resultados y registra tarjetas de tus partidos.",
            "Ver partidos",
            1
        )
        Rol.ORGANIZADOR -> Triple(
            "Arma los equipos, elige el formato y controla el torneo de principio a fin.",
            "Configurar torneo",
            2
        )
    }

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

        Text(mensaje)

        Button(
            onClick = { abrir(destino) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                textoBoton,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = NaranjaFH
            )
        ) {
            Column(
                Modifier.padding(16.dp)
            ) {
                Text(
                    "${vm.torneo.equipoIds.size}",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = NegroFH
                )

                Text(
                    "Equipos inscritos",
                    color = NegroFH
                )
            }
        }

        Card(
            Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = NegroFH
            )
        ) {
            Column(
                Modifier.padding(16.dp)
            ) {
                Text(
                    "${
                        vm.jugadores.count {
                            it.equipoId in vm.torneo.equipoIds &&
                                    vm.habilitado(it.id)
                        }
                    }",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = NaranjaFH
                )

                Text(
                    "Habilitados",
                    color = BlancoFH
                )
            }
        }
    }

    // El resumen del torneo solo tiene sentido para quien tiene pestaña de
    // Torneo (jugador y organizador); médico y árbitro no la necesitan aquí.
    if (vm.rol == Rol.JUGADOR || vm.rol == Rol.ORGANIZADOR) {
        Panel {
            Titulo(
                vm.torneo.nombre,
                vm.torneo.formato.titulo
            )

            Text(
                "${vm.partidos.size} partidos en el fixture"
            )

            OutlinedButton(
                onClick = { abrir(if (vm.rol == Rol.ORGANIZADOR) 2 else 3) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver torneo")
            }

            if (vm.rol == Rol.JUGADOR) {
                TextButton(
                    onClick = { abrir(2) }
                ) {
                    Text("Consultar jugadores por equipo →")
                }
            }
        }
    }
}

// --- JUGADOR: reservar ficha médica y ver el estado de sus solicitudes ---
@Composable
private fun FichasJugador(
    vm: CanchaViewModel,
    avisar: (String) -> Unit
) {
    Titulo(
        "Reserva una ficha",
        "Jugador: ${vm.jugador(vm.jugadorActualId).nombre} · ${vm.torneo.nombre}"
    )

    Estado(
        if (vm.habilitado(vm.jugadorActualId)) "Habilitado para jugar" else "Aún no habilitado",
        vm.habilitado(vm.jugadorActualId)
    )

    var horarioId by rememberSaveable {
        mutableIntStateOf(-1)
    }

    Panel {
        Text(
            "Dra. Ana Rojas",
            fontWeight = FontWeight.Bold
        )

        Text(
            "Revisión presencial · Centro médico de la asociación",
            fontSize = 13.sp
        )

        vm.horarios
            .groupBy { it.fecha }
            .forEach { (fecha, horarios) ->
                Text(
                    fecha,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    horarios.forEach { h ->
                        FilterChip(
                            selected = horarioId == h.id,
                            onClick = { horarioId = h.id },
                            enabled = vm.disponible(h.id),
                            label = { Text(h.hora) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NaranjaFH,
                                selectedLabelColor = NegroFH
                            )
                        )
                    }
                }
            }

        Button(
            onClick = { avisar(vm.reservar(horarioId)) },
            enabled = horarioId != -1 &&
                    vm.disponible(horarioId) &&
                    !vm.habilitado(vm.jugadorActualId),
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

    Titulo("Mis fichas")

    val misFichas = vm.fichas.filter { it.jugadorId == vm.jugadorActualId }

    if (misFichas.isEmpty()) {
        Panel {
            Text("Todavía no hay solicitudes. Elige un horario arriba.")
        }
        return
    }

    misFichas.reversed().forEach { ficha ->
        val horario = vm.horarios.first { it.id == ficha.horarioId }

        Panel {
            Text(
                "Ficha #${ficha.id}",
                fontWeight = FontWeight.Bold
            )

            Text("${horario.fecha} · ${horario.hora}")

            Estado(
                ficha.estado.name,
                ficha.estado in listOf(EstadoFicha.ACEPTADA, EstadoFicha.ATENDIDA, EstadoFicha.APTO)
            )
        }
    }
}

// --- MÉDICO: aceptar/rechazar solicitudes y marcar apto/no apto ---
@Composable
private fun AgendaMedico(
    vm: CanchaViewModel
) {
    Titulo(
        "Solicitudes médicas",
        "Fichas de revisión de todos los jugadores"
    )

    if (vm.fichas.isEmpty()) {
        Panel {
            Text("Todavía no hay solicitudes de los jugadores.")
        }
        return
    }

    vm.fichas.reversed().forEach { ficha ->
        val horario = vm.horarios.first { it.id == ficha.horarioId }

        Panel {
            Text(
                "Ficha #${ficha.id} · ${vm.jugador(ficha.jugadorId).nombre}",
                fontWeight = FontWeight.Bold
            )

            Text("${horario.fecha} · ${horario.hora}")

            Estado(
                ficha.estado.name,
                ficha.estado in listOf(EstadoFicha.ACEPTADA, EstadoFicha.ATENDIDA, EstadoFicha.APTO)
            )

            if (ficha.estado == EstadoFicha.PENDIENTE) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { vm.responder(ficha.id, true) }) {
                        Text("Aceptar")
                    }

                    OutlinedButton(onClick = { vm.responder(ficha.id, false) }) {
                        Text("Rechazar")
                    }
                }
            }

            if (ficha.estado == EstadoFicha.ACEPTADA) {
                Text(
                    "Después de la revisión presencial:",
                    fontSize = 13.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { vm.evaluar(ficha.id, true) }) {
                        Text("Apto")
                    }

                    OutlinedButton(onClick = { vm.evaluar(ficha.id, false) }) {
                        Text("No apto")
                    }
                }
            }
        }
    }
}

// --- Lectura de planteles (jugador: solo consulta, no puede editar) ---
@Composable
private fun Equipos(
    vm: CanchaViewModel
) {
    var equipoId by rememberSaveable {
        mutableIntStateOf(1)
    }

    var soloHabilitados by rememberSaveable {
        mutableStateOf(false)
    }

    Titulo(
        "Planteles",
        "Habilitación para ${vm.torneo.nombre}"
    )

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

        Estado(
            if (equipoId in vm.torneo.equipoIds) "Equipo inscrito" else "No inscrito en este torneo",
            equipoId in vm.torneo.equipoIds
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = soloHabilitados,
                onCheckedChange = { soloHabilitados = it }
            )

            Text("Mostrar solo habilitados")
        }

        val lista = vm.jugadores.filter {
            it.equipoId == equipoId && (!soloHabilitados || vm.habilitado(it.id))
        }

        if (lista.isEmpty()) {
            Text("No hay jugadores habilitados en este plantel.")
        }

        lista.forEach { jugador ->
            HorizontalDivider(color = GrisClaroFH)

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

// --- ORGANIZADOR: alta y baja de equipos del padrón general ---
@Composable
private fun EquiposAdmin(
    vm: CanchaViewModel,
    avisar: (String) -> Unit
) {
    var nombreNuevo by rememberSaveable { mutableStateOf("") }
    var siglaNueva by rememberSaveable { mutableStateOf("") }

    Titulo(
        "Equipos",
        "Agrega o elimina equipos del padrón general"
    )

    Panel {
        Text("Agregar equipo", fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = nombreNuevo,
            onValueChange = { nombreNuevo = it },
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = siglaNueva,
            onValueChange = { siglaNueva = it },
            label = { Text("Sigla") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (nombreNuevo.isBlank() || siglaNueva.isBlank()) {
                    avisar("Completa nombre y sigla")
                } else {
                    vm.agregarEquipo(nombreNuevo.trim(), siglaNueva.trim())
                    nombreNuevo = ""
                    siglaNueva = ""
                    avisar("Equipo agregado")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Agregar equipo")
        }
    }

    if (vm.equipos.isEmpty()) {
        Panel {
            Text("No hay equipos registrados.")
        }
        return
    }

    var equipoId by rememberSaveable { mutableIntStateOf(vm.equipos.first().id) }
    if (vm.equipos.none { it.id == equipoId }) {
        equipoId = vm.equipos.first().id
    }

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

    val equipoActual = vm.equipo(equipoId)

    Panel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Titulo(equipoActual.nombre)

            TextButton(
                onClick = {
                    vm.eliminarEquipo(equipoActual.id)
                    avisar("Equipo eliminado")
                }
            ) {
                Text("Eliminar", color = RojoFH)
            }
        }

        Estado(
            if (equipoId in vm.torneo.equipoIds) "Inscrito en ${vm.torneo.nombre}" else "No inscrito en este torneo",
            equipoId in vm.torneo.equipoIds
        )

        vm.jugadores.filter { it.equipoId == equipoId }.forEach { jugador ->
            HorizontalDivider(color = GrisClaroFH)

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
        "Para elegir qué equipos juegan este torneo y el formato, entra a Torneo → Configurar.",
        fontSize = 12.sp,
        color = GrisFH
    )
}

// --- ÁRBITRO: cargar resultado y registrar tarjetas por partido ---
@Composable
private fun PartidosArbitro(
    vm: CanchaViewModel,
    avisar: (String) -> Unit
) {
    var resultado by remember { mutableStateOf<Partido?>(null) }
    var tarjetaPartido by remember { mutableStateOf<Partido?>(null) }

    Titulo(
        "Partidos",
        "${vm.torneo.nombre} · resultados y tarjetas"
    )

    if (vm.partidos.isEmpty()) {
        Panel {
            Text("Todavía no hay partidos programados por el organizador.")
        }
        return
    }

    vm.partidos.groupBy { it.ronda }.forEach { (ronda, partidos) ->
        Titulo(if (vm.torneo.formato == Formato.LIGA) "Fecha $ronda" else "Ronda $ronda")

        partidos.forEach { partido ->
            Panel {
                Text(
                    "${vm.equipo(partido.localId).nombre}  vs  ${partido.visitanteId?.let { vm.equipo(it).nombre } ?: "Pase libre"}",
                    fontWeight = FontWeight.Bold
                )

                when {
                    partido.visitanteId == null -> Estado("Clasifica automáticamente")
                    partido.finalizado() -> Text(
                        "${partido.golesLocal} — ${partido.golesVisitante}",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = NaranjaFH
                    )
                    else -> Estado("Pendiente", false)
                }

                val tarjetasPartido = vm.tarjetasDe(partido.id)
                tarjetasPartido.forEach { t ->
                    Text(
                        "${if (t.tipo == TipoTarjeta.ROJA) "🟥" else "🟨"} ${vm.jugador(t.jugadorId).nombre} · min ${t.minuto}",
                        fontSize = 12.sp
                    )
                }

                if (partido.visitanteId != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { resultado = partido }) {
                            Text(if (partido.finalizado()) "Modificar resultado" else "Cargar resultado")
                        }

                        OutlinedButton(onClick = { tarjetaPartido = partido }) {
                            Text("Tarjeta")
                        }
                    }
                }
            }
        }
    }

    resultado?.let { p ->
        ResultadoDialogo(
            vm = vm,
            partido = p,
            cerrar = { resultado = null },
            alGuardar = { gl, gv, penal ->
                vm.registrarResultado(p.id, gl, gv, penal)
                avisar("Resultado guardado")
            }
        )
    }

    tarjetaPartido?.let { p ->
        TarjetaDialogo(
            vm = vm,
            partido = p,
            cerrar = { tarjetaPartido = null },
            alGuardar = { jugadorId, tipo, minuto ->
                vm.agregarTarjeta(p.id, jugadorId, tipo, minuto)
                avisar("Tarjeta registrada")
            }
        )
    }
}

// --- ORGANIZADOR: formato del torneo, fixture y tabla; también visible
// (en modo lectura) para el jugador ---
@Composable
private fun Torneos(
    vm: CanchaViewModel,
    avisar: (String) -> Unit
) {
    var editando by rememberSaveable {
        mutableStateOf(false)
    }

    var resultado by remember {
        mutableStateOf<Partido?>(null)
    }

    if (editando) {
        Configurar(
            vm,
            cerrar = { editando = false },
            avisar = avisar
        )
        return
    }

    Titulo(
        vm.torneo.nombre,
        vm.torneo.formato.titulo
    )

    Panel {
        Text(
            "${vm.torneo.equipoIds.size} equipos · ${vm.partidos.size} partidos",
            fontWeight = FontWeight.Bold
        )

        Text(
            vm.torneo.equipoIds.joinToString(" · ") { vm.equipo(it).sigla },
            fontSize = 13.sp
        )

        if (vm.rol == Rol.ORGANIZADOR && vm.partidos.isEmpty()) {
            OutlinedButton(
                onClick = { editando = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Configurar torneo y equipos")
            }

            Button(
                onClick = { avisar(vm.generar(true)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sortear y generar fixture")
            }

            TextButton(
                onClick = { avisar(vm.generar(false)) }
            ) {
                Text("Generar con el orden elegido")
            }
        }

        if (vm.partidos.isEmpty()) {
            Text(
                "El organizador debe generar los partidos.",
                fontSize = 13.sp
            )
        }

        if (vm.rol == Rol.ORGANIZADOR && vm.torneo.formato == Formato.ELIMINACION && vm.partidos.isNotEmpty() && vm.campeon() == null) {
            Button(
                onClick = { avisar(vm.avanzar()) }
            ) {
                Text("Crear siguiente ronda")
            }
        }

        vm.campeon()?.let {
            Text(
                "★ $it",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = NaranjaFH
            )
        }
    }

    if (vm.torneo.formato == Formato.LIGA) {
        Panel {
            Titulo("Tabla de posiciones")

            Text(
                "Equipo           PJ    DG    PTS",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            MotorTorneo.tabla(vm.torneo, vm.partidos).forEachIndexed { i, p ->
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "${i + 1}. ${vm.equipo(p.equipoId).sigla}",
                        Modifier.weight(1f)
                    )
                    Text("${p.jugados}", Modifier.width(40.dp))
                    Text("${p.diferencia}", Modifier.width(40.dp))
                    Text("${p.puntos}", Modifier.width(32.dp), fontWeight = FontWeight.Bold)
                }
            }

            Text(
                "Victoria 3 · Empate 1 · Derrota 0\nDesempate: diferencia de goles y goles a favor.",
                fontSize = 11.sp
            )
        }
    }

    vm.partidos.groupBy { it.ronda }.forEach { (ronda, partidos) ->
        val etiqueta = if (vm.torneo.formato == Formato.LIGA) {
            "Fecha $ronda"
        } else {
            when (partidos.size) {
                1 -> "Final"
                2 -> "Semifinales"
                4 -> "Cuartos de final"
                8 -> "Octavos de final"
                else -> "Ronda $ronda"
            }
        }

        Titulo(etiqueta)

        partidos.forEach { partido ->
            Panel {
                Text(
                    "${vm.equipo(partido.localId).nombre}  vs  ${partido.visitanteId?.let { vm.equipo(it).nombre } ?: "Pase libre"}",
                    fontWeight = FontWeight.Bold
                )

                if (partido.visitanteId == null) {
                    Estado("Clasifica automáticamente")
                } else if (partido.finalizado()) {
                    Text(
                        "${partido.golesLocal} — ${partido.golesVisitante}",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = NaranjaFH
                    )

                    partido.ganadorPenalesId?.let {
                        Text("Penales: ${vm.equipo(it).nombre}", fontSize = 12.sp)
                    }
                } else {
                    Estado("Pendiente", false)
                }

                // Solo el organizador edita resultados desde aquí; el árbitro
                // lo hace desde su propia pestaña de Partidos.
                if (vm.rol == Rol.ORGANIZADOR && partido.visitanteId != null) {
                    OutlinedButton(
                        onClick = { resultado = partido }
                    ) {
                        Text(if (partido.finalizado()) "Modificar resultado" else "Cargar resultado")
                    }
                }
            }
        }
    }

    resultado?.let { p ->
        ResultadoDialogo(
            vm = vm,
            partido = p,
            cerrar = { resultado = null },
            alGuardar = { gl, gv, penal ->
                vm.registrarResultado(p.id, gl, gv, penal)
                avisar("Resultado guardado")
            }
        )
    }
}

@Composable
private fun Configurar(
    vm: CanchaViewModel,
    cerrar: () -> Unit,
    avisar: (String) -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf(vm.torneo.nombre) }
    var formato by rememberSaveable { mutableStateOf(vm.torneo.formato) }
    var ids by rememberSaveable { mutableStateOf(vm.torneo.equipoIds.toList()) }

    Titulo("Configurar torneo", "Organizador")

    Panel {
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre del torneo") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Tipo de torneo:", fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Formato.entries.forEach { f ->
                FilterChip(
                    selected = formato == f,
                    onClick = { formato = f },
                    label = { Text(f.titulo) }
                )
            }
        }

        Text("Equipos participantes:", fontWeight = FontWeight.Bold)

        vm.equipos.forEach { equipo ->
            val seleccionado = equipo.id in ids
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = seleccionado,
                    onCheckedChange = { check ->
                        ids = if (check) {
                            ids + equipo.id
                        } else {
                            ids - equipo.id
                        }
                    }
                )
                Text(equipo.nombre)
            }
        }

        Button(
            onClick = {
                if (ids.isEmpty()) {
                    avisar("Selecciona al menos un equipo")
                } else {
                    vm.torneo = Torneo(nombre, formato, ids.toMutableList())
                    cerrar()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar cambios")
        }
    }
}

@Composable
private fun ResultadoDialogo(
    vm: CanchaViewModel,
    partido: Partido,
    cerrar: () -> Unit,
    alGuardar: (Int, Int, Int?) -> Unit
) {
    var gl by rememberSaveable { mutableStateOf(partido.golesLocal?.toString() ?: "0") }
    var gv by rememberSaveable { mutableStateOf(partido.golesVisitante?.toString() ?: "0") }
    var penalGanador by rememberSaveable { mutableStateOf(partido.ganadorPenalesId) }

    val empatado = gl.toIntOrNull() != null && gl.toIntOrNull() == gv.toIntOrNull()
    // Solo en eliminación directa un empate necesita penales: en liga el
    // empate ya se refleja en la tabla de posiciones tal cual.
    val necesitaPenales = empatado && vm.torneo.formato == Formato.ELIMINACION && partido.visitanteId != null

    AlertDialog(
        onDismissRequest = cerrar,
        title = { Text("Registrar resultado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = gl,
                    onValueChange = { gl = it; penalGanador = null },
                    label = { Text("Goles Local") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = gv,
                    onValueChange = { gv = it; penalGanador = null },
                    label = { Text("Goles Visitante") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                if (necesitaPenales) {
                    Text("Empate: define el ganador por penales", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = penalGanador == partido.localId,
                            onClick = { penalGanador = partido.localId },
                            label = { Text(vm.equipo(partido.localId).sigla) }
                        )
                        FilterChip(
                            selected = penalGanador == partido.visitanteId,
                            onClick = { penalGanador = partido.visitanteId },
                            label = { Text(vm.equipo(partido.visitanteId!!).sigla) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !necesitaPenales || penalGanador != null,
                onClick = {
                    val g1 = gl.toIntOrNull() ?: 0
                    val g2 = gv.toIntOrNull() ?: 0
                    alGuardar(g1, g2, if (g1 == g2) penalGanador else null)
                    cerrar()
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = cerrar) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun TarjetaDialogo(
    vm: CanchaViewModel,
    partido: Partido,
    cerrar: () -> Unit,
    alGuardar: (Int, TipoTarjeta, Int) -> Unit
) {
    val jugadoresPartido = vm.jugadores.filter {
        it.equipoId == partido.localId || it.equipoId == partido.visitanteId
    }

    var jugadorId by rememberSaveable { mutableIntStateOf(jugadoresPartido.firstOrNull()?.id ?: -1) }
    var tipo by rememberSaveable { mutableStateOf(TipoTarjeta.AMARILLA) }
    var minuto by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = cerrar,
        title = { Text("Registrar tarjeta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Jugador", fontWeight = FontWeight.SemiBold)

                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    jugadoresPartido.forEach { jugador ->
                        FilterChip(
                            selected = jugadorId == jugador.id,
                            onClick = { jugadorId = jugador.id },
                            label = { Text(jugador.nombre) }
                        )
                    }
                }

                Text("Tipo", fontWeight = FontWeight.SemiBold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TipoTarjeta.entries.forEach { t ->
                        FilterChip(
                            selected = tipo == t,
                            onClick = { tipo = t },
                            label = { Text(t.titulo) }
                        )
                    }
                }

                OutlinedTextField(
                    value = minuto,
                    onValueChange = { minuto = it },
                    label = { Text("Minuto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = jugadorId != -1,
                onClick = {
                    alGuardar(jugadorId, tipo, minuto.toIntOrNull() ?: 0)
                    cerrar()
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = cerrar) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun LoginPantalla(
    vm: CanchaViewModel,
    avisar: (String) -> Unit
) {
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by rememberSaveable { mutableStateOf("") }

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
                Text(
                    "FUTHUB",
                    color = NaranjaFH,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )

                Text(
                    "Iniciar Sesión",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NegroFH
                )

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

                Button(
                    onClick = {
                        val error = vm.iniciarSesion(correo, clave)
                        if (error != null) {
                            avisar(error)
                        }
                    },
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
