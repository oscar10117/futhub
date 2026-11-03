// guardar_torneo: guarda nombre, formato y equipos inscritos del torneo.
// Recibe: { nombre, formato, equipo_ids: [1, 2, ...] }
const { nombre, formato, equipo_ids = [] } = request.data;
if (!nombre || !formato) throw new Error('Faltan nombre o formato');

await connection.sql`INSERT OR REPLACE INTO torneo (id, nombre, formato) VALUES (1, ${nombre}, ${formato})`;
await connection.sql`DELETE FROM torneo_equipos`;
for (const id of equipo_ids) {
    await connection.sql`INSERT INTO torneo_equipos (equipo_id) VALUES (${id})`;
}

return { data: { ok: true } };
