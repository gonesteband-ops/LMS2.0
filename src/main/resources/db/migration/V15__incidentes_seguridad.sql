CREATE TABLE IF NOT EXISTS incidentes_seguridad (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
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
CREATE INDEX IF NOT EXISTS idx_incidentes_estado ON incidentes_seguridad(estado);
CREATE INDEX IF NOT EXISTS idx_incidentes_fecha ON incidentes_seguridad(fecha_deteccion);
