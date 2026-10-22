package com.example.canchabolivia.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ClimaEntity::class,
        EquipoEntity::class,        // versión 2
        JugadorEntity::class,       // versión 2
        TorneoEntity::class,        // versión 3
        TorneoEquipoEntity::class,  // versión 3
        PartidoEntity::class,       // versión 3
        TarjetaEntity::class,       // versión 3
        FichaEntity::class          // versión 3
    ],
    version = 3,
    exportSchema = false
)
abstract class FutHubDatabase : RoomDatabase() {

    abstract fun climaDao(): ClimaDao
    abstract fun equipoDao(): EquipoDao
    abstract fun torneoDao(): TorneoDao
    abstract fun partidoDao(): PartidoDao
    abstract fun fichaDao(): FichaDao

    companion object {
        @Volatile
        private var instancia: FutHubDatabase? = null

        // Pasa de la versión 1 a la 2 sin perder el clima guardado: crea las tablas
        // nuevas y carga los equipos iniciales. El SQL debe coincidir con las Entity.
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `equipos` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`nombre` TEXT NOT NULL, " +
                        "`sigla` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `jugadores` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`nombre` TEXT NOT NULL, " +
                        "`equipoId` INTEGER NOT NULL, " +
                        "`dorsal` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`equipoId`) REFERENCES `equipos`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_jugadores_equipoId` ON `jugadores` (`equipoId`)")
                DatosIniciales.insertarEquipos(db)
            }
        }

        // Pasa de la 2 a la 3 sin perder equipos ni jugadores: crea las tablas del
        // torneo, partidos, tarjetas y fichas, y carga el torneo de ejemplo.
        private val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `torneo` (" +
                        "`id` INTEGER NOT NULL, " +
                        "`nombre` TEXT NOT NULL, " +
                        "`formato` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `torneo_equipos` (" +
                        "`torneoId` INTEGER NOT NULL, " +
                        "`equipoId` INTEGER NOT NULL, " +
                        "`posicion` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`torneoId`, `equipoId`), " +
                        "FOREIGN KEY(`torneoId`) REFERENCES `torneo`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`equipoId`) REFERENCES `equipos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_torneo_equipos_equipoId` ON `torneo_equipos` (`equipoId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `partidos` (" +
                        "`id` INTEGER NOT NULL, " +
                        "`ronda` INTEGER NOT NULL, " +
                        "`localId` INTEGER NOT NULL, " +
                        "`visitanteId` INTEGER, " +
                        "`golesLocal` INTEGER, " +
                        "`golesVisitante` INTEGER, " +
                        "`ganadorPenalesId` INTEGER, " +
                        "`cancelado` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`localId`) REFERENCES `equipos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`visitanteId`) REFERENCES `equipos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_partidos_localId` ON `partidos` (`localId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_partidos_visitanteId` ON `partidos` (`visitanteId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tarjetas` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`partidoId` INTEGER NOT NULL, " +
                        "`jugadorId` INTEGER NOT NULL, " +
                        "`tipo` TEXT NOT NULL, " +
                        "`minuto` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`partidoId`) REFERENCES `partidos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`jugadorId`) REFERENCES `jugadores`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tarjetas_partidoId` ON `tarjetas` (`partidoId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tarjetas_jugadorId` ON `tarjetas` (`jugadorId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `fichas` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`jugadorId` INTEGER NOT NULL, " +
                        "`horarioId` INTEGER NOT NULL, " +
                        "`estado` TEXT NOT NULL, " +
                        "FOREIGN KEY(`jugadorId`) REFERENCES `jugadores`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fichas_jugadorId` ON `fichas` (`jugadorId`)")
                DatosIniciales.insertarTorneo(db)
            }
        }

        // Instalación nueva: Room crea todas las tablas y aquí se cargan los datos iniciales.
        private val alCrear = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                DatosIniciales.insertar(db)
            }
        }

        fun obtener(context: Context): FutHubDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    FutHubDatabase::class.java,
                    "futhub.db"
                )
                    .addMigrations(MIGRACION_1_2, MIGRACION_2_3)
                    .addCallback(alCrear)
                    .build()
                    .also { instancia = it }
            }
    }
}
