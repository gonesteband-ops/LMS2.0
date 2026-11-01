-- V26: Agregar columna deleted_at a documentos (existía en la entidad pero faltaba en la BD)

ALTER TABLE documentos ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_documentos_deleted_at ON documentos(deleted_at);
