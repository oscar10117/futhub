-- Vistas "legibles" para revisar los datos en SQLite Cloud Studio:
-- muestran nombres de equipos y jugadores en lugar de solo ids.
-- Son solo para mirar: la app sigue usando las tablas normales.
-- Requiere haber ejecutado antes 03_agregar_grupos.sql.

CREATE VIEW IF NOT EXISTS vista_partidos AS
SELECT p.id,
       p.ronda,
       CASE WHEN p.grupo IS NULL THEN NULL ELSE 'Grupo ' || char(65 + p.grupo) END AS grupo,
       l.nombre AS local,
       COALESCE(v.nombre, 'Pase libre') AS visitante,
       p.goles_local,
       p.goles_visitante,
       g.nombre AS ganador_penales,
       CASE p.cancelado WHEN 1 THEN 'Sí' ELSE 'No' END AS cancelado
FROM partidos p
LEFT JOIN equipos l ON l.id = p.local_id
LEFT JOIN equipos v ON v.id = p.visitante_id
LEFT JOIN equipos g ON g.id = p.ganador_penales_id;

CREATE VIEW IF NOT EXISTS vista_torneo_equipos AS
SELECT t.nombre AS torneo, t.formato, e.id AS equipo_id, e.nombre AS equipo, e.sigla
FROM torneo_equipos te
JOIN equipos e ON e.id = te.equipo_id
CROSS JOIN torneo t;

CREATE VIEW IF NOT EXISTS vista_jugadores AS
SELECT j.id, j.nombre AS jugador, j.dorsal, e.nombre AS equipo
FROM jugadores j
LEFT JOIN equipos e ON e.id = j.equipo_id;

CREATE VIEW IF NOT EXISTS vista_fichas AS
SELECT f.id, j.nombre AS jugador, e.nombre AS equipo, f.horario_id, f.estado
FROM fichas f
LEFT JOIN jugadores j ON j.id = f.jugador_id
LEFT JOIN equipos e ON e.id = j.equipo_id;

CREATE VIEW IF NOT EXISTS vista_tarjetas AS
SELECT t.id,
       l.sigla || ' vs ' || COALESCE(v.sigla, '-') AS partido,
       j.nombre AS jugador,
       e.nombre AS equipo,
       t.tipo,
       t.minuto
FROM tarjetas t
LEFT JOIN partidos p ON p.id = t.partido_id
LEFT JOIN equipos l ON l.id = p.local_id
LEFT JOIN equipos v ON v.id = p.visitante_id
LEFT JOIN jugadores j ON j.id = t.jugador_id
LEFT JOIN equipos e ON e.id = j.equipo_id;
