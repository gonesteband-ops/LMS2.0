-- ============================================================
-- LMS_BASE.sql — Esquema completo LMS LegalTech
-- Compatible con PostgreSQL 16
-- Consolidado desde Flyway V1 → V8
--
-- Cumplimiento: Ley 19.799 (Firma Electrónica), Ley 21.719 (Ciberseguridad)
-- ============================================================
-- ============================================================
-- 1. EXTENSIONES
-- ============================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- 2. TABLA: roles
-- ============================================================
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    es_sistema BOOLEAN NOT NULL DEFAULT FALSE,
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_rol_nombre ON roles(nombre);

-- ============================================================
-- 3. TABLA: permisos
-- ============================================================
CREATE TABLE IF NOT EXISTS permisos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre VARCHAR(100) NOT NULL UNIQUE,
    recurso VARCHAR(50) NOT NULL,
    accion VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_permiso_recurso ON permisos(recurso, accion);

-- ============================================================
-- 4. TABLA: roles_permisos (N:N)
-- ============================================================
CREATE TABLE IF NOT EXISTS roles_permisos (
    rol_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permiso_id UUID NOT NULL REFERENCES permisos(id) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, permiso_id)
);

-- ============================================================
-- 5. TABLA: usuarios
-- ============================================================
CREATE TABLE IF NOT EXISTS usuarios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    nombre_completo VARCHAR(255) NOT NULL,
    hash_contrasena VARCHAR(128) NOT NULL,
    mfa_secreto VARCHAR(64),
    mfa_habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_login TIMESTAMP,
    intentos_fallidos INTEGER NOT NULL DEFAULT 0,
    bloqueado_hasta TIMESTAMP,
    rut VARCHAR(20) UNIQUE,
    hash_sha256 VARCHAR(64) NOT NULL,
    consentimiento_otorgado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_usuario_email_activo ON usuarios(email) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_usuario_deleted ON usuarios(deleted_at);
CREATE INDEX IF NOT EXISTS idx_usuario_rut ON usuarios(rut) WHERE rut IS NOT NULL;

-- ============================================================
-- 6. TABLA: usuarios_roles (N:N)
-- ============================================================
CREATE TABLE IF NOT EXISTS usuarios_roles (
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, rol_id)
);

-- ============================================================
-- 7. TABLA: consentimientos
-- ============================================================
CREATE TABLE IF NOT EXISTS consentimientos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    ip_origen VARCHAR(45),
    user_agent VARCHAR(512),
    hash_documento VARCHAR(64) NOT NULL,
    version_politica VARCHAR(20) NOT NULL,
    idioma VARCHAR(10) NOT NULL,
    aceptado BOOLEAN NOT NULL,
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_consentimiento_usuario ON consentimientos(usuario_id);

-- ============================================================
-- 8. TABLA: documentos
-- ============================================================
CREATE TABLE IF NOT EXISTS documentos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nombre_original VARCHAR(255) NOT NULL,
    url_storage VARCHAR(512) NOT NULL,
    hash_sha256 VARCHAR(64) NOT NULL,
    mime_type VARCHAR(100),
    tamano_bytes BIGINT NOT NULL,
    codigo VARCHAR(20) UNIQUE,
    version INTEGER NOT NULL DEFAULT 1,
    metadata TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_by UUID,
    reason TEXT
);

-- ============================================================
-- 9. TABLA: firmas_electronicas
-- ============================================================
CREATE TABLE IF NOT EXISTS firmas_electronicas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    documento_id UUID NOT NULL REFERENCES documentos(id) ON DELETE CASCADE,
    ip_origen VARCHAR(45) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_agent VARCHAR(512),
    sistema_operativo VARCHAR(100),
    geolocalizacion VARCHAR(100),
    hash_documento VARCHAR(64) NOT NULL,
    hash_anterior VARCHAR(64),
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_firma_usuario ON firmas_electronicas(usuario_id);
CREATE INDEX IF NOT EXISTS idx_firma_documento ON firmas_electronicas(documento_id);
CREATE INDEX IF NOT EXISTS idx_firma_timestamp ON firmas_electronicas(timestamp);

