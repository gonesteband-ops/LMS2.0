-- V25: Agregar columna codigo a subservicios_bpo para URLs limpias

ALTER TABLE subservicios_bpo ADD COLUMN IF NOT EXISTS codigo VARCHAR(50);

-- Generar codigos a partir del nombre (ej: "Soporte Tecnico" -> "soporte-tecnico")
UPDATE subservicios_bpo SET codigo = LOWER(REGEXP_REPLACE(TRIM(nombre), '[^a-zA-Z0-9]+', '-', 'g')) WHERE codigo IS NULL;

-- Hacer codigo obligatorio
ALTER TABLE subservicios_bpo ALTER COLUMN codigo SET NOT NULL;

-- Crear indice unico por cuenta + codigo para evitar duplicados
CREATE UNIQUE INDEX IF NOT EXISTS idx_subservicios_codigo_cuenta ON subservicios_bpo(cuenta_id, codigo);
