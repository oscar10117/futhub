// eliminar_jugador: quita un jugador del plantel junto con sus tarjetas y fichas.
// Recibe: { jugador_id }
const { jugador_id } = request.data;
if (jugador_id == null) throw new Error('Falta jugador_id');

await connection.sql`DELETE FROM tarjetas WHERE jugador_id = ${jugador_id}`;
await connection.sql`DELETE FROM fichas   WHERE jugador_id = ${jugador_id}`;
await connection.sql`DELETE FROM jugadores WHERE id = ${jugador_id}`;

return { data: { ok: true } };
