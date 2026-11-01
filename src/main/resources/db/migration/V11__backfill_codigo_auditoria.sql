--
-- V11: Backfill codigo alfanumérico en metadata de eventos_auditoria
--
-- Agrega el campo "codigo" (DOC001, SCO001, EVA001) a eventos antiguos
-- que fueron registrados antes de incluir codigo en la metadata.
--
-- El trigger trg_prevent_event_update bloquea UPDATE en eventos_auditoria,
-- por lo que se desactiva temporalmente durante la migración.
--

DROP TRIGGER IF EXISTS trg_prevent_event_update ON eventos_auditoria;

-- 1. Documentos
UPDATE eventos_auditoria ea
SET metadata = ea.metadata || jsonb_build_object('codigo', d.codigo)
FROM documentos d
WHERE ea.recurso_tipo = 'DOCUMENTO'
  AND ea.recurso_id = d.id
  AND ea.metadata->>'codigo' IS NULL;

-- 2. Paquetes SCORM
UPDATE eventos_auditoria ea
SET metadata = ea.metadata || jsonb_build_object('codigo', p.codigo)
FROM paquetes_scorm p
WHERE ea.recurso_tipo = 'PAQUETE_SCORM'
  AND ea.recurso_id = p.id
  AND ea.metadata->>'codigo' IS NULL;

-- 3. Evaluaciones
UPDATE eventos_auditoria ea
SET metadata = ea.metadata || jsonb_build_object('codigo', e.codigo)
FROM evaluaciones e
WHERE ea.recurso_tipo = 'EVALUACION'
  AND ea.recurso_id = e.id
  AND ea.metadata->>'codigo' IS NULL;

-- Recrear el trigger de protección
CREATE TRIGGER trg_prevent_event_update
    BEFORE UPDATE ON eventos_auditoria
    FOR EACH ROW EXECUTE FUNCTION prevent_event_mutation();
