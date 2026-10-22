# Base de datos de FutHub (Room)

## ¿Qué hace?

FutHub guarda sus datos en una base de datos **dentro del celular** (archivo `futhub.db`,
hecha con Room). Antes todo vivía en memoria y se perdía al cerrar la app; ahora
**todo lo que se hace en la app queda guardado** y aparece igual al volver a abrirla.

## ¿Qué se guarda?

| Tabla | Qué guarda | Quién lo usa |
|---|---|---|
| `equipos` | Nombre y sigla de cada equipo | Organizador (agregar / eliminar) |
| `jugadores` | Nombre, dorsal y equipo de cada jugador | Todos |
| `torneo` | Nombre del torneo y formato (Liga / Eliminación directa) | Organizador |
| `torneo_equipos` | Qué equipos están inscritos en el torneo y en qué orden | Organizador |
| `partidos` | Fixture, rondas, goles, ganador por penales y si está cancelado | Organizador y árbitro |
| `tarjetas` | Tarjetas amarillas y rojas con el minuto | Árbitro |
| `fichas` | Reservas médicas y su estado (pendiente, aceptada, apto, no apto...) | Jugador y médico |
| `clima` | Última lectura del clima, para verla sin internet | Todos |

**Ejemplos de lo que ahora se mantiene al cerrar la app:**
- Un equipo nuevo que agregó el organizador (o uno que borró).
- El fixture generado y los resultados cargados.
- Las tarjetas que puso el árbitro.
- La reserva del jugador y si el médico lo dejó **apto** para jugar.
- Los cambios al nombre, formato o equipos inscritos del torneo.

**Lo que NO se guarda (a propósito):**
- Los usuarios y la sesión: son 4 cuentas fijas, hay que iniciar sesión al abrir la app.
- Los horarios del médico: son fijos en el código.

> La base es **local**: cada celular tiene sus propios datos. Lo que se haga en un
> celular no aparece en otro.

## Primera vez que se abre la app

La base se crea con datos de ejemplo para que no arranque vacía:
- 6 equipos (Bolívar, The Strongest, Wilstermann, Oriente Petrolero, Nacional Potosí, Aurora),
  cada uno con 2 jugadores.
- El torneo **Copa La Paz** (Liga) con Bolívar, The Strongest, Wilstermann y Oriente inscritos.

## Relaciones entre tablas

```
equipos ──< jugadores ──< fichas
   │            └───────< tarjetas
   ├──< partidos ─────────< tarjetas
   └──< torneo_equipos >── torneo
```

Las tablas están enlazadas con **claves foráneas con borrado en cascada (CASCADE)**.
Por eso, al **borrar un equipo**, la base borra sola todo lo que dependía de él:
sus jugadores, su inscripción en el torneo, sus partidos, las tarjetas de esos
partidos y las fichas médicas de sus jugadores. No quedan datos "huérfanos".

## Estructura del código

Misma organización por capas que el proyecto TresApis:

```
database/     Entity (tablas), DAO (consultas) y FutHubDatabase
  ├─ EquipoEntity, JugadorEntity, TorneoEntity, TorneoEquipoEntity,
  │  PartidoEntity, TarjetaEntity, FichaEntity, ClimaEntity
  ├─ EquipoDao, TorneoDao, PartidoDao, FichaDao, ClimaDao
  ├─ DatosIniciales     datos de ejemplo de la primera vez
  └─ FutHubDatabase     versión 3 + migraciones
mapper/       Convierte Entity <-> modelo de la app (EquipoMapper, TorneoMapper, FichaMapper)
repository/   Único punto de acceso a la base (EquiposRepository, TorneoRepository, FichasRepository)
CanchaViewModel   Escribe en los repositorios y escucha los cambios (Flow) para la UI
```

**Flujo:** la pantalla llama al `CanchaViewModel` → este guarda el cambio en el
repositorio → Room actualiza la tabla → Room avisa (Flow) → la pantalla se actualiza sola.

## Versiones y migraciones

| Versión | Qué agregó |
|---|---|
| 1 | `clima` |
| 2 | `equipos`, `jugadores` |
| 3 | `torneo`, `torneo_equipos`, `partidos`, `tarjetas`, `fichas` |

Las migraciones `1 → 2` y `2 → 3` crean las tablas nuevas **sin borrar lo que ya
estaba guardado**, así que actualizar la app no hace perder datos.

## Cómo probarlo

1. Entrar como organizador (`admin@futhub.com` / `123`), agregar un equipo y generar el fixture.
2. Cargar un resultado y, como árbitro, poner una tarjeta.
3. Cerrar la app por completo y volver a abrirla: todo sigue ahí.
