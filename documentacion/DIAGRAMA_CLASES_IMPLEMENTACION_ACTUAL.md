# FutHub / CanchaBolivia — Diagramas de clases de la implementación actual

**Código revisado:** `oscar10117/futhub`, rama `LEANDRO`, commit `0da818529c1d438277bd0cdb14409f9ba6cde24a` (21 de octubre de 2026, hora de Bolivia). **Implementación:** Open-Meteo + Retrofit, DTO, funciones mapper, Room versión 3, ViewModels y repositorios. 

> Estos diagramas describen las clases y relaciones que realmente existen en el código. El diagrama anterior de `modelodiagrama/ModeloDiagrama.kt` es un modelo documental diferente; no equivale al modelo de ejecución actual.

## 1. Diagrama de clases: arquitectura de la aplicación

```mermaid
classDiagram
 direction LR
 class AndroidViewModel {
   <<AndroidX>>
 }
 class CanchaViewModel {
   +usuarioLogueado: Usuario?
   +equipos: List~Equipo~
   +jugadores: List~Jugador~
   +torneo: Torneo
   +partidos: List~Partido~
   +tarjetas: List~Tarjeta~
   +fichas: List~FichaMedica~
   +iniciarSesion(correo, clave): String?
   +agregarEquipo(nombre, sigla)
   +guardarTorneo(nombre, formato, equipoIds)
   +generar(sortear): String
   +registrarResultado(partidoId, golesLocal, golesVisitante, ganadorPenalesId)
   +cancelarPendientes(): String
   +reservar(horarioId): String
   +responder(fichaId, aceptar)
   +evaluar(fichaId, apto)
 }
 class ClimaViewModel {
   +estado: StateFlow~ClimaUiState~
   +actualizar()
 }
 class ClimaUiState {
   +clima: Clima?
   +cargando: Boolean
   +error: String?
 }
 class EquiposRepository {
   +equipos: Flow
   +jugadores: Flow
   +agregar(nombre, sigla)
   +eliminar(equipoId)
 }
 class TorneoRepository {
   +torneo: Flow~Torneo~
   +partidos: Flow
   +tarjetas: Flow
   +guardarTorneo(nombre, formato, equipoIds)
   +reemplazarFixture(partidos)
   +agregarPartidos(partidos)
   +actualizarPartidos(partidos)
   +agregarTarjeta(tarjeta)
 }
 class FichasRepository {
   +fichas: Flow
   +agregar(ficha)
   +actualizar(ficha)
 }
 class ClimaRepository {
   +guardado: Flow~Clima?~
   +actualizar(): Result~Clima~
 }
 class FutHubDatabase {
   <<RoomDatabase>>
   +climaDao(): ClimaDao
   +equipoDao(): EquipoDao
   +torneoDao(): TorneoDao
   +partidoDao(): PartidoDao
   +fichaDao(): FichaDao
   +obtener(context): FutHubDatabase
 }
 class ClimaDao { <<interface>> }
 class EquipoDao { <<abstract>> }
 class TorneoDao { <<abstract>> }
 class PartidoDao { <<abstract>> }
 class FichaDao { <<interface>> }
 class OpenMeteoApi {
   <<interface>>
   +clima(latitud, longitud, variables, zonaHoraria): ClimaResponseDto
 }
 AndroidViewModel <|-- CanchaViewModel
 AndroidViewModel <|-- ClimaViewModel
 CanchaViewModel --> FutHubDatabase
 CanchaViewModel --> EquiposRepository
 CanchaViewModel --> TorneoRepository
 CanchaViewModel --> FichasRepository
 ClimaViewModel --> ClimaUiState
 ClimaViewModel --> ClimaRepository
 ClimaRepository --> OpenMeteoApi
 ClimaRepository --> ClimaDao
 EquiposRepository --> EquipoDao
 TorneoRepository --> TorneoDao
 TorneoRepository --> PartidoDao
 FichasRepository --> FichaDao
 FutHubDatabase --> ClimaDao
 FutHubDatabase --> EquipoDao
 FutHubDatabase --> TorneoDao
 FutHubDatabase --> PartidoDao
 FutHubDatabase --> FichaDao
```

## 2. Diagrama de clases: API, DTO, mapper y caché del clima

```mermaid
classDiagram
 direction LR
 class OpenMeteoApi {
   <<interface>>
   +clima(latitud: Double, longitud: Double, variables: String, zonaHoraria: String): ClimaResponseDto
 }
 class ClimaResponseDto {
   +latitud: Double?
   +longitud: Double?
   +actual: ClimaActualDto?
 }
 class ClimaActualDto {
   +hora: String?
   +temperatura: Double?
   +humedad: Int?
   +viento: Double?
 }
 class ClimaEntity {
   <<Room Entity clima>>
   +id: Int
   +temperatura: Double
   +humedad: Int
   +viento: Double
   +hora: String
 }
 class ClimaDao {
   <<interface>>
   +observar(): Flow~ClimaEntity?~
   +guardar(clima: ClimaEntity)
 }
 class Clima {
   +temperatura: Double
   +humedad: Int
   +viento: Double
   +hora: String
   +desdeCache: Boolean
   +noApto: Boolean
   +aptitud: Aptitud
   +consejo: String
   +horaCorta: String
 }
 class Aptitud {
   <<enumeration>>
   IDEAL
   PRECAUCION
   NO_RECOMENDADO
 }
 class ClimaRepository {
   +guardado: Flow~Clima?~
   +actualizar(): Result~Clima~
 }
 class ClimaViewModel {
   +estado: StateFlow~ClimaUiState~
   +actualizar()
 }
 class ClimaUiState {
   +clima: Clima?
   +cargando: Boolean
   +error: String?
 }
 class FutHubDatabase {
   +climaDao(): ClimaDao
 }
 OpenMeteoApi ..> ClimaResponseDto : devuelve
 ClimaResponseDto --> ClimaActualDto : actual
 ClimaRepository --> OpenMeteoApi
 ClimaRepository --> ClimaDao
 ClimaRepository ..> ClimaResponseDto : aEntity()
 ClimaRepository ..> ClimaEntity : guarda / aModelo()
 ClimaRepository ..> Clima : devuelve
 ClimaDao ..> ClimaEntity : lee / escribe
 FutHubDatabase --> ClimaDao
 ClimaViewModel --> ClimaRepository
 ClimaViewModel --> ClimaUiState
 ClimaUiState --> Clima
 Clima --> Aptitud
```

