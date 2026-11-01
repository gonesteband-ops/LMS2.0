# Contratos REST API: Gestor de Contenidos BPO

**Versión**: 1.0
**Base URL**: `/api/v1/bpo`
**Formato**: JSON
**Autenticación**: JWT (header `Authorization: Bearer <token>`)
**Permisos**: Validados mediante RBAC granular (`bpo.contenido.*`)

---

## Vistas HTMX (Interfaz de Usuario)

Las rutas de interfaz devuelven fragmentos HTML para HTMX. No son API REST.

| Método | Ruta | Descripción | Roles |
|--------|------|-------------|-------|
| GET | `/bpo/contenidos` | Página principal del Gestor de Contenidos | AGENTE, SUPERVISOR, ADMIN |
| GET | `/bpo/contenidos/tarjetas` | Fragmento: tarjetas de resumen | AGENTE, SUPERVISOR, ADMIN |
| GET | `/bpo/contenidos/lista` | Fragmento: listado de contenidos (filtrable) | AGENTE, SUPERVISOR, ADMIN |
| GET | `/bpo/contenidos/{id}` | Fragmento: detalle de contenido | AGENTE, SUPERVISOR, ADMIN |
| GET | `/admin/bpo/contenidos` | Página de administración de contenidos | SUPERVISOR, ADMIN |
| GET | `/admin/bpo/contenidos/nuevo` | Formulario: nuevo contenido | SUPERVISOR, ADMIN |
| POST | `/admin/bpo/contenidos` | Crear contenido | SUPERVISOR, ADMIN |
| GET | `/admin/bpo/contenidos/{id}/editar` | Formulario: editar contenido | SUPERVISOR, ADMIN |
| PUT | `/admin/bpo/contenidos/{id}` | Actualizar contenido | SUPERVISOR, ADMIN |
| DELETE | `/admin/bpo/contenidos/{id}` | Eliminar contenido (soft delete) | SUPERVISOR, ADMIN |
| POST | `/admin/bpo/contenidos/{id}/cambiar-estado` | Cambiar estado (borrador/publicado/archivado) | SUPERVISOR, ADMIN |

---

## Endpoints REST (Fase 2)

### POST /api/v1/bpo/contenidos/importar

Importar contenido desde sistema externo.

**Permiso requerido**: `BPO_CONTENIDO_IMPORTAR`

**Request Body**:
```json
{
    "tipo": "procedimiento",
    "titulo": "Procedimiento para portabilidad móvil",
    "descripcion_corta": "Pasos para realizar portabilidad numérica",
    "contenido_html": "<h2>Requisitos</h2><p>...</p>",
    "metadatos": {
        "version": 1,
        "departamento": "retencion"
    },
    "etiquetas": ["portabilidad", "movil", "retencion"],
    "vigencia_desde": "2026-07-24T00:00:00Z",
    "vigencia_hasta": "2026-12-31T23:59:59Z",
    "cuenta_codigo": "CLIENTE_A",
    "subservicio_nombre": "Cliente A Hogar"
}
```

**Response 201 Created**:
```json
{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "tipo": "procedimiento",
    "titulo": "Procedimiento para portabilidad móvil",
    "estado": "publicado",
    "cuenta_codigo": "CLIENTE_A",
    "created_at": "2026-07-24T12:00:00Z"
}
```

**Response 400 Bad Request** (validación):
```json
{
    "error": "VALIDACION",
    "mensaje": "Error de validación",
    "detalles": [
        {
            "campo": "tipo",
            "mensaje": "Tipo no válido. Valores aceptados: procedimiento, noticia, oferta"
        }
    ]
}
```

**Response 401 Unauthorized**:
```json
{
    "error": "NO_AUTORIZADO",
    "mensaje": "Token inválido o expirado"
}
```

**Response 403 Forbidden**:
```json
{
    "error": "PERMISO_DENEGADO",
    "mensaje": "No tienes permiso BPO_CONTENIDO_IMPORTAR"
}
```

---

### GET /api/v1/bpo/contenidos

Listar contenidos con filtros y paginación.

**Permiso requerido**: `BPO_CONTENIDO_LEER`
**Filtros automáticos**: Por cuenta del usuario autenticado (agentes) o cuentas asignadas (supervisores).

**Query Parameters**:
| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| q | string | Búsqueda por texto (título + descripción) |
| tipo | string | Filtrar por tipo (procedimiento, noticia, oferta) |
| cuenta_id | UUID | Filtrar por cuenta (solo supervisores/admin) |
| subservicio_id | UUID | Filtrar por subservicio |
| estado | string | Filtrar por estado (borrador, publicado, archivado) |
| incluir_vencidos | boolean | Si es true, incluye contenidos con vigencia vencida |
| page | int | Número de página (default: 0) |
| size | int | Tamaño de página (default: 20, max: 100) |

**Response 200 OK**:
```json
{
    "content": [
        {
            "id": "550e8400-e29b-41d4-a716-446655440000",
            "tipo": "procedimiento",
            "titulo": "Procedimiento para portabilidad móvil",
            "descripcion_corta": "Pasos para realizar portabilidad numérica",
            "estado": "publicado",
            "vigencia_desde": "2026-07-24T00:00:00Z",
            "vigencia_hasta": "2026-12-31T23:59:59Z",
            "cuenta_nombre": "Cliente A",
            "subservicio_nombre": "Cliente A Hogar",
            "created_at": "2026-07-24T12:00:00Z"
        }
    ],
    "page": 0,
    "size": 20,
    "total_elements": 1,
    "total_pages": 1
}
```

---

### GET /api/v1/bpo/contenidos/resumen

Obtener tarjetas de resumen con contadores.

**Permiso requerido**: `BPO_CONTENIDO_LEER`

**Response 200 OK**:
```json
{
    "procedimientos_vigentes": 15,
    "noticias_recientes": 3,
    "ofertas_activas": 7
}
```

---

## Tipos de Evento de Auditoría (Event Sourcing)

| Tipo de Evento | Acción | Disparado por |
|----------------|--------|--------------|
| `BPO_CONTENIDO_CREADO` | CREAR | Creación de contenido |
| `BPO_CONTENIDO_ACTUALIZADO` | ACTUALIZAR | Modificación de contenido |
| `BPO_CONTENIDO_ELIMINADO` | ELIMINAR | Soft delete de contenido |
| `BPO_CONTENIDO_CAMBIADO_ESTADO` | CAMBIAR_ESTADO | Transición de estado |
| `BPO_CONTENIDO_IMPORTADO` | IMPORTAR | Importación vía API |

**Metadata estándar para cada evento**:
```json
{
    "contenido_id": "UUID",
    "contenido_titulo": "string",
    "contenido_tipo": "string",
    "cuenta_id": "UUID",
    "ip_solicitante": "string"
}
```
