-- Para bases creadas antes del formato "Grupos + eliminación (tipo Mundial)".
-- Ejecutar una sola vez.
ALTER TABLE partidos ADD COLUMN grupo INTEGER;
