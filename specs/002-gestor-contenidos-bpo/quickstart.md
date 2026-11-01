# Quickstart: Gestor de Contenidos BPO

**Propósito**: Escenarios rápidos de integración y prueba para el módulo Gestor de Contenidos BPO.

## Prerrequisitos

- LMS LegalTech en ejecución (Java 17 + PostgreSQL)
- Migraciones Flyway aplicadas (`V20260724__crear_modulo_bpo.sql`)
- Roles BPO creados en el sistema (vía `RolService`)
- Usuarios de prueba con asignaciones en `usuarios_cuenta_bpo`

## Escenario 1: Agente consulta contenidos (US1)

```bash
# 1. Iniciar sesión como agente BPO (rol: ROLE_BPO_AGENTE)
# 2. Navegar a /bpo/contenidos
# 3. Verificar que solo se muestran contenidos de la cuenta asignada
# 4. Usar la barra de búsqueda con "portabilidad"
# 5. Verificar que los resultados se filtran por cuenta y subservicio
```

**Prueba automatizada**:
```java
@Test
@WithMockUser(roles = "BPO_AGENTE")
void agenteSoloVeContenidosDeSuCuenta() {
    // asignar agente a cuenta "Cliente A"
    // crear contenido para "Cliente A" y otro para "Cliente B"
    // GET /bpo/contenidos
    // assert que solo ve contenido de Cliente A
}
```

## Escenario 2: Supervisor administra contenidos (US2)

```bash
# 1. Iniciar sesión como supervisor (rol: ROLE_BPO_SUPERVISOR)
# 2. Navegar a /admin/bpo/contenidos/nuevo
# 3. Completar formulario: tipo=procedimiento, título, contenido_html, cuenta=Cliente A
# 4. Guardar → contenido creado en estado "borrador"
# 5. Publicar contenido → estado cambia a "publicado"
# 6. Verificar auditoría: evento BPO_CONTENIDO_CREADO + BPO_CONTENIDO_CAMBIADO_ESTADO
```

**Prueba automatizada**:
```java
@Test
@WithMockUser(roles = "BPO_SUPERVISOR")
void supervisorCreaContenidoEnCuentaAsignada() {
    // POST /admin/bpo/contenidos con datos válidos
    // assert 201 Created
    // assert contenido tiene estado "borrador"
    // assert evento de auditoría creado
}

@Test
@WithMockUser(roles = "BPO_SUPERVISOR")
void supervisorNoPuedeCrearEnCuentaNoAsignada() {
    // POST /admin/bpo/contenidos con cuenta_id no asignada
    // assert 403 Forbidden
}
```

## Escenario 3: Búsqueda inteligente (US3)

```bash
# 1. Como agente de Cliente A, buscar "portabilidad"
# 2. Resultados: contenidos de Cliente A que contienen "portabilidad"
# 3. Filtrar por tipo "oferta" → solo ofertas de Cliente A
# 4. Buscar término sin resultados → mensaje "No se encontraron contenidos"
```

**Prueba automatizada**:
```java
@Test
@WithMockUser(roles = "BPO_AGENTE")
void busquedaFiltradaPorCuenta() {
    // GET /bpo/contenidos/lista?q=portabilidad
    // assert solo resultados de cuenta del agente
}
```

## Escenario 4: Tarjetas de resumen (US4)

```bash
# 1. Como agente, cargar /bpo/contenidos
# 2. Ver tres tarjetas superiores con contadores
# 3. "Procedimientos Vigentes": >0 si hay procedimientos publicados en vigencia
# 4. "Noticias Recientes": conteo de noticias de los últimos 7 días
# 5. "Ofertas Activas": ofertas publicadas en vigencia
```

**Prueba automatizada**:
```java
@Test
@WithMockUser(roles = "BPO_AGENTE")
void tarjetasResumenMuestranContadoresCorrectos() {
    // GET /bpo/contenidos/tarjetas
    // assert tarjetas con valores correctos para la cuenta
}
```

## Escenario 5: Ciclo de vida de estados

```bash
# 1. Supervisor crea contenido → estado "borrador"
# 2. Contenido en borrador: NO visible para agentes
# 3. Supervisor publica contenido → estado "publicado", requiere vigencia
# 4. Contenido publicado: visible para agentes
# 5. Supervisor archiva contenido → estado "archivado"
# 6. Contenido archivado: NO visible en consultas normales
```

## Escenario 6: Importación REST API (Fase 2)

```bash
curl -X POST /api/v1/bpo/contenidos/importar \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "tipo": "procedimiento",
    "titulo": "Test importación",
    "contenido_html": "<p>Contenido de prueba</p>",
    "cuenta_codigo": "CLIENTE_A"
  }'
```

## Verificación de Trazabilidad

```sql
-- Verificar eventos de auditoría del módulo BPO
SELECT tipo_evento, accion, recurso_id, metadata
FROM eventos_auditoria
WHERE recurso_tipo = 'BPO_CONTENIDO'
ORDER BY created_at DESC;

-- Verificar permisos BPO creados
SELECT * FROM permisos WHERE recurso = 'bpo.contenido';
```
