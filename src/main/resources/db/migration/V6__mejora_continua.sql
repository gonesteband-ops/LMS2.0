-- ============================================================
-- MigraciÃƒÂ³n V6: Mejora Continua Ã¢â‚¬â€ Inscripciones, Documentos por Curso,
--                Fecha LÃƒÂ­mite, CÃƒÂ³digos AlfanumÃƒÂ©ricos
-- Cumplimiento: Ley 19.799, Ley 21.719, Event Sourcing
-- ============================================================

-- ============================================================
-- 1. TABLA: inscripciones (participaciÃƒÂ³n de usuarios en cursos)
-- ============================================================
CREATE TABLE IF NOT EXISTS inscripciones (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    curso_id UUID NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    fecha_inscripcion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_limite TIMESTAMP,
    completado BOOLEAN NOT NULL DEFAULT FALSE,
    nota DOUBLE PRECISION,
    fecha_completado TIMESTAMP,
    hash_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    reason TEXT,
    UNIQUE(curso_id, usuario_id, deleted_at)
);

CREATE INDEX IF NOT EXISTS idx_inscripcion_curso ON inscripciones(curso_id);
CREATE INDEX IF NOT EXISTS idx_inscripcion_usuario ON inscripciones(usuario_id);
CREATE INDEX IF NOT EXISTS idx_inscripcion_usuario_activa ON inscripciones(usuario_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_inscripcion_completado ON inscripciones(completado);
CREATE INDEX IF NOT EXISTS idx_inscripcion_fecha_limite ON inscripciones(fecha_limite);

COMMENT ON TABLE inscripciones IS 'Registro de participantes inscritos en cursos (Ley 19.799 trazabilidad)';
COMMENT ON COLUMN inscripciones.completado IS 'Indica si el usuario completÃƒÂ³ el curso';
COMMENT ON COLUMN inscripciones.nota IS 'Nota obtenida en la evaluaciÃƒÂ³n del curso';
COMMENT ON COLUMN inscripciones.fecha_limite IS 'Fecha lÃƒÂ­mite para completar el curso (por usuario o por curso)';

-- ============================================================
-- 2. TABLA: cursos_documentos (relaciÃƒÂ³n N:M entre cursos y documentos)
-- ============================================================
CREATE TABLE IF NOT EXISTS cursos_documentos (
    curso_id UUID NOT NULL REFERENCES cursos(id) ON DELETE CASCADE,
    documento_id UUID NOT NULL REFERENCES documentos(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (curso_id, documento_id)
);

CREATE INDEX IF NOT EXISTS idx_cursos_docs_documento ON cursos_documentos(documento_id);

COMMENT ON TABLE cursos_documentos IS 'AsociaciÃƒÂ³n muchos-a-muchos entre cursos y documentos';

-- ============================================================
-- 3. AGREGAR fecha_limite a cursos
-- ============================================================
ALTER TABLE cursos
    ADD COLUMN IF NOT EXISTS fecha_limite TIMESTAMP;

COMMENT ON COLUMN cursos.fecha_limite IS 'Fecha lÃƒÂ­mite por defecto para completar el curso';

-- ============================================================
-- 4. AGREGAR codigo alfanumÃƒÂ©rico a evaluaciones (EVA00+)
-- ============================================================
ALTER TABLE evaluaciones
    ADD COLUMN IF NOT EXISTS codigo VARCHAR(20) UNIQUE;

COMMENT ON COLUMN evaluaciones.codigo IS 'CÃƒÂ³digo alfanumÃƒÂ©rico correlativo tipo EVA001, EVA002...';

-- ============================================================
-- 5. AGREGAR codigo alfanumÃƒÂ©rico a paquetes_scorm (SCO00+)
-- ============================================================
ALTER TABLE paquetes_scorm
    ADD COLUMN IF NOT EXISTS codigo VARCHAR(20) UNIQUE;

COMMENT ON COLUMN paquetes_scorm.codigo IS 'CÃƒÂ³digo alfanumÃƒÂ©rico correlativo tipo SCO001, SCO002...';

-- ============================================================
-- 6. AGREGAR codigo alfanumÃƒÂ©rico a documentos (DOC00+)
-- ============================================================
ALTER TABLE documentos
    ADD COLUMN IF NOT EXISTS codigo VARCHAR(20) UNIQUE;

COMMENT ON COLUMN documentos.codigo IS 'CÃƒÂ³digo alfanumÃƒÂ©rico correlativo tipo DOC001, DOC002...';

-- ============================================================
-- 7. TABLA: secuencias_codigos (control de correlatividad)
-- ============================================================
CREATE TABLE IF NOT EXISTS secuencias_codigos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    prefijo VARCHAR(5) NOT NULL UNIQUE,
    ultimo_numero INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE secuencias_codigos IS 'Control de secuencias para generaciÃƒÂ³n de cÃƒÂ³digos alfanumÃƒÂ©ricos';

-- Insertar secuencias iniciales
INSERT INTO secuencias_codigos (prefijo, ultimo_numero) VALUES
    ('EVA', 0),
    ('SCO', 0),
    ('DOC', 0)
ON CONFLICT (prefijo) DO NOTHING;

-- ============================================================
-- 8. FUNCIÃƒâ€œN: generar_codigo_alfanumerico(prefijo)
-- ============================================================
CREATE OR REPLACE FUNCTION generar_codigo_alfanumerico(p_prefijo VARCHAR)
RETURNS VARCHAR(20) AS $$
DECLARE
    v_numero INTEGER;
    v_codigo VARCHAR(20);
BEGIN
    -- Actualizar y obtener el siguiente nÃƒÂºmero de forma atÃƒÂ³mica
    UPDATE secuencias_codigos
    SET ultimo_numero = ultimo_numero + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE prefijo = p_prefijo
    RETURNING ultimo_numero INTO v_numero;

    -- Si no existe el prefijo, crearlo
    IF NOT FOUND THEN
        INSERT INTO secuencias_codigos (prefijo, ultimo_numero)
        VALUES (p_prefijo, 1)
        RETURNING ultimo_numero INTO v_numero;
    END IF;

    -- Formatear: PREFIJO + nÃƒÂºmero de 3 dÃƒÂ­gitos con padding
    v_codigo := p_prefijo || LPAD(v_numero::TEXT, 3, '0');

    RETURN v_codigo;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION generar_codigo_alfanumerico IS 'Genera cÃƒÂ³digo correlativo con formato PREFIJO + 3 dÃƒÂ­gitos (ej: EVA001)';

-- ============================================================
-- 9. ACTUALIZAR tabla documentos: agregar soft delete faltante
-- ============================================================
ALTER TABLE documentos
    ADD COLUMN IF NOT EXISTS deleted_by UUID,
    ADD COLUMN IF NOT EXISTS reason TEXT;

-- ============================================================
-- 10. ACTUALIZAR tabla evaluaciones: agregar hash_sha256 si no existe
-- ============================================================
-- Nota: hash_sha256 ya deberÃƒÂ­a existir en evaluaciones segÃƒÂºn V1, pero se agrega condicionalmente
ALTER TABLE evaluaciones
    ADD COLUMN IF NOT EXISTS hash_sha256 VARCHAR(64);

COMMENT ON COLUMN evaluaciones.hash_sha256 IS 'Hash SHA-256 para integridad del registro (Event Sourcing)';


