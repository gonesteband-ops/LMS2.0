ALTER TABLE consentimientos ADD COLUMN IF NOT EXISTS finalidad VARCHAR(50) NOT NULL DEFAULT 'CUENTA';
ALTER TABLE consentimientos ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE consentimientos ADD COLUMN IF NOT EXISTS fecha_revocacion TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_consentimiento_finalidad ON consentimientos(usuario_id, finalidad);
