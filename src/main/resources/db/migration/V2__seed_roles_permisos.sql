-- ============================================================
-- Migración V2: Seed de roles y permisos base
-- ============================================================

-- ============================================================
-- 1. PERMISOS BASE
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
-- 2. ROLES BASE
-- ============================================================

-- SUPERADMIN: Todos los permisos
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
-- 3. ASIGNACIÓN DE PERMISOS POR ROL
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

-- ESTUDIANTE: solo lectura y firma
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'ESTUDIANTE'
  AND p.nombre IN (
    'CURSO_LEER',
    'EVALUACION_LEER',
    'DOCUMENTO_SUBIR',
    'FIRMA_CREAR'
  )
ON CONFLICT DO NOTHING;
