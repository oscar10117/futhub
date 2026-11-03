-- Tablas de FutHub en SQLite Cloud.
-- Ejecutar una sola vez en la consola de la base de datos (Studio).
-- Los nombres de columna coinciden con los campos de dto/NubeDto.kt.
-- Los usuarios NO van aquí: el inicio de sesión lo maneja Firebase Authentication.

CREATE TABLE IF NOT EXISTS equipos (
    id      INTEGER PRIMARY KEY,
    nombre  TEXT NOT NULL,
    sigla   TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS jugadores (
    id         INTEGER PRIMARY KEY,
    nombre     TEXT NOT NULL,
    equipo_id  INTEGER NOT NULL,
    dorsal     INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS fichas (
    id          INTEGER PRIMARY KEY,
    jugador_id  INTEGER NOT NULL,
    horario_id  INTEGER NOT NULL,
    estado      TEXT NOT NULL DEFAULT 'PENDIENTE'  -- PENDIENTE, ACEPTADA, RECHAZADA, ATENDIDA, APTO, NO_APTO
);

-- Un solo torneo (id = 1) y la lista de equipos inscritos.
CREATE TABLE IF NOT EXISTS torneo (
    id       INTEGER PRIMARY KEY CHECK (id = 1),
    nombre   TEXT NOT NULL,
    formato  TEXT NOT NULL         -- LIGA o ELIMINACION
);

CREATE TABLE IF NOT EXISTS torneo_equipos (
    equipo_id  INTEGER PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS partidos (
    id                  INTEGER PRIMARY KEY,
    ronda               INTEGER NOT NULL,
    local_id            INTEGER NOT NULL,
    visitante_id        INTEGER,           -- NULL = pase libre
    goles_local         INTEGER,
    goles_visitante     INTEGER,
    ganador_penales_id  INTEGER,
    cancelado           INTEGER NOT NULL DEFAULT 0,  -- 0 = no, 1 = sí
    grupo               INTEGER                      -- formato GRUPOS: 0 = A, 1 = B... NULL = eliminación
);

CREATE TABLE IF NOT EXISTS tarjetas (
    id          INTEGER PRIMARY KEY,
    partido_id  INTEGER NOT NULL,
    jugador_id  INTEGER NOT NULL,
    tipo        TEXT NOT NULL,     -- AMARILLA o ROJA
    minuto      INTEGER NOT NULL
);
