-- ============================================================
-- SCRIPT COMPLETO LMS LEGALTECH — PostgreSQL 16
-- Basado en el modelo JPA del proyecto Java/Spring Boot
-- Incluye: Tablas, índices, restricciones, relaciones,
--          funciones, triggers, roles y permisos base.
-- Cumplimiento: Ley 19.799 (Firma Electrónica),
--               Ley 21.719 (Ciberseguridad)
-- ============================================================

-- ============================================================
-- EXTENSIONES
-- ============================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

BEGIN;

-- ============================================================
-- 1. TABLA: roles
-- ============================================================
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    es_sistema BOOLEAN NOT NULL DEFAULT FALSE,
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_rol_nombre ON roles(nombre);

-- ============================================================
-- 2. TABLA: permisos
-- ============================================================
CREATE TABLE permisos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) NOT NULL UNIQUE,
    recurso VARCHAR(50) NOT NULL,
    accion VARCHAR(50) NOT NULL,
    descripcion TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_permiso_recurso ON permisos(recurso, accion);

-- ============================================================
-- 3. TABLA: roles_permisos (N:N)
-- ============================================================
CREATE TABLE roles_permisos (
    rol_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permiso_id UUID NOT NULL REFERENCES permisos(id) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, permiso_id)
);

-- ============================================================
-- 4. TABLA: usuarios
-- ============================================================
CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    rut VARCHAR(20) UNIQUE,
    nombre_completo VARCHAR(255) NOT NULL,
    hash_contrasena VARCHAR(128) NOT NULL,
    mfa_secreto VARCHAR(64),
    mfa_habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_login TIMESTAMP,
    intentos_fallidos INTEGER NOT NULL DEFAULT 0,
    bloqueado_hasta TIMESTAMP,
    hash_sha256 VARCHAR(64) NOT NULL,
    consentimiento_otorgado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    reason TEXT
);

CREATE INDEX idx_usuario_email ON usuarios(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_usuario_rut ON usuarios(rut) WHERE rut IS NOT NULL;
CREATE INDEX idx_usuario_deleted ON usuarios(deleted_at);

-- ============================================================
-- 5. TABLA: usuarios_roles (N:N)
-- ============================================================
CREATE TABLE usuarios_roles (
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, rol_id)
);

-- ============================================================
-- 6. TABLA: consentimientos
-- ============================================================
CREATE TABLE consentimientos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    ip_origen VARCHAR(45) NOT NULL,
    user_agent VARCHAR(512) NOT NULL,
    hash_documento VARCHAR(64) NOT NULL,
    version_politica VARCHAR(20) NOT NULL,
    idioma VARCHAR(10) NOT NULL,
    aceptado BOOLEAN NOT NULL,
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_consentimiento_usuario ON consentimientos(usuario_id);

-- ============================================================
-- 7. TABLA: documentos
-- ============================================================
CREATE TABLE documentos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre_original VARCHAR(255) NOT NULL,
    url_storage VARCHAR(512) NOT NULL,
    hash_sha256 VARCHAR(64) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    metadata TEXT,
    codigo VARCHAR(20) UNIQUE,
    habilitado BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    reason TEXT
);

-- ============================================================
-- 8. TABLA: firmas_electronicas
-- ============================================================
CREATE TABLE firmas_electronicas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    documento_id UUID NOT NULL REFERENCES documentos(id) ON DELETE CASCADE,
    ip_origen VARCHAR(45) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_agent VARCHAR(512) NOT NULL,
    sistema_operativo VARCHAR(100),
    geolocalizacion VARCHAR(100),
    hash_documento VARCHAR(64) NOT NULL,
    hash_anterior VARCHAR(64),
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_firma_usuario ON firmas_electronicas(usuario_id);
CREATE INDEX idx_firma_documento ON firmas_electronicas(documento_id);
CREATE INDEX idx_firma_timestamp ON firmas_electronicas(timestamp);

-- ============================================================
-- 9. TABLA: cursos
-- ============================================================
CREATE TABLE cursos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    version_actual INTEGER NOT NULL DEFAULT 1,
    estado VARCHAR(20) NOT NULL DEFAULT 'borrador',
    categoria VARCHAR(100),
    imagen_url VARCHAR(512),
    hash_sha256 VARCHAR(64) NOT NULL,
    requiere_firma_cumplimiento BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_limite TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    reason TEXT
);

