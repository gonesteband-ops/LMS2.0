-- ============================================================
-- Migración V13: Agregar fecha de nacimiento a usuarios
-- ============================================================
-- Ley 21.719: Control de menores de 16 años
-- ============================================================

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS fecha_nacimiento DATE;
