// guardar_partidos: reemplaza el fixture completo por la lista que manda la app.
// Se usa al generar el fixture, cargar resultados, cancelar/reprogramar, avanzar de ronda
// y al empezar un torneo nuevo (lista vacía).
// Recibe: { partidos: [{ id, ronda, local_id, visitante_id, goles_local, goles_visitante, ganador_penales_id, cancelado, grupo }] }
const { partidos = [] } = request.data;

await connection.sql`DELETE FROM partidos`;
for (const p of partidos) {
    await connection.sql`
        INSERT INTO partidos (id, ronda, local_id, visitante_id, goles_local, goles_visitante, ganador_penales_id, cancelado, grupo)
        VALUES (${p.id}, ${p.ronda}, ${p.local_id}, ${p.visitante_id ?? null}, ${p.goles_local ?? null},
                ${p.goles_visitante ?? null}, ${p.ganador_penales_id ?? null}, ${p.cancelado ?? 0}, ${p.grupo ?? null})`;
}

// Las tarjetas de partidos que ya no existen (torneo anterior) se borran.
await connection.sql`DELETE FROM tarjetas WHERE partido_id NOT IN (SELECT id FROM partidos)`;

return { data: { ok: true } };