CREATE INDEX idx_curso_estado ON cursos(estado);
CREATE INDEX idx_curso_categoria ON cursos(categoria);

-- ============================================================
-- 10. TABLA: versiones_curso
-- ============================================================
CREATE TABLE versiones_curso (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    curso_id UUID NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
    numero_version INTEGER NOT NULL,
    contenido TEXT NOT NULL,
    hash_contenido VARCHAR(64) NOT NULL,
    hash_version_anterior VARCHAR(64),
    autor_id UUID NOT NULL,
    motivo_cambio TEXT,
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(curso_id, numero_version)
);

CREATE INDEX idx_version_curso ON versiones_curso(curso_id, numero_version DESC);

-- ============================================================
-- 11. TABLA: evaluaciones
-- ============================================================
CREATE TABLE evaluaciones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    curso_id UUID NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    peso_total DOUBLE PRECISION NOT NULL DEFAULT 100.0,
    tiempo_estimado_minutos INTEGER,
    nota_minima DOUBLE PRECISION NOT NULL DEFAULT 60.0,
    configuracion TEXT,
    codigo VARCHAR(20) UNIQUE,
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_evaluacion_curso ON evaluaciones(curso_id);

-- ============================================================
-- 12. TABLA: preguntas
-- ============================================================
CREATE TABLE preguntas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evaluacion_id UUID NOT NULL REFERENCES evaluaciones(id) ON DELETE CASCADE,
    tipo VARCHAR(30) NOT NULL,
    contenido TEXT NOT NULL,
    peso DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    tiempo_respuesta_ms INTEGER,
    opciones TEXT,
    respuesta_correcta TEXT,
    multimedia_url VARCHAR(512),
    hash_sha256 VARCHAR(64) NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_pregunta_evaluacion ON preguntas(evaluacion_id, orden);

-- ============================================================
-- 13. TABLA: inscripciones
-- ============================================================
CREATE TABLE inscripciones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    curso_id UUID NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    fecha_inscripcion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_limite TIMESTAMP,
    completado BOOLEAN NOT NULL DEFAULT FALSE,
    nota DOUBLE PRECISION,
    fecha_completado TIMESTAMP,
    hash_sha256 VARCHAR(64) NOT NULL,
    declaracion_jurada_aceptada BOOLEAN NOT NULL DEFAULT FALSE,
    ip_firma VARCHAR(45),
    timestamp_firma TIMESTAMP,
    firma_hash VARCHAR(64),
    forzado_por_supervisor BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_forzado TEXT,
    comentario_forzado TEXT,
    forzado_por_usuario_id UUID,
    forzado_en TIMESTAMP,
    etiqueta_forzado VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    reason TEXT,
    UNIQUE(curso_id, usuario_id, deleted_at)
);

