-- =============================================================
-- Migration: V21__seed_roles_bpo.sql
-- Descripción: Crea roles BPO y asigna permisos + usuario admin
-- =============================================================

-- 1. Roles BPO
INSERT INTO roles (id, nombre, descripcion, es_sistema, hash_sha256) VALUES
    (gen_random_uuid(), 'BPO_AGENTE', 'Agente BPO - solo lectura', FALSE,
     '305d5a250d56097f22653571b2323eed11c445331cffaabef655dd85042a2581'),
    (gen_random_uuid(), 'BPO_SUPERVISOR', 'Supervisor BPO - CRUD asignado', FALSE,
     'b63bf35592516946ed30c54d354001aef99f4af04364766b36cbf2cb882d54f3'),
    (gen_random_uuid(), 'BPO_ADMIN', 'Admin BPO - CRUD total', FALSE,
     '6fa2f1d2650a219ae4c9392dbb489652b63c37854ad5c96ffda5e20680057a97')
ON CONFLICT (nombre) DO NOTHING;

-- 2. Asignar permisos BPO a cada rol
-- BPO_AGENTE: solo LEER
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'BPO_AGENTE'
  AND p.nombre IN ('BPO_CONTENIDO_LEER')
ON CONFLICT DO NOTHING;

-- BPO_SUPERVISOR: LEER, CREAR, ACTUALIZAR, ELIMINAR
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'BPO_SUPERVISOR'
  AND p.nombre IN ('BPO_CONTENIDO_LEER', 'BPO_CONTENIDO_CREAR', 'BPO_CONTENIDO_ACTUALIZAR', 'BPO_CONTENIDO_ELIMINAR')
ON CONFLICT DO NOTHING;

-- BPO_ADMIN: todos los permisos BPO
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'BPO_ADMIN'
  AND p.recurso = 'bpo.contenido'
ON CONFLICT DO NOTHING;

-- 3. Asignar rol BPO_ADMIN al admin existente (admin@lms.cl)
INSERT INTO usuarios_roles (usuario_id, rol_id)
SELECT u.id, r.id FROM usuarios u, roles r
WHERE u.email = 'admin@lms.cl'
  AND r.nombre = 'BPO_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM usuarios_roles ur
    WHERE ur.usuario_id = u.id AND ur.rol_id = r.id
  );