-- ============================================================
-- 10. TABLA: cursos
-- ============================================================
CREATE TABLE IF NOT EXISTS cursos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    version_actual INTEGER NOT NULL DEFAULT 1,
    estado VARCHAR(30) NOT NULL DEFAULT 'borrador',
    categoria VARCHAR(100),
    imagen_url VARCHAR(512),
    fecha_limite TIMESTAMP,
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_curso_estado ON cursos(estado);
CREATE INDEX IF NOT EXISTS idx_curso_categoria ON cursos(categoria);

-- ============================================================
-- 11. TABLA: versiones_curso
-- ============================================================
CREATE TABLE IF NOT EXISTS versiones_curso (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    curso_id UUID NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
    numero_version INTEGER NOT NULL,
    contenido TEXT,
    hash_contenido VARCHAR(64) NOT NULL,
    hash_version_anterior VARCHAR(64),
    autor_id UUID NOT NULL,
    motivo_cambio VARCHAR(255),
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(curso_id, numero_version)
);

CREATE INDEX IF NOT EXISTS idx_version_curso ON versiones_curso(curso_id, numero_version DESC);

-- ============================================================
-- 12. TABLA: evaluaciones
-- ============================================================
CREATE TABLE IF NOT EXISTS evaluaciones (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    curso_id UUID NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    peso_total DOUBLE PRECISION NOT NULL DEFAULT 100.0,
    tiempo_estimado_minutos INTEGER,
    configuracion TEXT,
    codigo VARCHAR(20) UNIQUE,
    hash_sha256 VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evaluacion_curso ON evaluaciones(curso_id);

-- ============================================================
-- 13. TABLA: preguntas
-- ============================================================
CREATE TABLE IF NOT EXISTS preguntas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pregunta_evaluacion ON preguntas(evaluacion_id, orden);

-- ============================================================
-- 14. TABLA: eventos_auditoria (SOLO INSERT — Event Sourcing)
-- ============================================================
CREATE TABLE IF NOT EXISTS eventos_auditoria (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tipo_evento VARCHAR(50) NOT NULL,
    actor_id UUID,
    accion VARCHAR(100) NOT NULL,
    recurso_tipo VARCHAR(50),
    recurso_id UUID,
    metadata JSONB,
    hash_anterior VARCHAR(64),
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evento_tipo ON eventos_auditoria(tipo_evento);
CREATE INDEX IF NOT EXISTS idx_evento_actor ON eventos_auditoria(actor_id);
CREATE INDEX IF NOT EXISTS idx_evento_created ON eventos_auditoria(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_evento_metadata_gin ON eventos_auditoria USING GIN (metadata);

-- Trigger para evitar UPDATE/DELETE en eventos_auditoria
CREATE OR REPLACE FUNCTION prevent_event_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'eventos_auditoria: INSERT-only table. UPDATE/DELETE not allowed.';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_event_update ON eventos_auditoria;
CREATE TRIGGER trg_prevent_event_update
    BEFORE UPDATE ON eventos_auditoria
    FOR EACH ROW EXECUTE FUNCTION prevent_event_mutation();

DROP TRIGGER IF EXISTS trg_prevent_event_delete ON eventos_auditoria;
CREATE TRIGGER trg_prevent_event_delete
    BEFORE DELETE ON eventos_auditoria
    FOR EACH ROW EXECUTE FUNCTION prevent_event_mutation();

-- ============================================================
-- 15. TABLA: exportaciones
-- ============================================================
CREATE TABLE IF NOT EXISTS exportaciones (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    solicitante_id UUID NOT NULL,
    tipo_reporte VARCHAR(50) NOT NULL,
    filtros TEXT,
    estado VARCHAR(30) NOT NULL DEFAULT 'pendiente',
    url_resultado VARCHAR(512),
    task_id_celery VARCHAR(255),
    error_mensaje TEXT,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_exportacion_solicitante ON exportaciones(solicitante_id);
CREATE INDEX IF NOT EXISTS idx_exportacion_estado ON exportaciones(estado);

-- ============================================================
-- 16. TABLA: paquetes_scorm
-- ============================================================
CREATE TABLE IF NOT EXISTS paquetes_scorm (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    curso_id UUID NOT NULL REFERENCES cursos(id),
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    version_scorm VARCHAR(10) NOT NULL,
    url_zip VARCHAR(512) NOT NULL,
    url_index VARCHAR(512) NOT NULL,
    codigo VARCHAR(20) UNIQUE,
    hash_sha256 VARCHAR(64) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID REFERENCES usuarios(id),
    reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_scorm_paquete_curso ON paquetes_scorm(curso_id);

-- ============================================================
-- 17. TABLA: scorm_datos_alumno
-- ============================================================
CREATE TABLE IF NOT EXISTS scorm_datos_alumno (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    paquete_id UUID NOT NULL REFERENCES paquetes_scorm(id),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    intento_id UUID NOT NULL,
    clave VARCHAR(255) NOT NULL,
    valor TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(paquete_id, usuario_id, intento_id, clave)
);

CREATE INDEX IF NOT EXISTS idx_scorm_datos_paquete ON scorm_datos_alumno(paquete_id);
CREATE INDEX IF NOT EXISTS idx_scorm_datos_usuario ON scorm_datos_alumno(usuario_id);

-- ============================================================
-- 18. TABLA: inscripciones
-- ============================================================
CREATE TABLE IF NOT EXISTS inscripciones (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    curso_id UUID NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    fecha_inscripcion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_limite TIMESTAMP,
    completado BOOLEAN NOT NULL DEFAULT FALSE,
    nota DOUBLE PRECISION,
    fecha_completado TIMESTAMP,
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    reason TEXT,
    UNIQUE(curso_id, usuario_id, deleted_at)
);

CREATE INDEX IF NOT EXISTS idx_inscripcion_curso ON inscripciones(curso_id);
CREATE INDEX IF NOT EXISTS idx_inscripcion_usuario ON inscripciones(usuario_id);
CREATE INDEX IF NOT EXISTS idx_inscripcion_usuario_activa ON inscripciones(usuario_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_inscripcion_completado ON inscripciones(completado);
CREATE INDEX IF NOT EXISTS idx_inscripcion_fecha_limite ON inscripciones(fecha_limite);

-- ============================================================
-- 19. TABLA: cursos_documentos (N:M)
-- ============================================================
CREATE TABLE IF NOT EXISTS cursos_documentos (
    curso_id UUID NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
    documento_id UUID NOT NULL REFERENCES documentos(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (curso_id, documento_id)
);

CREATE INDEX IF NOT EXISTS idx_cursos_docs_documento ON cursos_documentos(documento_id);

-- ============================================================
-- 20. TABLA: notificaciones
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

CREATE INDEX IF NOT EXISTS idx_notif_usuario ON notificaciones(usuario_id);
CREATE INDEX IF NOT EXISTS idx_notif_leida ON notificaciones(leida);
CREATE INDEX IF NOT EXISTS idx_notif_tipo ON notificaciones(tipo);
CREATE INDEX IF NOT EXISTS idx_notif_created ON notificaciones(created_at DESC);

-- ============================================================
-- 21. TABLA: secuencias_codigos
-- ============================================================
CREATE TABLE IF NOT EXISTS secuencias_codigos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    prefijo VARCHAR(5) NOT NULL UNIQUE,
    ultimo_numero INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 22. FUNCIÓN: generar_codigo_alfanumerico(prefijo)
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
-- 23. SEED DATA: Permisos base
-- ============================================================
INSERT INTO permisos (id, nombre, recurso, accion, descripcion) VALUES
    (uuid_generate_v4(), 'USUARIO_CREAR', 'USUARIO', 'CREAR', 'Crear usuarios'),
    (uuid_generate_v4(), 'USUARIO_LEER', 'USUARIO', 'LEER', 'Leer usuarios'),
    (uuid_generate_v4(), 'USUARIO_ACTUALIZAR', 'USUARIO', 'ACTUALIZAR', 'Actualizar usuarios'),
    (uuid_generate_v4(), 'USUARIO_ELIMINAR', 'USUARIO', 'ELIMINAR', 'Eliminar usuarios'),
    (uuid_generate_v4(), 'CURSO_CREAR', 'CURSO', 'CREAR', 'Crear cursos'),
    (uuid_generate_v4(), 'CURSO_LEER', 'CURSO', 'LEER', 'Leer cursos'),
    (uuid_generate_v4(), 'CURSO_ACTUALIZAR', 'CURSO', 'ACTUALIZAR', 'Actualizar cursos'),
    (uuid_generate_v4(), 'CURSO_ELIMINAR', 'CURSO', 'ELIMINAR', 'Eliminar cursos'),
    (uuid_generate_v4(), 'CURSO_PUBLICAR', 'CURSO', 'PUBLICAR', 'Publicar cursos'),
    (uuid_generate_v4(), 'EVALUACION_CREAR', 'EVALUACION', 'CREAR', 'Crear evaluaciones'),
    (uuid_generate_v4(), 'EVALUACION_LEER', 'EVALUACION', 'LEER', 'Leer evaluaciones'),
    (uuid_generate_v4(), 'EVALUACION_ACTUALIZAR', 'EVALUACION', 'ACTUALIZAR', 'Actualizar evaluaciones'),
    (uuid_generate_v4(), 'EVALUACION_ELIMINAR', 'EVALUACION', 'ELIMINAR', 'Eliminar evaluaciones'),
    (uuid_generate_v4(), 'ROL_CREAR', 'ROL', 'CREAR', 'Crear roles'),
    (uuid_generate_v4(), 'ROL_LEER', 'ROL', 'LEER', 'Leer roles'),
    (uuid_generate_v4(), 'ROL_ACTUALIZAR', 'ROL', 'ACTUALIZAR', 'Actualizar roles'),
    (uuid_generate_v4(), 'ROL_ELIMINAR', 'ROL', 'ELIMINAR', 'Eliminar roles'),
    (uuid_generate_v4(), 'EXPORTACION_CREAR', 'EXPORTACION', 'CREAR', 'Solicitar exportaciones'),
    (uuid_generate_v4(), 'EXPORTACION_LEER', 'EXPORTACION', 'LEER', 'Leer exportaciones'),
    (uuid_generate_v4(), 'AUDITORIA_LEER', 'AUDITORIA', 'LEER', 'Leer eventos de auditoría'),
    (uuid_generate_v4(), 'DOCUMENTO_SUBIR', 'DOCUMENTO', 'SUBIR', 'Subir documentos'),
    (uuid_generate_v4(), 'DOCUMENTO_ELIMINAR', 'DOCUMENTO', 'ELIMINAR', 'Eliminar documentos'),
    (uuid_generate_v4(), 'FIRMA_CREAR', 'FIRMA', 'CREAR', 'Firmar electrónicamente'),
    (uuid_generate_v4(), 'CONSENTIMIENTO_ADMIN', 'CONSENTIMIENTO', 'ADMIN', 'Administrar consentimientos')
ON CONFLICT (nombre) DO NOTHING;

-- ============================================================
-- 24. SEED DATA: Roles base
-- ============================================================
INSERT INTO roles (id, nombre, descripcion, es_sistema, hash_sha256) VALUES
    (uuid_generate_v4(), 'SUPERADMIN', 'Superadministrador del sistema', TRUE,
     'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2'),
    (uuid_generate_v4(), 'ADMIN', 'Administrador del sistema', TRUE,
     'b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3'),
    (uuid_generate_v4(), 'INSTRUCTOR', 'Instructor de cursos', TRUE,
     'c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4'),
    (uuid_generate_v4(), 'ESTUDIANTE', 'Estudiante del sistema', TRUE,
     'd4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5')
ON CONFLICT (nombre) DO NOTHING;

-- ============================================================
-- 25. ASIGNACIÓN DE PERMISOS POR ROL
-- ============================================================

-- SUPERADMIN: todos los permisos
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'SUPERADMIN'
ON CONFLICT DO NOTHING;

-- ADMIN: permisos administrativos
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
-- 26. SEED DATA: Secuencias de códigos iniciales
-- ============================================================
INSERT INTO secuencias_codigos (prefijo, ultimo_numero) VALUES
    ('EVA', 0),
    ('SCO', 0),
    ('DOC', 0)
ON CONFLICT (prefijo) DO NOTHING;

-- ============================================================
-- 27. COMENTARIOS DE TABLAS (documentación)
-- ============================================================
COMMENT ON TABLE notificaciones IS 'Notificaciones automáticas para usuarios del sistema';
COMMENT ON TABLE inscripciones IS 'Registro de participantes inscritos en cursos (Ley 19.799 trazabilidad)';
COMMENT ON TABLE secuencias_codigos IS 'Control de secuencias para generación de códigos alfanuméricos';
COMMENT ON FUNCTION generar_codigo_alfanumerico IS 'Genera código correlativo con formato PREFIJO + 3 dígitos (ej: EVA001)';

-- ============================================================
-- FIN DEL SCRIPT
-- ============================================================
