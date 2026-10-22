package com.example.canchabolivia.database

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.canchabolivia.Formato

// Datos con los que arranca la app la primera vez. Los ids son fijos porque
// el torneo de ejemplo (1, 2, 4, 5) y el jugador actual (11) dependen de ellos.
object DatosIniciales {

    private val equipos = listOf(
        EquipoEntity(1, "Bolívar", "BOL"),
        EquipoEntity(2, "The Strongest", "STR"),
        EquipoEntity(4, "Wilstermann", "WIL"),
        EquipoEntity(5, "Oriente Petrolero", "OPE"),
        EquipoEntity(7, "Nacional Potosí", "NAC"),
        EquipoEntity(8, "Aurora", "AUR")
    )

    private val jugadores = equipos.flatMap { equipo ->
        listOf(
            JugadorEntity(equipo.id * 10 + 1, "Juan Pérez", equipo.id, 10),
            JugadorEntity(equipo.id * 10 + 2, "Diego ${equipo.sigla}", equipo.id, 7)
        )
    }

    private val torneo = TorneoEntity(nombre = "Copa La Paz", formato = Formato.LIGA)
    private val inscritos = listOf(1, 2, 4, 5)

    // Instalación nueva: todo de una vez.
    fun insertar(db: SupportSQLiteDatabase) {
        insertarEquipos(db)
        insertarTorneo(db)
    }

    // SQL directo: se usa en el callback de creación y en las migraciones,
    // donde todavía no hay DAO disponible.
    fun insertarEquipos(db: SupportSQLiteDatabase) {
        equipos.forEach {
            db.execSQL(
                "INSERT INTO equipos (id, nombre, sigla) VALUES (?, ?, ?)",
                arrayOf<Any>(it.id, it.nombre, it.sigla)
            )
        }
        jugadores.forEach {
            db.execSQL(
                "INSERT INTO jugadores (id, nombre, equipoId, dorsal) VALUES (?, ?, ?, ?)",
                arrayOf<Any>(it.id, it.nombre, it.equipoId, it.dorsal)
            )
        }
    }

    fun insertarTorneo(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO torneo (id, nombre, formato) VALUES (?, ?, ?)",
            arrayOf<Any>(torneo.id, torneo.nombre, torneo.formato.name)
        )
        // Solo inscribe los equipos que todavía existen (al migrar, el
        // organizador pudo haber borrado alguno en la versión anterior).
        inscritos.forEachIndexed { posicion, equipoId ->
            db.execSQL(
                "INSERT INTO torneo_equipos (torneoId, equipoId, posicion) " +
                    "SELECT ?, id, ? FROM equipos WHERE id = ?",
                arrayOf<Any>(torneo.id, posicion, equipoId)
            )
        }
    }
}
