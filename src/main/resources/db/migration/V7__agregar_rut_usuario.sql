-- ============================================================
-- MigraciÃƒÂ³n V7: Agregar campo RUT a usuarios
-- ============================================================

-- Agregar columna rut a la tabla usuarios
-- El RUT es opcional (nullable) para permitir usuarios sin RUT chileno
ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS rut VARCHAR(20) UNIQUE;

COMMENT ON COLUMN usuarios.rut IS 'RUT chileno del usuario (opcional, unique si se proporciona)';

-- ÃƒÂndice para bÃƒÂºsqueda por RUT
CREATE INDEX IF NOT EXISTS idx_usuario_rut ON usuarios(rut) WHERE rut IS NOT NULL;


