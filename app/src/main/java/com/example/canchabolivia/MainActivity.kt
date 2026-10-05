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

private val Verde = Color(0xFF103E35)
private val Lima = Color(0xFFD5F57B)
private val Fondo = Color(0xFFF4F7F5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme=lightColorScheme(primary=Verde,secondary=Color(0xFF547C3A),background=Fondo,surface=Color.White)) {
                CanchaApp()
            }
        }
    }
}

@Composable
fun CanchaApp(vm: CanchaViewModel = viewModel()) {
    var pagina by rememberSaveable { mutableIntStateOf(0) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val avisar: (String) -> Unit = { mensaje -> scope.launch { snack.showSnackbar(mensaje) }; Unit }
    Scaffold(containerColor=Fondo,snackbarHost={SnackbarHost(snack)},bottomBar={
        NavigationBar(containerColor=Color.White) {
            listOf("Inicio","Fichas","Equipos","Torneo").forEachIndexed { i, titulo ->
                NavigationBarItem(selected=pagina==i,onClick={pagina=i},icon={Text(listOf("◉","＋","▤","☆")[i],fontSize=23.sp)},label={Text(titulo)})
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxWidth().background(Verde).padding(20.dp)) {
                Text("CANCHA / BOLIVIA",color=Lima,fontWeight=FontWeight.Bold,fontSize=13.sp)
                Text(listOf("Todo listo para jugar","Tu revisión, sin filas","Equipos y jugadores","El torneo en tus manos")[pagina],color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Prototipo · datos de ejemplo",color=Color(0xFFCEDDD6),fontSize=12.sp)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Rol.entries.forEach { rol -> FilterChip(selected=vm.rol==rol,onClick={vm.rol=rol},label={Text(rol.titulo)}) }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                when(pagina) {
                    0 -> Inicio(vm) { pagina=it }
                    1 -> Fichas(vm,avisar)
                    2 -> Equipos(vm)
                    3 -> Torneos(vm,avisar)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
@Composable private fun Titulo(texto: String, detalle: String? = null) {
    Column {
        Text(texto,fontSize=21.sp,fontWeight=FontWeight.Bold,color=Verde)
        if(detalle!=null) Text(detalle,color=Color(0xFF62746D),fontSize=13.sp)
    }
}
@Composable private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(9.dp),content=content)
    }
}
@Composable private fun Estado(texto: String, positivo: Boolean = true) {
    Text(texto,modifier=Modifier.background(if(positivo) Color(0xFFE7F2DD) else Color(0xFFFFEAC9),RoundedCornerShape(8.dp)).padding(horizontal=10.dp,vertical=5.dp),
        color=if(positivo) Verde else Color(0xFF85551C),fontSize=12.sp,fontWeight=FontWeight.SemiBold)
}
@Composable private fun Inicio(vm: CanchaViewModel, abrir: (Int)->Unit) {
    Titulo("Hola, ${vm.usuarios.first { it.rol==vm.rol }.nombre}","Copa local · La Paz, Bolivia")
    Panel {
        Text("EL PARTIDO EMPIEZA ANTES DE LA CANCHA",fontSize=11.sp,color=Color(0xFF62746D),fontWeight=FontWeight.Bold)
        Text("Menos filas.\nMás fútbol.",fontSize=32.sp,lineHeight=35.sp,fontWeight=FontWeight.Bold,color=Verde)
        Text("Reserva tu revisión y consulta tu habilitación para el torneo.")
        Button(onClick={abrir(1)},modifier=Modifier.fillMaxWidth()) { Text("Reservar ficha médica") }
    }
    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        Card(Modifier.weight(1f),colors=CardDefaults.cardColors(containerColor=Lima)) {
            Column(Modifier.padding(16.dp)) { Text("${vm.torneo.equipoIds.size}",fontSize=30.sp,fontWeight=FontWeight.Bold); Text("Equipos inscritos") }
        }
        Card(Modifier.weight(1f),colors=CardDefaults.cardColors(containerColor=Color(0xFFE2ECE7))) {
            Column(Modifier.padding(16.dp)) { Text("${vm.jugadores.count { it.equipoId in vm.torneo.equipoIds && vm.habilitado(it.id) }}",fontSize=30.sp,fontWeight=FontWeight.Bold); Text("Habilitados") }
        }
    }
    Panel {
        Titulo("${vm.torneo.nombre}",vm.torneo.formato.titulo)
        Text("${vm.partidos.size} partidos en el fixture")
        OutlinedButton(onClick={abrir(3)},modifier=Modifier.fillMaxWidth()) { Text("Ver torneo") }
        TextButton(onClick={abrir(2)}) { Text("Consultar jugadores por equipo →") }
    }
}
@Composable private fun Fichas(vm: CanchaViewModel, avisar: (String)->Unit) {
    if(vm.rol==Rol.JUGADOR) {
        Titulo("Reserva una ficha","Jugador: ${vm.jugador(vm.jugadorActualId).nombre} · ${vm.torneo.nombre}")
        Estado(if(vm.habilitado(vm.jugadorActualId)) "Habilitado para jugar" else "Aún no habilitado",vm.habilitado(vm.jugadorActualId))
        var horarioId by rememberSaveable { mutableIntStateOf(-1) }
        Panel {
            Text("Dra. Ana Rojas",fontWeight=FontWeight.Bold)
            Text("Revisión presencial · Centro médico de la asociación",fontSize=13.sp)
            vm.horarios.groupBy { it.fecha }.forEach { (fecha, horarios) ->
                Text(fecha,fontWeight=FontWeight.SemiBold)
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    horarios.forEach { h -> FilterChip(selected=horarioId==h.id,onClick={horarioId=h.id},enabled=vm.disponible(h.id),label={Text(h.hora)}) }
                }
            }
            Button(onClick={avisar(vm.reservar(horarioId))},enabled=horarioId!=-1 && vm.disponible(horarioId) && !vm.habilitado(vm.jugadorActualId),modifier=Modifier.fillMaxWidth()) { Text("Solicitar ficha") }
            Text("La reserva queda pendiente hasta que el médico la acepte.",fontSize=12.sp)
        }
    } else if(vm.rol!=Rol.MEDICO) {
        Panel { Titulo("Agenda médica"); Text("Cambia al perfil Jugador para reservar o al perfil Médico para atender solicitudes.") }
        return
    }
    Titulo(if(vm.rol==Rol.MEDICO) "Solicitudes médicas" else "Mis fichas")
    val visibles = vm.fichas.filter { vm.rol==Rol.MEDICO || it.jugadorId==vm.jugadorActualId }
    if(visibles.isEmpty()) Panel { Text("Todavía no hay solicitudes. Reserva una ficha desde el perfil Jugador.") }
    visibles.reversed().forEach { ficha ->
        val horario = vm.horarios.first { it.id==ficha.horarioId }
        Panel {
            Text("Ficha #${ficha.id} · ${vm.jugador(ficha.jugadorId).nombre}",fontWeight=FontWeight.Bold)
            Text("${horario.fecha} · ${horario.hora}")
            Estado(ficha.estado.name, ficha.estado in listOf(EstadoFicha.ACEPTADA,EstadoFicha.ATENDIDA))
            if(vm.rol==Rol.MEDICO && ficha.estado==EstadoFicha.PENDIENTE) {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Button(onClick={vm.responder(ficha,true)}) { Text("Aceptar") }
                    OutlinedButton(onClick={vm.responder(ficha,false)}) { Text("Rechazar") }
                }
            }
            if(vm.rol==Rol.MEDICO && ficha.estado==EstadoFicha.ACEPTADA) {
                Text("Después de la revisión presencial:",fontSize=13.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Button(onClick={vm.evaluar(ficha,true)}) { Text("Apto") }
                    OutlinedButton(onClick={vm.evaluar(ficha,false)}) { Text("No apto") }
                }
            }
        }
    }
}
@Composable private fun Equipos(vm: CanchaViewModel) {
    var equipoId by rememberSaveable { mutableIntStateOf(1) }
    var soloHabilitados by rememberSaveable { mutableStateOf(false) }
    Titulo("Planteles","Habilitación para ${vm.torneo.nombre}")
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        vm.equipos.forEach { equipo -> FilterChip(selected=equipoId==equipo.id,onClick={equipoId=equipo.id},label={Text(equipo.sigla)}) }
    }
    Panel {
        Titulo(vm.equipo(equipoId).nombre)
        Estado(if(equipoId in vm.torneo.equipoIds) "Equipo inscrito" else "No inscrito en este torneo",equipoId in vm.torneo.equipoIds)
        Row(verticalAlignment=Alignment.CenterVertically) {
            Checkbox(checked=soloHabilitados,onCheckedChange={soloHabilitados=it}); Text("Mostrar solo habilitados")
        }
        val lista=vm.jugadores.filter { it.equipoId==equipoId && (!soloHabilitados || vm.habilitado(it.id)) }
        if(lista.isEmpty()) Text("No hay jugadores habilitados en este plantel.")
        lista.forEach { jugador ->
            HorizontalDivider(color=Fondo)
            Text("${jugador.dorsal.toString().padStart(2,'0')}   ${jugador.nombre}",fontWeight=FontWeight.Bold)
            Estado(if(vm.habilitado(jugador.id)) "Habilitado" else "No habilitado",vm.habilitado(jugador.id))
        }
    }
    Text("El árbitro consulta el estado de habilitación. No se muestran diagnósticos médicos.",fontSize=12.sp,color=Color(0xFF62746D))
}
@Composable private fun Torneos(vm: CanchaViewModel, avisar: (String)->Unit) {
    var editando by rememberSaveable { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<Partido?>(null) }
    if(editando) {
        Configurar(vm,{editando=false},avisar)
        return
    }
    Titulo(vm.torneo.nombre,vm.torneo.formato.titulo)
    Panel {
        Text("${vm.torneo.equipoIds.size} equipos · ${vm.partidos.size} partidos",fontWeight=FontWeight.Bold)
        Text(vm.torneo.equipoIds.joinToString(" · ") { vm.equipo(it).sigla },fontSize=13.sp)
        if(vm.rol==Rol.ORGANIZADOR && vm.partidos.isEmpty()) {
            OutlinedButton(onClick={editando=true},modifier=Modifier.fillMaxWidth()) { Text("Configurar torneo y equipos") }
            Button(onClick={avisar(vm.generar(true))},modifier=Modifier.fillMaxWidth()) { Text("Sortear y generar fixture") }
            TextButton(onClick={avisar(vm.generar(false))}) { Text("Generar con el orden elegido") }
        }
        if(vm.partidos.isEmpty()) Text("El organizador debe generar los partidos.",fontSize=13.sp)
        if(vm.rol==Rol.ORGANIZADOR && vm.torneo.formato==Formato.ELIMINACION && vm.partidos.isNotEmpty() && vm.campeon()==null) {
            Button(onClick={avisar(vm.avanzar())}) { Text("Crear siguiente ronda") }
        }
        vm.campeon()?.let { Text("★ $it",fontSize=21.sp,fontWeight=FontWeight.Bold,color=Verde) }
    }
    if(vm.torneo.formato==Formato.LIGA) {
        Panel {
            Titulo("Tabla de posiciones")
            Text("Equipo           PJ    DG    PTS",fontWeight=FontWeight.Bold,fontSize=12.sp)
            MotorTorneo.tabla(vm.torneo,vm.partidos).forEachIndexed { i,p ->
                Row(Modifier.fillMaxWidth()) {
                    Text("${i+1}. ${vm.equipo(p.equipoId).sigla}",Modifier.weight(1f))
                    Text("${p.jugados}",Modifier.width(40.dp)); Text("${p.diferencia}",Modifier.width(40.dp)); Text("${p.puntos}",Modifier.width(32.dp),fontWeight=FontWeight.Bold)
                }
            }
            Text("Victoria 3 · Empate 1 · Derrota 0\nDesempate: diferencia de goles y goles a favor. Si persiste, se requiere una definición adicional.",fontSize=11.sp)
        }
    }
    vm.partidos.groupBy { it.ronda }.forEach { (ronda, partidos) ->
        val etiqueta = if(vm.torneo.formato==Formato.LIGA) "Fecha $ronda" else when(partidos.size) { 1->"Final"; 2->"Semifinales"; 4->"Cuartos de final"; 8->"Octavos de final"; else->"Ronda $ronda" }
        Titulo(etiqueta)
        partidos.forEach { partido ->
            Panel {
                Text("${vm.equipo(partido.localId).nombre}  vs  ${partido.visitanteId?.let { vm.equipo(it).nombre } ?: "Pase libre"}",fontWeight=FontWeight.Bold)
                if(partido.visitanteId==null) Estado("Clasifica automáticamente")
                else if(partido.finalizado()) {
                    Text("${partido.golesLocal} — ${partido.golesVisitante}",fontSize=27.sp,fontWeight=FontWeight.Bold,color=Verde)
                    partido.ganadorPenalesId?.let { Text("Penales: ${vm.equipo(it).nombre}",fontSize=12.sp) }
                } else {
                    Estado("Por jugar",false)
                    if(vm.rol==Rol.ARBITRO) Button(onClick={resultado=partido}) { Text("Registrar resultado") }
                }
                Text("Árbitro: Luis Flores",fontSize=12.sp,color=Color(0xFF62746D))
            }
        }
    }
    resultado?.let { partido -> ResultadoDialogo(vm,partido,{resultado=null},avisar) }
}
@Composable private fun Configurar(vm: CanchaViewModel, cerrar: ()->Unit, avisar: (String)->Unit) {
    var nombre by remember { mutableStateOf(vm.torneo.nombre) }
    var formato by remember { mutableStateOf(vm.torneo.formato) }
    var ids by remember { mutableStateOf(vm.torneo.equipoIds) }
    Titulo("Configurar torneo","Selecciona equipos y ajusta su orden antes del sorteo.")
    OutlinedTextField(value=nombre,onValueChange={nombre=it},label={Text("Nombre")},singleLine=true,modifier=Modifier.fillMaxWidth())
    Formato.entries.forEach { f -> Row(verticalAlignment=Alignment.CenterVertically) {
        RadioButton(selected=formato==f,onClick={formato=f}); Text(f.titulo)
    } }
    Panel {
        vm.equipos.forEach { equipo -> Row(verticalAlignment=Alignment.CenterVertically) {
            Checkbox(checked=equipo.id in ids,onCheckedChange={seleccionado -> ids=if(seleccionado) ids+equipo.id else ids-equipo.id})
            Text(equipo.nombre)
        } }
    }
    Panel {
        Text("Orden manual",fontWeight=FontWeight.Bold)
        Text("En eliminatorias se emparejan los equipos consecutivos; si faltan plazas, los primeros reciben pase libre.",fontSize=12.sp)
        ids.forEachIndexed { indice,id -> Row(verticalAlignment=Alignment.CenterVertically) {
            Text("${indice+1}. ${vm.equipo(id).sigla}",Modifier.weight(1f))
            TextButton(enabled=indice>0,onClick={ids=ids.toMutableList().apply { val anterior=this[indice-1]; this[indice-1]=id; this[indice]=anterior }}) { Text("Subir ↑") }
        } }
    }
    Button(enabled=nombre.isNotBlank() && ids.size>=2,onClick={avisar(vm.configurar(nombre,formato,ids)); cerrar()},modifier=Modifier.fillMaxWidth()) { Text("Guardar configuración") }
    TextButton(onClick=cerrar) { Text("Cancelar") }
}
@Composable private fun ResultadoDialogo(vm: CanchaViewModel, partido: Partido, cerrar: ()->Unit, avisar: (String)->Unit) {
    var local by remember { mutableStateOf("") }; var visita by remember { mutableStateOf("") }
    var penales by remember { mutableStateOf<Int?>(null) }
    val a=local.toIntOrNull(); val b=visita.toIntOrNull()
    val requierePenales=vm.torneo.formato==Formato.ELIMINACION && a!=null && a==b
    AlertDialog(onDismissRequest=cerrar,title={Text("Confirmar marcador")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("${vm.equipo(partido.localId).nombre} vs ${vm.equipo(partido.visitanteId!!).nombre}")
            OutlinedTextField(value=local,onValueChange={if(it.length<=2 && it.all(Char::isDigit)) local=it},label={Text("Goles local")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
            OutlinedTextField(value=visita,onValueChange={if(it.length<=2 && it.all(Char::isDigit)) visita=it},label={Text("Goles visitante")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
            if(requierePenales) {
                Text("Ganador por penales:")
                listOf(partido.localId,partido.visitanteId).filterNotNull().forEach { id ->
                    Row(verticalAlignment=Alignment.CenterVertically) { RadioButton(selected=penales==id,onClick={penales=id}); Text(vm.equipo(id).sigla) }
                }
            }
            Text("Al confirmar, el resultado queda bloqueado en esta demostración.",fontSize=12.sp)
        }
    },confirmButton={TextButton(enabled=a!=null && b!=null && (!requierePenales || penales!=null),onClick={
        avisar(vm.resultado(partido.id,a!!,b!!,if(requierePenales) penales else null)); cerrar()
    }) { Text("Confirmar") }},dismissButton={TextButton(onClick=cerrar) { Text("Cancelar") }})
}
