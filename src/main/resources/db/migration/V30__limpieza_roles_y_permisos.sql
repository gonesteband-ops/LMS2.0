-- V30: Limpieza de roles y permisos de certificaciones
-- Elimina roles redundantes: ADMIN, BPO_ADMIN, BPO_SUPERVISOR, ROL DE PRUEBA
-- Agrega permisos de certificaciones a SUPERADMIN, ESTUDIANTE e INSTRUCTOR

-- 1. Migrar usuarios de roles eliminados a SUPERADMIN
INSERT INTO usuarios_roles (usuario_id, rol_id)
SELECT ur.usuario_id, (SELECT id FROM roles WHERE nombre = 'SUPERADMIN')
FROM usuarios_roles ur
JOIN roles r ON r.id = ur.rol_id
WHERE r.nombre IN ('ADMIN', 'BPO_ADMIN', 'BPO_SUPERVISOR')
  AND NOT EXISTS (
    SELECT 1 FROM usuarios_roles ur2
    WHERE ur2.usuario_id = ur.usuario_id
      AND ur2.rol_id = (SELECT id FROM roles WHERE nombre = 'SUPERADMIN')
  );

-- 2. Eliminar roles_permisos de roles a eliminar
DELETE FROM roles_permisos
WHERE rol_id IN (SELECT id FROM roles WHERE nombre IN ('ADMIN', 'BPO_ADMIN', 'BPO_SUPERVISOR', 'ROL DE PRUEBA'));

-- 3. Eliminar usuarios_roles de roles a eliminar
DELETE FROM usuarios_roles
WHERE rol_id IN (SELECT id FROM roles WHERE nombre IN ('ADMIN', 'BPO_ADMIN', 'BPO_SUPERVISOR', 'ROL DE PRUEBA'));

-- 4. Eliminar roles
DELETE FROM roles WHERE nombre IN ('ADMIN', 'BPO_ADMIN', 'BPO_SUPERVISOR', 'ROL DE PRUEBA');

-- 5. Crear permisos de certificaciones
INSERT INTO permisos (id, nombre, recurso, accion, descripcion)
SELECT gen_random_uuid(), v.nombre, v.recurso, v.accion, v.descripcion
FROM (VALUES
    ('CERTIFICACION_LEER', 'certificacion', 'LEER', 'Leer certificaciones'),
    ('CERTIFICACION_RENDIR', 'certificacion', 'RENDIR', 'Rendir certificaciones'),
    ('CERTIFICACION_ADMIN', 'certificacion', 'ADMIN', 'Administrar certificaciones')
) AS v(nombre, recurso, accion, descripcion)
WHERE NOT EXISTS (SELECT 1 FROM permisos p WHERE p.nombre = v.nombre);

-- 6. Asignar permisos a SUPERADMIN
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'SUPERADMIN'
  AND p.nombre IN ('CERTIFICACION_LEER', 'CERTIFICACION_RENDIR', 'CERTIFICACION_ADMIN')
  AND NOT EXISTS (SELECT 1 FROM roles_permisos rp WHERE rp.rol_id = r.id AND rp.permiso_id = p.id);

-- 7. Asignar permisos a ESTUDIANTE
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'ESTUDIANTE'
  AND p.nombre IN ('CERTIFICACION_LEER', 'CERTIFICACION_RENDIR')
  AND NOT EXISTS (SELECT 1 FROM roles_permisos rp WHERE rp.rol_id = r.id AND rp.permiso_id = p.id);

-- 8. Asignar permisos a INSTRUCTOR
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'INSTRUCTOR'
  AND p.nombre IN ('CERTIFICACION_LEER', 'CERTIFICACION_ADMIN')
  AND NOT EXISTS (SELECT 1 FROM roles_permisos rp WHERE rp.rol_id = r.id AND rp.permiso_id = p.id);
