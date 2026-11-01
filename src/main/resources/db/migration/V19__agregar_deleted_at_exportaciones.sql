-- ============================================================
-- MigraciÃ³n V19: Agregar columna deleted_at a exportaciones
-- (necesaria para soft-delete; no existÃ­a en V1)
-- ============================================================

ALTER TABLE exportaciones ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

