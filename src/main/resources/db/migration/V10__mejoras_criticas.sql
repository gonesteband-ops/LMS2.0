-- ========================================================
-- MigraciÃ³n V10 â€” Mejoras CrÃ­ticas v1.0
-- Fecha: 14-Jul-2026
-- Tareas: Firma Digital de Cumplimiento + Forzar Completado
-- ========================================================

-- Tarea 1.1: Campo requiere_firma_cumplimiento en cursos
ALTER TABLE cursos ADD COLUMN IF NOT EXISTS requiere_firma_cumplimiento BOOLEAN NOT NULL DEFAULT FALSE;

-- Tarea 1.2: Campos de firma digital en inscripciones
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS declaracion_jurada_aceptada BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS ip_firma VARCHAR(45);
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS timestamp_firma TIMESTAMP;
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS firma_hash VARCHAR(64);

-- Tarea 2.1: Campos de trazabilidad forzado por supervisor en inscripciones
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS forzado_por_supervisor BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS motivo_forzado TEXT;
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS comentario_forzado TEXT;
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS forzado_por_usuario_id UUID;
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS forzado_en TIMESTAMP;
ALTER TABLE inscripciones ADD COLUMN IF NOT EXISTS etiqueta_forzado VARCHAR(100);

