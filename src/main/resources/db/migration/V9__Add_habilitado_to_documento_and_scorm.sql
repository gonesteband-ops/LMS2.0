-- Agrega columna habilitado a las tablas documentos y paquetes_scorm
-- para permitir habilitar/deshabilitar sin eliminar (soft-disable).
-- Por defecto TRUE para que los registros existentes queden habilitados.

ALTER TABLE documentos
    ADD COLUMN IF NOT EXISTS habilitado BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE paquetes_scorm
    ADD COLUMN IF NOT EXISTS habilitado BOOLEAN NOT NULL DEFAULT TRUE;

