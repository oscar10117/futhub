-- Los mismos datos de ejemplo que tenía CanchaViewModel.
-- Ejecutar una sola vez, después de 01_tablas.sql.

INSERT OR REPLACE INTO equipos (id, nombre, sigla) VALUES
    (1, 'Bolívar',           'BOL'),
    (2, 'The Strongest',     'STR'),
    (4, 'Wilstermann',       'WIL'),
    (5, 'Oriente Petrolero', 'OPE'),
    (7, 'Nacional Potosí',   'NAC'),
    (8, 'Aurora',            'AUR');

INSERT OR REPLACE INTO jugadores (id, nombre, equipo_id, dorsal) VALUES
    (11, 'Juan Pérez', 1, 10), (12, 'Diego BOL', 1, 7),
    (21, 'Juan Pérez', 2, 10), (22, 'Diego STR', 2, 7),
    (41, 'Juan Pérez', 4, 10), (42, 'Diego WIL', 4, 7),
    (51, 'Juan Pérez', 5, 10), (52, 'Diego OPE', 5, 7),
    (71, 'Juan Pérez', 7, 10), (72, 'Diego NAC', 7, 7),
    (81, 'Juan Pérez', 8, 10), (82, 'Diego AUR', 8, 7);

INSERT OR REPLACE INTO torneo (id, nombre, formato) VALUES (1, 'Copa La Paz', 'LIGA');

INSERT OR REPLACE INTO torneo_equipos (equipo_id) VALUES (1), (2), (4), (5);
