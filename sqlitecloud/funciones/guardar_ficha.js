// guardar_ficha: crea una ficha médica o actualiza su estado.
// Recibe: { id, jugador_id, horario_id, estado }
const f = request.data;
if (f.id == null) throw new Error('Falta id');

await connection.sql`
    INSERT OR REPLACE INTO fichas (id, jugador_id, horario_id, estado)
    VALUES (${f.id}, ${f.jugador_id}, ${f.horario_id}, ${f.estado ?? 'PENDIENTE'})`;

return { data: { ok: true } };
