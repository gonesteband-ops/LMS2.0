-- =============================================================
-- Migration: V22 - Agrega archivos adjuntos a contenidos_bpo
-- =============================================================
ALTER TABLE contenidos_bpo
    ADD COLUMN IF NOT EXISTS archivo_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS archivo_nombre VARCHAR(255),
    ADD COLUMN IF NOT EXISTS archivo_mime VARCHAR(100),
    ADD COLUMN IF NOT EXISTS archivo_tamano BIGINT;
