-- =============================================================
-- Migration: V20__crear_modulo_bpo.sql
-- Módulo: Gestor de Contenidos BPO
-- Descripción: Tablas para cuentas, subservicios, contenidos BPO
--             y asignación usuario-cuenta.
-- Fecha: 2026-07-24
-- =============================================================

-- 1. Cuentas BPO (clientes/empresas)
CREATE TABLE IF NOT EXISTS cuentas_bpo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(150) NOT NULL UNIQUE,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

-- 2. Subservicios BPO (subdivisión de una cuenta)
CREATE TABLE IF NOT EXISTS subservicios_bpo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(150) NOT NULL,
    cuenta_id UUID NOT NULL REFERENCES cuentas_bpo(id),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE (nombre, cuenta_id)
);

-- 3. Contenidos BPO (procedimientos, noticias, ofertas)
CREATE TABLE IF NOT EXISTS contenidos_bpo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo VARCHAR(50) NOT NULL CHECK (tipo IN ('procedimiento', 'noticia', 'oferta')),
    titulo VARCHAR(255) NOT NULL,
    descripcion_corta TEXT,
    contenido_html TEXT NOT NULL,
    metadatos JSONB NOT NULL DEFAULT '{}',
    etiquetas JSONB NOT NULL DEFAULT '[]',
    estado VARCHAR(20) NOT NULL DEFAULT 'borrador'
        CHECK (estado IN ('borrador', 'publicado', 'archivado')),
    vigencia_desde TIMESTAMP,
    vigencia_hasta TIMESTAMP,
    cuenta_id UUID NOT NULL REFERENCES cuentas_bpo(id),
    subservicio_id UUID REFERENCES subservicios_bpo(id),
    creado_por UUID NOT NULL REFERENCES usuarios(id),
    actualizado_por UUID REFERENCES usuarios(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    deleted_by UUID REFERENCES usuarios(id),
    reason VARCHAR(500),
    CONSTRAINT vigencia_requerida_publicado
        CHECK (estado != 'publicado' OR (vigencia_desde IS NOT NULL AND vigencia_hasta IS NOT NULL))
);

CREATE INDEX IF NOT EXISTS idx_contenido_cuenta ON contenidos_bpo(cuenta_id);
CREATE INDEX IF NOT EXISTS idx_contenido_estado ON contenidos_bpo(estado);
CREATE INDEX IF NOT EXISTS idx_contenido_tipo ON contenidos_bpo(tipo);
CREATE INDEX IF NOT EXISTS idx_contenido_vigencia ON contenidos_bpo(vigencia_desde, vigencia_hasta);
CREATE INDEX IF NOT EXISTS idx_contenido_busqueda
    ON contenidos_bpo USING GIN (to_tsvector('spanish',
        COALESCE(titulo,'') || ' ' || COALESCE(descripcion_corta,'')));
CREATE INDEX IF NOT EXISTS idx_contenido_metadatos ON contenidos_bpo USING GIN (metadatos);

-- 4. Usuarios-Cuenta BPO (tabla puente con subservicio opcional)
CREATE TABLE IF NOT EXISTS usuarios_cuenta_bpo (
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    cuenta_id UUID NOT NULL REFERENCES cuentas_bpo(id),
    subservicio_id UUID REFERENCES subservicios_bpo(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL REFERENCES usuarios(id),
    PRIMARY KEY (usuario_id, cuenta_id)
);

-- 5. Permisos BPO (RBAC granular)
INSERT INTO permisos (id, nombre, recurso, accion, descripcion)
SELECT gen_random_uuid(), v.nombre, v.recurso, v.accion, v.descripcion
FROM (VALUES
    ('BPO_CONTENIDO_LEER', 'bpo.contenido', 'LEER', 'Leer contenidos BPO'),
    ('BPO_CONTENIDO_CREAR', 'bpo.contenido', 'CREAR', 'Crear contenidos BPO'),
    ('BPO_CONTENIDO_ACTUALIZAR', 'bpo.contenido', 'ACTUALIZAR', 'Actualizar contenidos BPO'),
    ('BPO_CONTENIDO_ELIMINAR', 'bpo.contenido', 'ELIMINAR', 'Eliminar contenidos BPO'),
    ('BPO_CONTENIDO_IMPORTAR', 'bpo.contenido', 'IMPORTAR', 'Importar contenidos vía API')
) AS v(nombre, recurso, accion, descripcion)
WHERE NOT EXISTS (SELECT 1 FROM permisos p WHERE p.nombre = v.nombre);