**Mapper real:** `mapper/ClimaMapper.kt` **no declara una clase** `ClimaMapper`. Declara dos funciones de extensión a nivel de archivo: `ClimaResponseDto.aEntity(): ClimaEntity?` y `ClimaEntity.aModelo(desdeCache: Boolean): Clima`. Por eso se representan mediante flechas etiquetadas, sin inventar una clase. La consulta usa Open-Meteo para La Paz; una lectura válida se guarda en `clima` (fila `id = 1`) y se puede recuperar sin internet.

## 3. Diagrama de clases y relaciones persistidas: Room (v3)

```mermaid
classDiagram
 direction LR
 class EquipoEntity {
   <<equipos>>
   +id: Int PK
   +nombre: String
   +sigla: String
 }
 class JugadorEntity {
   <<jugadores>>
   +id: Int PK
   +nombre: String
   +equipoId: Int FK
   +dorsal: Int
 }
 class TorneoEntity {
   <<torneo>>
   +id: Int PK
   +nombre: String
   +formato: Formato
 }
 class TorneoEquipoEntity {
   <<torneo_equipos>>
   +torneoId: Int PK_FK
   +equipoId: Int PK_FK
   +posicion: Int
 }
 class PartidoEntity {
   <<partidos>>
   +id: Int PK
   +ronda: Int
   +localId: Int FK
   +visitanteId: Int? FK
   +golesLocal: Int?
   +golesVisitante: Int?
   +ganadorPenalesId: Int?
   +cancelado: Boolean
 }
 class TarjetaEntity {
   <<tarjetas>>
   +id: Int PK
   +partidoId: Int FK
   +jugadorId: Int FK
   +tipo: TipoTarjeta
   +minuto: Int
 }
 class FichaEntity {
   <<fichas>>
   +id: Int PK
   +jugadorId: Int FK
   +horarioId: Int
   +estado: EstadoFicha
 }
 class ClimaEntity {
   <<clima>>
   +id: Int PK
   +temperatura: Double
   +humedad: Int
   +viento: Double
   +hora: String
 }
 EquipoEntity "1" --> "0..*" JugadorEntity : equipoId
 EquipoEntity "1" --> "0..*" TorneoEquipoEntity : equipoId
 TorneoEntity "1" --> "0..*" TorneoEquipoEntity : torneoId
 EquipoEntity "1" --> "0..*" PartidoEntity : localId
 EquipoEntity "0..1" --> "0..*" PartidoEntity : visitanteId
 PartidoEntity "1" --> "0..*" TarjetaEntity : partidoId
 JugadorEntity "1" --> "0..*" TarjetaEntity : jugadorId
 JugadorEntity "1" --> "0..*" FichaEntity : jugadorId
```

Todas las relaciones mostradas entre entidades son **claves foráneas declaradas con `onDelete = CASCADE`**. `TorneoEquipoEntity` es la tabla puente y su PK compuesta es `(torneoId, equipoId)`. `TorneoEntity` y `ClimaEntity` usan `id = 1`. `FichaEntity.horarioId` **no** es FK: los horarios son una lista fija en `CanchaViewModel`. `PartidoEntity` **no** tiene `torneoId` (actualmente la app maneja un único torneo).

## 4. Relación entre entidades Room y modelos del dominio

| Archivo de mapeo real | Conversiones declaradas |
|---|---|
| `ClimaMapper.kt` | `ClimaResponseDto → ClimaEntity?`, `ClimaEntity → Clima` |
| `EquipoMapper.kt` | `EquipoEntity → Equipo`, `JugadorEntity → Jugador` |
| `TorneoMapper.kt` | `TorneoEntity → Torneo`, `PartidoEntity ↔ Partido`, `TarjetaEntity ↔ Tarjeta` |
| `FichaMapper.kt` | `FichaEntity ↔ FichaMedica` |

**Otros modelos reales de `Modelo.kt`:** `Usuario`, `Equipo`, `Jugador`, `HorarioMedico`, `FichaMedica`, `Torneo`, `Partido`, `Tarjeta`, `Posicion`, `MotorTorneo`, y enumeraciones `Rol`, `Formato`, `EstadoFicha`, `TipoTarjeta`. El modelo de clima declara `Clima` y `Aptitud`.

**DAOs reales:** `ClimaDao`, `EquipoDao`, `TorneoDao`, `PartidoDao`, `FichaDao`. **Repositorios reales:** `ClimaRepository`, `EquiposRepository`, `TorneoRepository`, `FichasRepository`. **ViewModels reales:** `ClimaViewModel`, `CanchaViewModel`.

**Migraciones existentes:** `1→2` agrega equipos y jugadores, `2→3` agrega torneo, inscripciones, partidos, tarjetas y fichas. El archivo `futhub.db` es una base local de cada dispositivo; no sincroniza entre dispositivos.

**Límite de verificación:** documentación elaborada contrastando el código fuente de la rama `LEANDRO`. No constituye confirmación de compilación o ejecución en emulador.
