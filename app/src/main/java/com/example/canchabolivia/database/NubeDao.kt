package com.example.canchabolivia.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

// Copia en el teléfono lo que llegó de SQLite Cloud (cargar_datos).
// La nube manda: se borra lo local y se escribe lo de la nube.
@Dao
abstract class NubeDao {

    @Query("SELECT * FROM torneo WHERE id = 1")
    protected abstract suspend fun torneoActual(): TorneoEntity?

    // Borrar los equipos se lleva por CASCADE jugadores, inscritos, partidos,
    // tarjetas y fichas: la base queda lista para recibir la copia.
    @Query("DELETE FROM equipos")
    protected abstract suspend fun borrarEquipos()

    @Query("DELETE FROM torneo_equipos")
    protected abstract suspend fun borrarInscritos()

    @Insert protected abstract suspend fun insertarEquipos(equipos: List<EquipoEntity>)
    @Insert protected abstract suspend fun insertarJugadores(jugadores: List<JugadorEntity>)
    @Insert protected abstract suspend fun insertarFichas(fichas: List<FichaEntity>)
    @Insert protected abstract suspend fun insertarPartidos(partidos: List<PartidoEntity>)
    @Insert protected abstract suspend fun insertarTarjetas(tarjetas: List<TarjetaEntity>)
    @Insert protected abstract suspend fun insertarInscritos(inscritos: List<TorneoEquipoEntity>)
    @Upsert protected abstract suspend fun guardarTorneo(torneo: TorneoEntity)

    // En la nube no hay claves foráneas: se descarta lo que apunte a un equipo,
    // jugador o partido que no existe, para que Room no rechace toda la copia.
    @Transaction
    open suspend fun importar(
        equipos: List<EquipoEntity>,
        jugadores: List<JugadorEntity>,
        fichas: List<FichaEntity>,
        torneo: TorneoEntity?,
        equipoIds: List<Int>,
        partidos: List<PartidoEntity>,
        tarjetas: List<TarjetaEntity>
    ) {
        val idsEquipos = equipos.map { it.id }.toSet()
        val jugadoresOk = jugadores.filter { it.equipoId in idsEquipos }
        val idsJugadores = jugadoresOk.map { it.id }.toSet()
        val partidosOk = partidos.filter {
            it.localId in idsEquipos && (it.visitanteId == null || it.visitanteId in idsEquipos)
        }
        val idsPartidos = partidosOk.map { it.id }.toSet()

        borrarEquipos()
        insertarEquipos(equipos)
        insertarJugadores(jugadoresOk)
        insertarFichas(fichas.filter { it.jugadorId in idsJugadores })
        insertarPartidos(partidosOk)
        insertarTarjetas(tarjetas.filter { it.partidoId in idsPartidos && it.jugadorId in idsJugadores })

        // La nube no sabe si el torneo se terminó: eso se conserva del teléfono.
        if (torneo != null) {
            val actual = torneoActual()
            guardarTorneo(torneo.copy(finalizado = actual?.finalizado ?: false, campeon = actual?.campeon))
            borrarInscritos()
            insertarInscritos(
                equipoIds.filter { it in idsEquipos }
                    .mapIndexed { i, id -> TorneoEquipoEntity(torneo.id, id, i) }
            )
        }
    }
}
