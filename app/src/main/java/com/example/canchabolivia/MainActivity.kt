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

    var pagina by rememberSaveable {
        mutableIntStateOf(0)
    }

    Scaffold(
        containerColor = FondoFH,
        snackbarHost = {
            SnackbarHost(snack)
        },
        bottomBar = {
            NavigationBar(
                containerColor = NegroFH
            ) {
                listOf(
                    "Inicio",
                    "Fichas",
                    "Equipos",
                    "Torneo"
                ).forEachIndexed { i, titulo ->
                    NavigationBarItem(
                        selected = pagina == i,
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
                            Text(
                                listOf(
                                    "◉",
                                    "＋",
                                    "▤",
                                    "☆"
                                )[i],
                                fontSize = 23.sp
                            )
                        },
                        label = {
                            Text(titulo)
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
                    Text(
                        "FUTHUB",
                        color = NaranjaFH,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    TextButton(
                        onClick = { vm.cerrarSesion() }
                    ) {
                        Text("Cerrar sesión", color = BlancoFH, fontSize = 12.sp)
                    }
                }

                Text(
                    listOf(
                        "Todo listo para jugar",
                        "Tu revisión, sin filas",
                        "Equipos y jugadores",
                        "El torneo en tus manos"
                    )[pagina],
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

            // SELECTOR DE ROLES (Opcional para pruebas)
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 10.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Rol.entries.forEach { rol ->
                    FilterChip(
                        selected = vm.rol == rol,
                        onClick = {
                            vm.rol = rol
                        },
                        label = {
                            Text(rol.titulo)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = BlancoFH,
                            labelColor = NegroFH,
                            selectedContainerColor = NaranjaFH,
                            selectedLabelColor = NegroFH
                        )
                    )
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (pagina) {
                    0 -> Inicio(vm) {
                        pagina = it
                    }

                    1 -> Fichas(
                        vm,
                        avisar
                    )

                    2 -> Equipos(vm)

                    3 -> Torneos(
                        vm,
                        avisar
                    )
                }

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

@Composable
private fun Inicio(
    vm: CanchaViewModel,
    abrir: (Int) -> Unit
) {
    Titulo(
        "Hola, ${
            vm.usuarios.first {
                it.rol == vm.rol
            }.nombre
        }",
        "Copa local · La Paz, Bolivia"
    )

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

        Text(
            "Reserva tu revisión y consulta tu habilitación para el torneo."
        )

        Button(
            onClick = {
                abrir(1)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Reservar ficha médica",
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

    Panel {
        Titulo(
            vm.torneo.nombre,
            vm.torneo.formato.titulo
        )

        Text(
            "${vm.partidos.size} partidos en el fixture"
        )

        OutlinedButton(
            onClick = {
                abrir(3)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Ver torneo"
            )
        }

        TextButton(
            onClick = {
                abrir(2)
            }
        ) {
            Text(
                "Consultar jugadores por equipo →"
            )
        }
    }
}

@Composable
private fun Fichas(
    vm: CanchaViewModel,
    avisar: (String) -> Unit
) {
    if (vm.rol == Rol.JUGADOR) {
        Titulo(
            "Reserva una ficha",
            "Jugador: ${
                vm.jugador(
                    vm.jugadorActualId
                ).nombre
            } · ${vm.torneo.nombre}"
        )

        Estado(
            if (
                vm.habilitado(
                    vm.jugadorActualId
                )
            )
                "Habilitado para jugar"
            else
                "Aún no habilitado",
            vm.habilitado(
                vm.jugadorActualId
            )
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
                .groupBy {
                    it.fecha
                }
                .forEach { (fecha, horarios) ->
                    Text(
                        fecha,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        Modifier.horizontalScroll(
                            rememberScrollState()
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        horarios.forEach { h ->
                            FilterChip(
                                selected = horarioId == h.id,
                                onClick = {
                                    horarioId = h.id
                                },
                                enabled = vm.disponible(h.id),
                                label = {
                                    Text(h.hora)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NaranjaFH,
                                    selectedLabelColor = NegroFH
                                )
                            )
                        }
                    }
                }

            Button(
                onClick = {
                    avisar(
                        vm.reservar(
                            horarioId
                        )
                    )
                },
                enabled = horarioId != -1 &&
                        vm.disponible(horarioId) &&
                        !vm.habilitado(vm.jugadorActualId),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Solicitar ficha"
                )
            }

            Text(
                "La reserva queda pendiente hasta que el médico la acepte.",
                fontSize = 12.sp,
                color = GrisFH
            )
        }

    } else if (vm.rol != Rol.MEDICO) {
        Panel {
            Titulo(
                "Agenda médica"
            )

            Text(
                "Cambia al perfil Jugador para reservar o al perfil Médico para atender solicitudes."
            )
        }

        return
    }

    Titulo(
        if (vm.rol == Rol.MEDICO)
            "Solicitudes médicas"
        else
            "Mis fichas"
    )

    val visibles = vm.fichas.filter {
        vm.rol == Rol.MEDICO || it.jugadorId == vm.jugadorActualId
    }

    if (visibles.isEmpty()) {
        Panel {
            Text(
                "Todavía no hay solicitudes. Reserva una ficha desde el perfil Jugador."
            )
        }
    }

    visibles
        .reversed()
        .forEach { ficha ->
            val horario = vm.horarios.first {
                it.id == ficha.horarioId
            }

            Panel {
                Text(
                    "Ficha #${ficha.id} · ${
                        vm.jugador(
                            ficha.jugadorId
                        ).nombre
                    }",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "${horario.fecha} · ${horario.hora}"
                )

                Estado(
                    ficha.estado.name,
                    ficha.estado in listOf(
                        EstadoFicha.ACEPTADA,
                        EstadoFicha.ATENDIDA
                    )
                )

                if (vm.rol == Rol.MEDICO && ficha.estado == EstadoFicha.PENDIENTE) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                vm.responder(
                                    ficha,
                                    true
                                )
                            }
                        ) {
                            Text("Aceptar")
                        }

                        OutlinedButton(
                            onClick = {
                                vm.responder(
                                    ficha,
                                    false
                                )
                            }
                        ) {
                            Text("Rechazar")
                        }
                    }
                }

                if (vm.rol == Rol.MEDICO && ficha.estado == EstadoFicha.ACEPTADA) {
                    Text(
                        "Después de la revisión presencial:",
                        fontSize = 13.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                vm.evaluar(
                                    ficha,
                                    true
                                )
                            }
                        ) {
                            Text("Apto")
                        }

                        OutlinedButton(
                            onClick = {
                                vm.evaluar(
                                    ficha,
                                    false
                                )
                            }
                        ) {
                            Text("No apto")
                        }
                    }
                }
            }
        }
}

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
        Modifier.horizontalScroll(
            rememberScrollState()
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        vm.equipos.forEach { equipo ->
            FilterChip(
                selected = equipoId == equipo.id,
                onClick = {
                    equipoId = equipo.id
                },
                label = {
                    Text(
                        equipo.sigla
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NaranjaFH,
                    selectedLabelColor = NegroFH
                )
            )
        }
    }

    Panel {
        Titulo(
            vm.equipo(equipoId).nombre
        )

        Estado(
            if (equipoId in vm.torneo.equipoIds)
                "Equipo inscrito"
            else
                "No inscrito en este torneo",
            equipoId in vm.torneo.equipoIds
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = soloHabilitados,
                onCheckedChange = {
                    soloHabilitados = it
                }
            )

            Text(
                "Mostrar solo habilitados"
            )
        }

        val lista = vm.jugadores.filter {
            it.equipoId == equipoId && (!soloHabilitados || vm.habilitado(it.id))
        }

        if (lista.isEmpty()) {
            Text(
                "No hay jugadores habilitados en este plantel."
            )
        }

        lista.forEach { jugador ->
            HorizontalDivider(
                color = GrisClaroFH
            )

            Text(
                "${jugador.dorsal.toString().padStart(2, '0')}   ${jugador.nombre}",
                fontWeight = FontWeight.Bold
            )

            Estado(
                if (vm.habilitado(jugador.id))
                    "Habilitado"
                else
                    "No habilitado",
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
            vm.torneo.equipoIds.joinToString(" · ") {
                vm.equipo(it).sigla
            },
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

                if ((vm.rol == Rol.ARBITRO || vm.rol == Rol.ORGANIZADOR) && partido.visitanteId != null) {
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
            partido = p,
            cerrar = { resultado = null },
            alGuardar = { gl, gv ->
                p.golesLocal = gl
                p.golesVisitante = gv
                vm.partidos = vm.partidos.toList()
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
    partido: Partido,
    cerrar: () -> Unit,
    alGuardar: (Int, Int) -> Unit
) {
    var gl by rememberSaveable { mutableStateOf(partido.golesLocal?.toString() ?: "0") }
    var gv by rememberSaveable { mutableStateOf(partido.golesVisitante?.toString() ?: "0") }

    AlertDialog(
        onDismissRequest = cerrar,
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
            Button(
                onClick = {
                    val g1 = gl.toIntOrNull() ?: 0
                    val g2 = gv.toIntOrNull() ?: 0
                    alGuardar(g1, g2)
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