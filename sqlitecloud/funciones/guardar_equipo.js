// guardar_equipo: crea (o actualiza) un equipo junto con sus jugadores.
// Recibe: { equipo: { id, nombre, sigla }, jugadores: [{ id, nombre, equipo_id, dorsal }] }
// [GUIA DOCENTE - EDGE FUNCTION] Ejemplo de escritura remota.
// Toda operacion nueva necesita validacion y permisos reales en servidor.
const { equipo, jugadores = [] } = request.data;
if (!equipo || !equipo.nombre) throw new Error('Falta el equipo');

await connection.sql`
    INSERT OR REPLACE INTO equipos (id, nombre, sigla)
    VALUES (${equipo.id}, ${equipo.nombre}, ${equipo.sigla})`;

for (const j of jugadores) {
    await connection.sql`
        INSERT OR REPLACE INTO jugadores (id, nombre, equipo_id, dorsal)
        VALUES (${j.id}, ${j.nombre}, ${j.equipo_id}, ${j.dorsal})`;
}

return { data: { ok: true } };
