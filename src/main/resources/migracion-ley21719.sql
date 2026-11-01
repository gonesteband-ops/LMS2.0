-- Migraciones Ley 21.719 (V13 a V18)
-- Se ejecutan via spring.sql.init antes de Hibernate validate

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS fecha_nacimiento DATE;

ALTER TABLE eventos_auditoria ADD COLUMN IF NOT EXISTS es_pii BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS incidentes_seguridad (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo VARCHAR(50) NOT NULL,
    nivel_riesgo VARCHAR(20) NOT NULL DEFAULT 'BAJO',
    descripcion TEXT NOT NULL,
    datos_afectados TEXT,
    usuarios_afectados INT,
    ip_origen VARCHAR(45),
    fecha_deteccion TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_notificacion_agencia TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'DETECTADO',
    evidencia TEXT,
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE consentimientos ADD COLUMN IF NOT EXISTS finalidad VARCHAR(50) NOT NULL DEFAULT 'CUENTA';
ALTER TABLE consentimientos ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE consentimientos ADD COLUMN IF NOT EXISTS fecha_revocacion TIMESTAMP;

CREATE TABLE IF NOT EXISTS solicitudes_eliminacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_solicitud TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_ejecucion TIMESTAMP,
    fecha_cancelacion TIMESTAMP,
    motivo TEXT,
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS estado_tratamiento VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';

CREATE TABLE IF NOT EXISTS rectificaciones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    campo_modificado VARCHAR(50) NOT NULL,
    valor_anterior TEXT,
    valor_nuevo TEXT NOT NULL,
    ip_origen VARCHAR(45),
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
