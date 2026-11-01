-- =============================================================
-- Migration: V23 - Agrega permisos BPO a roles existentes
-- =============================================================

-- BPO_CONTENIDO_LEER a INSTRUCTOR
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'INSTRUCTOR'
  AND p.nombre = 'BPO_CONTENIDO_LEER'
ON CONFLICT DO NOTHING;

-- BPO_CONTENIDO_* a ADMIN y SUPERADMIN
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre IN ('ADMIN', 'SUPERADMIN')
  AND p.recurso = 'bpo.contenido'
ON CONFLICT DO NOTHING;

-- Asignar BPO_ADMIN a todos los ADMIN y SUPERADMIN existentes
INSERT INTO usuarios_roles (usuario_id, rol_id)
SELECT ur.usuario_id, r.id FROM roles r, usuarios_roles ur, roles rol_usuario
WHERE r.nombre = 'BPO_ADMIN'
  AND ur.rol_id = rol_usuario.id
  AND rol_usuario.nombre IN ('ADMIN', 'SUPERADMIN')
  AND NOT EXISTS (
    SELECT 1 FROM usuarios_roles ur2
    WHERE ur2.usuario_id = ur.usuario_id AND ur2.rol_id = r.id
  )
ON CONFLICT DO NOTHING;
