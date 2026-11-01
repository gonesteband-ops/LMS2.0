-- V29: Seed prefijo CER para certificaciones

INSERT INTO secuencias_codigos (prefijo, ultimo_numero)
VALUES ('CER', 1)
ON CONFLICT (prefijo) DO NOTHING;
