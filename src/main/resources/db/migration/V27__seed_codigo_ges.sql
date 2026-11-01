-- V27: Seed prefijo GES para gestores de contenido BPO

INSERT INTO secuencias_codigos (prefijo, ultimo_numero)
VALUES ('GES', 1)
ON CONFLICT (prefijo) DO NOTHING;
