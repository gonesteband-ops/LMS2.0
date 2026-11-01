-- V32: Agregar duracion_estimada a cursos
ALTER TABLE cursos ADD COLUMN IF NOT EXISTS duracion_estimada INTEGER;
