-- ============================================================
-- MigraciÃ³n V3: CorrecciÃ³n de Ã­ndices y metadata a JSONB
-- ============================================================

-- ============================================================
-- 1. Reemplazar Ã­ndice email por versiÃ³n con soft-delete
-- ============================================================
DROP INDEX IF EXISTS idx_usuario_email;
DROP INDEX IF EXISTS idx_usuario_email_activo;
CREATE INDEX IF NOT EXISTS idx_usuario_email_activo ON usuarios(email) WHERE deleted_at IS NULL;

-- ============================================================
-- 2. Convertir metadata de eventos_auditoria de TEXT a JSONB
-- ============================================================
-- Nota: Se asume que los datos existentes son JSON vÃ¡lido.
-- Si hay texto no-JSON, la conversiÃ³n fallarÃ¡ y requerirÃ¡ limpieza manual.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'eventos_auditoria' AND column_name = 'metadata' AND data_type = 'text'
    ) THEN
        ALTER TABLE eventos_auditoria ALTER COLUMN metadata TYPE JSONB USING metadata::jsonb;
    END IF;
END $$;

-- Ãndice GIN para bÃºsqueda en metadata JSONB
CREATE INDEX IF NOT EXISTS idx_evento_metadata_gin ON eventos_auditoria USING GIN (metadata);

