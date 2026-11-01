-- ============================================================
-- MigraciÃ³n V5: Tabla de Notificaciones AutomÃ¡ticas
-- Sistema de notificaciones internas (in-app) con soporte
-- futuro para envÃ­o por email.
-- ============================================================

-- ============================================================
-- 1. TABLA: notificaciones
-- Almacena notificaciones automÃ¡ticas para usuarios del sistema.
-- Tipos: vencimiento, reprobacion, logro, sistema
-- ============================================================
CREATE TABLE IF NOT EXISTS notificaciones (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('vencimiento', 'reprobacion', 'logro', 'sistema')),
    titulo VARCHAR(200) NOT NULL,
    mensaje TEXT NOT NULL,
    link VARCHAR(500),
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 2. ÃNDICES
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_notif_usuario  ON notificaciones(usuario_id);
CREATE INDEX IF NOT EXISTS idx_notif_leida    ON notificaciones(leida);
CREATE INDEX IF NOT EXISTS idx_notif_tipo     ON notificaciones(tipo);
CREATE INDEX IF NOT EXISTS idx_notif_created  ON notificaciones(created_at DESC);

-- ============================================================
-- 3. COMENTARIOS
-- ============================================================
COMMENT ON TABLE  notificaciones               IS 'Notificaciones automÃ¡ticas para usuarios del sistema';
COMMENT ON COLUMN notificaciones.id            IS 'Identificador Ãºnico de la notificaciÃ³n';
COMMENT ON COLUMN notificaciones.usuario_id    IS 'Usuario destinatario de la notificaciÃ³n';
COMMENT ON COLUMN notificaciones.tipo          IS 'Tipo de notificaciÃ³n: vencimiento, reprobacion, logro, sistema';
COMMENT ON COLUMN notificaciones.titulo        IS 'TÃ­tulo breve de la notificaciÃ³n';
COMMENT ON COLUMN notificaciones.mensaje       IS 'Cuerpo del mensaje de la notificaciÃ³n';
COMMENT ON COLUMN notificaciones.link          IS 'Enlace opcional a recurso relacionado';
COMMENT ON COLUMN notificaciones.leida         IS 'Indica si el usuario ha leÃ­do la notificaciÃ³n';
COMMENT ON COLUMN notificaciones.created_at    IS 'Fecha y hora de creaciÃ³n de la notificaciÃ³n';

