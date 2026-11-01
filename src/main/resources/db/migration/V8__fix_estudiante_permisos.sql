-- ============================================================
-- Migración V8: Corregir permisos de ESTUDIANTE
-- ============================================================
-- Elimina el permiso DOCUMENTO_SUBIR del rol ESTUDIANTE
-- para que los estudiantes NO puedan subir documentos.
-- ============================================================

-- Eliminar DOCUMENTO_SUBIR del rol ESTUDIANTE
DELETE FROM roles_permisos
WHERE rol_id = (SELECT id FROM roles WHERE nombre = 'ESTUDIANTE')
  AND permiso_id = (SELECT id FROM permisos WHERE nombre = 'DOCUMENTO_SUBIR');