CREATE INDEX idx_inscripcion_curso ON inscripciones(curso_id);
CREATE INDEX idx_inscripcion_usuario ON inscripciones(usuario_id);
CREATE INDEX idx_inscripcion_usuario_activa ON inscripciones(usuario_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_inscripcion_completado ON inscripciones(completado);
CREATE INDEX idx_inscripcion_fecha_limite ON inscripciones(fecha_limite);

-- ============================================================
-- 14. TABLA: cursos_documentos (N:N)
-- ============================================================
CREATE TABLE cursos_documentos (
    curso_id UUID NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
    documento_id UUID NOT NULL REFERENCES documentos(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (curso_id, documento_id)
);

CREATE INDEX idx_cursos_docs_documento ON cursos_documentos(documento_id);

-- ============================================================
-- 15. TABLA: paquetes_scorm
-- ============================================================
CREATE TABLE paquetes_scorm (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    curso_id UUID NOT NULL REFERENCES cursos(id),
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    version_scorm VARCHAR(10) NOT NULL,
    url_zip VARCHAR(512) NOT NULL,
    url_index VARCHAR(512) NOT NULL,
    hash_sha256 VARCHAR(64) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    codigo VARCHAR(20) UNIQUE,
    habilitado BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    reason TEXT
);

CREATE INDEX idx_scorm_paquete_curso ON paquetes_scorm(curso_id);

-- ============================================================
-- 16. TABLA: scorm_datos_alumno
-- ============================================================
CREATE TABLE scorm_datos_alumno (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    paquete_id UUID NOT NULL REFERENCES paquetes_scorm(id),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    intento_id UUID NOT NULL,
    clave VARCHAR(255) NOT NULL,
    valor TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(paquete_id, usuario_id, intento_id, clave)
);

CREATE INDEX idx_scorm_datos_paquete ON scorm_datos_alumno(paquete_id);
CREATE INDEX idx_scorm_datos_usuario ON scorm_datos_alumno(usuario_id);

-- ============================================================
-- 17. TABLA: intentos_evaluacion
-- ============================================================
CREATE TABLE intentos_evaluacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evaluacion_id UUID NOT NULL REFERENCES evaluaciones(id) ON DELETE CASCADE,
    inscripcion_id UUID REFERENCES inscripciones(id),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    respuestas JSONB,
    nota DOUBLE PRECISION,
    aprobado BOOLEAN,
    estado VARCHAR(20) NOT NULL DEFAULT 'EN_CURSO',
    fecha_inicio TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_entrega TIMESTAMP,
    hash_sha256 VARCHAR(64) NOT NULL,
    reiniciado_por UUID,
    motivo_reinicio TEXT,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_intento_evaluacion ON intentos_evaluacion(evaluacion_id);
CREATE INDEX idx_intento_usuario ON intentos_evaluacion(usuario_id);
CREATE INDEX idx_intento_inscripcion ON intentos_evaluacion(inscripcion_id);

-- ============================================================
-- 18. TABLA: eventos_auditoria (SOLO INSERT — Event Sourcing)
-- ============================================================
CREATE TABLE eventos_auditoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_evento VARCHAR(50) NOT NULL,
    actor_id UUID,
    accion VARCHAR(100) NOT NULL,
    recurso_tipo VARCHAR(50),
    recurso_id UUID,
    metadata JSONB NOT NULL,
    hash_anterior VARCHAR(64),
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_evento_tipo ON eventos_auditoria(tipo_evento);
CREATE INDEX idx_evento_actor ON eventos_auditoria(actor_id);
CREATE INDEX idx_evento_created ON eventos_auditoria(created_at DESC);

-- ============================================================
-- 19. TABLA: notificaciones
-- ============================================================
CREATE TABLE notificaciones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('vencimiento', 'reprobacion', 'logro', 'sistema')),
    titulo VARCHAR(200) NOT NULL,
    mensaje TEXT NOT NULL,
    link VARCHAR(500),
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notif_usuario ON notificaciones(usuario_id);
CREATE INDEX idx_notif_leida ON notificaciones(leida);
CREATE INDEX idx_notif_tipo ON notificaciones(tipo);
CREATE INDEX idx_notif_created ON notificaciones(created_at DESC);

-- ============================================================
-- 20. TABLA: exportaciones
-- ============================================================
CREATE TABLE exportaciones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    solicitante_id UUID NOT NULL REFERENCES usuarios(id),
    tipo_reporte VARCHAR(50) NOT NULL,
    filtros TEXT,
    estado VARCHAR(20) NOT NULL DEFAULT 'pendiente',
    url_resultado VARCHAR(512),
    task_id_celery VARCHAR(255),
    error_mensaje TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_exportacion_solicitante ON exportaciones(solicitante_id);
CREATE INDEX idx_exportacion_estado ON exportaciones(estado);

-- ============================================================
-- 21. TABLA: secuencias_codigos
-- ============================================================
CREATE TABLE secuencias_codigos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prefijo VARCHAR(5) NOT NULL UNIQUE,
    ultimo_numero INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- FUNCIÓN: generar_codigo_alfanumerico(prefijo)
-- Genera códigos correlativos: EVA001, SCO001, DOC001
-- ============================================================
CREATE OR REPLACE FUNCTION generar_codigo_alfanumerico(p_prefijo VARCHAR)
RETURNS VARCHAR(20) AS $$
DECLARE
    v_numero INTEGER;
    v_codigo VARCHAR(20);
BEGIN
    UPDATE secuencias_codigos
    SET ultimo_numero = ultimo_numero + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE prefijo = p_prefijo
    RETURNING ultimo_numero INTO v_numero;

    IF NOT FOUND THEN
        INSERT INTO secuencias_codigos (prefijo, ultimo_numero)
        VALUES (p_prefijo, 1)
        RETURNING ultimo_numero INTO v_numero;
    END IF;

    v_codigo := p_prefijo || LPAD(v_numero::TEXT, 3, '0');
    RETURN v_codigo;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- TRIGGER: Proteger eventos_auditoria (solo INSERT)
-- ============================================================
CREATE OR REPLACE FUNCTION prevent_event_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'eventos_auditoria: INSERT-only table. UPDATE/DELETE not allowed.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_event_update
    BEFORE UPDATE ON eventos_auditoria
    FOR EACH ROW EXECUTE FUNCTION prevent_event_mutation();

CREATE TRIGGER trg_prevent_event_delete
    BEFORE DELETE ON eventos_auditoria
    FOR EACH ROW EXECUTE FUNCTION prevent_event_mutation();

-- ============================================================
-- SEED: Secuencias de códigos iniciales
-- ============================================================
INSERT INTO secuencias_codigos (prefijo, ultimo_numero) VALUES
    ('EVA', 0),
    ('SCO', 0),
    ('DOC', 0);

-- ============================================================
-- SEED: Permisos base del sistema
-- ============================================================
INSERT INTO permisos (id, nombre, recurso, accion, descripcion) VALUES
    (gen_random_uuid(), 'USUARIO_CREAR', 'USUARIO', 'CREAR', 'Crear usuarios'),
    (gen_random_uuid(), 'USUARIO_LEER', 'USUARIO', 'LEER', 'Leer usuarios'),
    (gen_random_uuid(), 'USUARIO_ACTUALIZAR', 'USUARIO', 'ACTUALIZAR', 'Actualizar usuarios'),
    (gen_random_uuid(), 'USUARIO_ELIMINAR', 'USUARIO', 'ELIMINAR', 'Eliminar usuarios'),
    (gen_random_uuid(), 'CURSO_CREAR', 'CURSO', 'CREAR', 'Crear cursos'),
    (gen_random_uuid(), 'CURSO_LEER', 'CURSO', 'LEER', 'Leer cursos'),
    (gen_random_uuid(), 'CURSO_ACTUALIZAR', 'CURSO', 'ACTUALIZAR', 'Actualizar cursos'),
    (gen_random_uuid(), 'CURSO_ELIMINAR', 'CURSO', 'ELIMINAR', 'Eliminar cursos'),
    (gen_random_uuid(), 'CURSO_PUBLICAR', 'CURSO', 'PUBLICAR', 'Publicar cursos'),
    (gen_random_uuid(), 'EVALUACION_CREAR', 'EVALUACION', 'CREAR', 'Crear evaluaciones'),
    (gen_random_uuid(), 'EVALUACION_LEER', 'EVALUACION', 'LEER', 'Leer evaluaciones'),
    (gen_random_uuid(), 'EVALUACION_ACTUALIZAR', 'EVALUACION', 'ACTUALIZAR', 'Actualizar evaluaciones'),
    (gen_random_uuid(), 'EVALUACION_ELIMINAR', 'EVALUACION', 'ELIMINAR', 'Eliminar evaluaciones'),
    (gen_random_uuid(), 'ROL_CREAR', 'ROL', 'CREAR', 'Crear roles'),
    (gen_random_uuid(), 'ROL_LEER', 'ROL', 'LEER', 'Leer roles'),
    (gen_random_uuid(), 'ROL_ACTUALIZAR', 'ROL', 'ACTUALIZAR', 'Actualizar roles'),
    (gen_random_uuid(), 'ROL_ELIMINAR', 'ROL', 'ELIMINAR', 'Eliminar roles'),
    (gen_random_uuid(), 'EXPORTACION_CREAR', 'EXPORTACION', 'CREAR', 'Solicitar exportaciones'),
    (gen_random_uuid(), 'EXPORTACION_LEER', 'EXPORTACION', 'LEER', 'Leer exportaciones'),
    (gen_random_uuid(), 'AUDITORIA_LEER', 'AUDITORIA', 'LEER', 'Leer eventos de auditoría'),
    (gen_random_uuid(), 'DOCUMENTO_SUBIR', 'DOCUMENTO', 'SUBIR', 'Subir documentos'),
    (gen_random_uuid(), 'DOCUMENTO_ELIMINAR', 'DOCUMENTO', 'ELIMINAR', 'Eliminar documentos'),
    (gen_random_uuid(), 'FIRMA_CREAR', 'FIRMA', 'CREAR', 'Firmar electrónicamente'),
    (gen_random_uuid(), 'CONSENTIMIENTO_ADMIN', 'CONSENTIMIENTO', 'ADMIN', 'Administrar consentimientos')
ON CONFLICT (nombre) DO NOTHING;

-- ============================================================
-- SEED: Roles del sistema
-- ============================================================
INSERT INTO roles (id, nombre, descripcion, es_sistema, hash_sha256) VALUES
    (gen_random_uuid(), 'SUPERADMIN', 'Superadministrador del sistema', TRUE,
     encode(digest('SUPERADMIN' || NOW()::TEXT, 'sha256'), 'hex')),
    (gen_random_uuid(), 'ADMIN', 'Administrador del sistema', TRUE,
     encode(digest('ADMIN' || NOW()::TEXT, 'sha256'), 'hex')),
    (gen_random_uuid(), 'INSTRUCTOR', 'Instructor de cursos', TRUE,
     encode(digest('INSTRUCTOR' || NOW()::TEXT, 'sha256'), 'hex')),
    (gen_random_uuid(), 'ESTUDIANTE', 'Estudiante del sistema', TRUE,
     encode(digest('ESTUDIANTE' || NOW()::TEXT, 'sha256'), 'hex'))
ON CONFLICT (nombre) DO NOTHING;

-- ============================================================
-- SEED: Asignación de permisos por rol
-- ============================================================

-- SUPERADMIN: todos los permisos
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'SUPERADMIN'
ON CONFLICT DO NOTHING;

-- ADMIN: permisos administrativos (excepto eliminar roles)
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'ADMIN'
  AND p.nombre IN (
    'USUARIO_CREAR', 'USUARIO_LEER', 'USUARIO_ACTUALIZAR', 'USUARIO_ELIMINAR',
    'CURSO_CREAR', 'CURSO_LEER', 'CURSO_ACTUALIZAR', 'CURSO_ELIMINAR', 'CURSO_PUBLICAR',
    'EVALUACION_CREAR', 'EVALUACION_LEER', 'EVALUACION_ACTUALIZAR', 'EVALUACION_ELIMINAR',
    'ROL_LEER',
    'EXPORTACION_CREAR', 'EXPORTACION_LEER',
    'AUDITORIA_LEER',
    'DOCUMENTO_SUBIR', 'DOCUMENTO_ELIMINAR',
    'FIRMA_CREAR',
    'CONSENTIMIENTO_ADMIN'
  )
ON CONFLICT DO NOTHING;

-- INSTRUCTOR: gestión de cursos y evaluaciones
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'INSTRUCTOR'
  AND p.nombre IN (
    'CURSO_CREAR', 'CURSO_LEER', 'CURSO_ACTUALIZAR', 'CURSO_PUBLICAR',
    'EVALUACION_CREAR', 'EVALUACION_LEER', 'EVALUACION_ACTUALIZAR',
    'EXPORTACION_CREAR', 'EXPORTACION_LEER',
    'DOCUMENTO_SUBIR',
    'FIRMA_CREAR'
  )
ON CONFLICT DO NOTHING;

-- ESTUDIANTE: solo lectura y firma (sin DOCUMENTO_SUBIR)
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'ESTUDIANTE'
  AND p.nombre IN (
    'CURSO_LEER',
    'EVALUACION_LEER',
    'FIRMA_CREAR'
  )
ON CONFLICT DO NOTHING;

-- ============================================================
-- COMENTARIOS DE DOCUMENTACIÓN
-- ============================================================
COMMENT ON TABLE roles IS 'Roles del sistema RBAC — SUPERADMIN, ADMIN, INSTRUCTOR, ESTUDIANTE';
COMMENT ON TABLE permisos IS 'Permisos granulares sobre recurso+acción';
COMMENT ON TABLE roles_permisos IS 'Asignación N:N de permisos a roles';
COMMENT ON TABLE usuarios IS 'Usuarios del sistema — PK UUID, hash de contraseña, MFA, soft-delete';
COMMENT ON TABLE usuarios_roles IS 'Asignación N:N de roles a usuarios';
COMMENT ON TABLE consentimientos IS 'Registro de consentimiento informado (Ley 19.799)';
COMMENT ON TABLE documentos IS 'Documentos almacenados en MinIO/S3 con trazabilidad';
COMMENT ON TABLE firmas_electronicas IS 'Firmas electrónicas con no repudio (Ley 19.799)';
COMMENT ON TABLE cursos IS 'Cursos del LMS con versionado y estados';
COMMENT ON TABLE versiones_curso IS 'Historial de versiones de contenido del curso';
COMMENT ON TABLE evaluaciones IS 'Evaluaciones asociadas a cursos';
COMMENT ON TABLE preguntas IS 'Preguntas de evaluaciones con tipos y opciones';
COMMENT ON TABLE inscripciones IS 'Inscripción de usuarios en cursos con trazabilidad';
COMMENT ON TABLE cursos_documentos IS 'Relación N:N entre cursos y documentos';
COMMENT ON TABLE paquetes_scorm IS 'Paquetes SCORM subidos (1.2 / 2004)';
COMMENT ON TABLE scorm_datos_alumno IS 'Datos de progreso SCORM por alumno';
COMMENT ON TABLE intentos_evaluacion IS 'Intentos de evaluación por estudiante con corrección';
COMMENT ON TABLE eventos_auditoria IS 'Event Sourcing — INSERT-only, hash encadenado';
COMMENT ON TABLE notificaciones IS 'Notificaciones in-app: vencimiento, reprobacion, logro, sistema';
COMMENT ON TABLE exportaciones IS 'Exportaciones de datos solicitadas por usuarios';
COMMENT ON TABLE secuencias_codigos IS 'Secuencias para códigos alfanuméricos EVA/SCO/DOC';

-- ============================================================
-- Migraciones V13 a V18 (Ley 21.719)
-- ============================================================

-- V13: Fecha de nacimiento en usuarios
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS fecha_nacimiento DATE;

-- V14: Flag PII en eventos de auditoría
ALTER TABLE eventos_auditoria ADD COLUMN IF NOT EXISTS es_pii BOOLEAN NOT NULL DEFAULT FALSE;

-- V15: Incidentes de seguridad (breach detection)
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
CREATE INDEX IF NOT EXISTS idx_incidentes_estado ON incidentes_seguridad(estado);
CREATE INDEX IF NOT EXISTS idx_incidentes_fecha ON incidentes_seguridad(fecha_deteccion);

-- V16: Consentimiento granular (finalidad, activo, fecha_revocacion)
ALTER TABLE consentimientos ADD COLUMN IF NOT EXISTS finalidad VARCHAR(50) NOT NULL DEFAULT 'CUENTA';
ALTER TABLE consentimientos ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE consentimientos ADD COLUMN IF NOT EXISTS fecha_revocacion TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_consentimiento_finalidad ON consentimientos(usuario_id, finalidad);

-- V17: Solicitudes de eliminación + estado_tratamiento en usuarios
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
CREATE INDEX IF NOT EXISTS idx_solicitudes_usuario ON solicitudes_eliminacion(usuario_id);
CREATE INDEX IF NOT EXISTS idx_solicitudes_estado ON solicitudes_eliminacion(estado);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS estado_tratamiento VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';

-- V18: Historial de rectificaciones
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
CREATE INDEX IF NOT EXISTS idx_rectificaciones_usuario ON rectificaciones(usuario_id);

COMMIT;

-- ============================================================
-- FIN DEL SCRIPT
-- ============================================================
