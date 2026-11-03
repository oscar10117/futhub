// cargar_datos: todo lo que la app necesita al abrirse, en una sola llamada.
// Recibe: nada   Devuelve: { equipos, jugadores, fichas, torneo, partidos, tarjetas }
const filas = (resultado) => resultado.map((fila) => ({ ...fila }));

const equipos   = await connection.sql`SELECT id, nombre, sigla FROM equipos ORDER BY id`;
const jugadores = await connection.sql`SELECT id, nombre, equipo_id, dorsal FROM jugadores ORDER BY id`;
const fichas    = await connection.sql`SELECT id, jugador_id, horario_id, estado FROM fichas ORDER BY id`;
const partidos  = await connection.sql`
    SELECT id, ronda, local_id, visitante_id, goles_local, goles_visitante, ganador_penales_id, cancelado, grupo
    FROM partidos ORDER BY id`;
const tarjetas  = await connection.sql`SELECT id, partido_id, jugador_id, tipo, minuto FROM tarjetas ORDER BY id`;
const torneo    = await connection.sql`SELECT nombre, formato FROM torneo WHERE id = 1`;
const inscritos = await connection.sql`SELECT equipo_id FROM torneo_equipos ORDER BY equipo_id`;

return {
    data: {
        equipos: filas(equipos),
        jugadores: filas(jugadores),
        fichas: filas(fichas),
        partidos: filas(partidos),
        tarjetas: filas(tarjetas),
        torneo: torneo.length
            ? { ...torneo[0], equipo_ids: inscritos.map((fila) => fila.equipo_id) }
            : null
    }
};
