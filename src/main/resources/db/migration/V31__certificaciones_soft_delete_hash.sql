-- V31: Agregar soft-delete y hash SHA-256 a certificaciones_usuarios
ALTER TABLE certificaciones_usuarios
    ADD COLUMN IF NOT EXISTS hash_sha256 VARCHAR(64),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by UUID,
    ADD COLUMN IF NOT EXISTS reason VARCHAR(500);
