package com.example.canchabolivia

// =====================================================
// MODELO.KT
// Aquí están los "moldes" de los datos de la app:
// roles, estados y las clases que guardan la información.
// =====================================================

// Los 4 tipos de usuario. "titulo" es el texto que se muestra en pantalla.
enum class Rol(val titulo: String) {
    JUGADOR("Jugador"),
    MEDICO("Médico"),
    ARBITRO("Árbitro"),
    ORGANIZADOR("Organizador")
}

// Los estados por los que pasa una ficha médica:
// el jugador la pide (PENDIENTE), el médico la acepta o rechaza,
// y después de la revisión la marca como APTO o NO_APTO.
enum class EstadoFicha {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA,
    APTO,
    NO_APTO
}

// Un usuario que puede iniciar sesión.
// jugadorId solo se llena si el usuario es JUGADOR: indica cuál
// de los jugadores de la lista es él (por eso tiene "?" y valor null por defecto).
data class Usuario(
    val id: Int,
    val nombre: String,
    val correo: String,
    val clave: String,
    val rol: Rol,
    val jugadorId: Int? = null
)

// Un equipo de fútbol. "sigla" es la abreviatura de 3 letras (ej. BOL).
data class Equipo(
    val id: Int,
    val nombre: String,
    val sigla: String
)

// Un jugador, que pertenece a un equipo (equipoId) y tiene un número de camiseta (dorsal).
data class Jugador(
    val id: Int,
    val nombre: String,
    val equipoId: Int,
    val dorsal: Int
)

// Un horario disponible del médico para una revisión.
data class HorarioMedico(
    val id: Int,
    val fecha: String,
    val hora: String
)

// Una ficha médica: une a un jugador con un horario y guarda su estado.
// Todos los campos son "val" (no cambian). Para cambiar el estado se crea
// una copia con .copy(), así Compose detecta el cambio y actualiza la pantalla.
data class FichaMedica(
    val id: Int,
    val jugadorId: Int,
    val horarioId: Int,
    val estado: EstadoFicha = EstadoFicha.PENDIENTE
)

// El torneo: nombre y lista de ids de los equipos que participan.
data class Torneo(
    val nombre: String,
    val equipoIds: List<Int>
)

// Un partido entre dos equipos.
// Los goles son null mientras el partido no se haya jugado.
data class Partido(
    val id: Int,
    val localId: Int,
    val visitanteId: Int,
    val golesLocal: Int? = null,
    val golesVisitante: Int? = null
) {
    // Un partido está finalizado cuando ya tiene los dos resultados cargados
    fun finalizado() = golesLocal != null && golesVisitante != null
}

// Una fila de la tabla de posiciones de un equipo.
// jugados = partidos jugados, favor = goles a favor,
// diferencia = goles a favor - goles en contra.
data class Posicion(
    val equipoId: Int,
    val jugados: Int,
    val favor: Int,
    val diferencia: Int,
    val puntos: Int
)