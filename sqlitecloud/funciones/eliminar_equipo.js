// eliminar_equipo: borra el equipo y todo lo que depende de él
// (tarjetas y fichas de sus jugadores, sus partidos, su inscripción y sus jugadores).
// Recibe: { equipo_id }
const { equipo_id } = request.data;
if (equipo_id == null) throw new Error('Falta equipo_id');

await connection.sql`DELETE FROM tarjetas WHERE jugador_id IN (SELECT id FROM jugadores WHERE equipo_id = ${equipo_id})`;
await connection.sql`DELETE FROM fichas   WHERE jugador_id IN (SELECT id FROM jugadores WHERE equipo_id = ${equipo_id})`;
await connection.sql`DELETE FROM partidos WHERE local_id = ${equipo_id} OR visitante_id = ${equipo_id}`;
await connection.sql`DELETE FROM torneo_equipos WHERE equipo_id = ${equipo_id}`;
await connection.sql`DELETE FROM jugadores WHERE equipo_id = ${equipo_id}`;
await connection.sql`DELETE FROM equipos WHERE id = ${equipo_id}`;

return { data: { ok: true } };
