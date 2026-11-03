// guardar_tarjeta: registra una tarjeta (árbitro).
// Recibe: { id, partido_id, jugador_id, tipo, minuto }
const t = request.data;
if (t.id == null) throw new Error('Falta id');

await connection.sql`
    INSERT OR REPLACE INTO tarjetas (id, partido_id, jugador_id, tipo, minuto)
    VALUES (${t.id}, ${t.partido_id}, ${t.jugador_id}, ${t.tipo}, ${t.minuto})`;

return { data: { ok: true } };
