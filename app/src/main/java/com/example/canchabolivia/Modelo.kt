package com.example.canchabolivia

import android.content.Context
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.time.LocalDate

enum class Rol(val titulo: String) {
    JUGADOR("Jugador"),
    MEDICO("Médico"),
    ARBITRO("Árbitro"),
    ORGANIZADOR("Organizador")
}

enum class Formato(val titulo: String) {
    LIGA("Todos contra todos"),
    ELIMINACION("Eliminación directa")
}

enum class EstadoFicha {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA,
    ATENDIDA,
    APTO,
    NO_APTO
}

data class Usuario(
    val id: Int,
    val nombre: String,
    val correo: String,
    val clave: String,
    val rol: Rol
)

data class Equipo(
    val id: Int,
    val nombre: String,
    val sigla: String
)

data class Jugador(
    val id: Int,
    val nombre: String,
    val equipoId: Int,
    val dorsal: Int
)

data class HorarioMedico(
    val id: Int,
    val medicoId: Int,
    val fecha: String,
    val hora: String
)

data class FichaMedica(
    val id: Int,
    val jugadorId: Int,
    val horarioId: Int,
    var estado: EstadoFicha = EstadoFicha.PENDIENTE
)

data class Torneo(
    var nombre: String,
    var formato: Formato,
    var equipoIds: MutableList<Int>
)

data class Partido(
    val id: Int,
    val ronda: Int,
    val localId: Int,
    val visitanteId: Int?,
    var golesLocal: Int? = null,
    var golesVisitante: Int? = null,
    var ganadorPenalesId: Int? = null
) {
    fun finalizado(): Boolean = golesLocal != null && golesVisitante != null
}

data class Posicion(
    val equipoId: Int,
    var jugados: Int = 0,
    var ganados: Int = 0,
    var empatados: Int = 0,
    var perdidos: Int = 0,
    var favor: Int = 0,
    var contra: Int = 0,
    var diferencia: Int = 0,
    var puntos: Int = 0
)

object MotorTorneo {
    fun tabla(torneo: Torneo, partidos: List<Partido>): List<Posicion> {
        val mapa = torneo.equipoIds.associateWith { Posicion(it) }.toMutableMap()
        for (p in partidos.filter { it.finalizado() && it.visitanteId != null }) {
            val loc = mapa[p.localId] ?: continue
            val vis = mapa[p.visitanteId] ?: continue
            val gl = p.golesLocal!!
            val gv = p.golesVisitante!!

            loc.jugados++
            vis.jugados++
            loc.favor += gl
            loc.contra += gv
            vis.favor += gv
            vis.contra += gl
            loc.diferencia = loc.favor - loc.contra
            vis.diferencia = vis.favor - vis.contra

            when {
                gl > gv -> {
                    loc.ganados++
                    loc.puntos += 3
                    vis.perdidos++
                }
                gv > gl -> {
                    vis.ganados++
                    vis.puntos += 3
                    loc.perdidos++
                }
                else -> {
                    loc.empatados++
                    loc.puntos += 1
                    vis.empatados++
                    vis.puntos += 1
                }
            }
        }
        return mapa.values.sortedWith(
            compareByDescending<Posicion> { it.puntos }
                .thenByDescending { it.diferencia }
                .thenByDescending { it.favor }
        )
    }
}

fun realizarLogin(
    context: Context,
    emailInput: String,
    passwordInput: String,
    onSuccess: (String) -> Unit
) {
    val url = "http://10.0.2.2/canchabolivia/login.php"
    val queue = Volley.newRequestQueue(context)
    val stringRequest = object : StringRequest(
        Request.Method.POST, url,
        { response ->
            try {
                val json = JSONObject(response)
                val status = json.getString("status")
                val message = json.getString("message")

                if (status == "success") {
                    val usuarioObj = json.getJSONObject("usuario")
                    val rol = usuarioObj.getString("rol")
                    val nombre = usuarioObj.getString("nombre")
                    Toast.makeText(context, "Bienvenido $nombre", Toast.LENGTH_SHORT).show()
                    onSuccess(rol)
                } else {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al procesar la respuesta", Toast.LENGTH_SHORT).show()
            }
        },
        { error ->
            Toast.makeText(context, "Error de red: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    ) {
        override fun getParams(): MutableMap<String, String> {
            val params = HashMap<String, String>()
            params["email"] = emailInput
            params["password"] = passwordInput
            return params
        }
    }
    queue.add(stringRequest)
}