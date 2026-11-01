# ESPECIFICACIÓN MAESTRA DEL SISTEMA LMS (DOCUMENTO FINAL BLINDADO)

## 1. FUNDAMENTOS DE ARQUITECTURA Y SEGURIDAD
- **Identidad de Datos:** Prohibido el uso de RUT como Primary Key. Se utilizarán UUIDs para todas las entidades para asegurar el anonimato y la integridad de los datos.
- **Seguridad en Reposo/Tránsito:** Implementar AES-256 para datos en reposo y TLS 1.3 para tránsito.
- **Integridad (Hashing):** Todo archivo y registro debe incluir un Hash SHA-256 para detectar alteraciones. Implementar verificación de integridad periódica.
- **Infraestructura:** Diseño "API-First". Alta disponibilidad mediante Load Balancer, réplicas de base de datos y uso de almacenamiento de objetos (tipo S3/MinIO) para archivos físicos.

## 2. CUMPLIMIENTO LEGAL (LEY 19.799 Y 21.719)
- **Consentimiento Informado:** Flujo obligatorio en el primer login capturando: IP, User-Agent, Idioma, Hash del documento aceptado y versión de la política.
- **Firma Electrónica:** Evento vinculante que registra IP, Timestamp, User-Agent, sistema operativo y geolocalización aproximada (bajo consentimiento).
- **No Repudio:** El sistema implementa mecanismos técnicos (Hash encadenado, logs inmutables) para fortalecer la evidencia jurídica, garantizando la integridad de los datos.

## 3. TRAZABILIDAD E INMUTABILIDAD (EVENT SOURCING)
- **Política "No Borrar":** Se implementa Soft Delete (deleted_at, deleted_by, reason).
- **Event Sourcing:** Registro obligatorio de eventos mediante INSERT (prohibido UPDATE en logs). Eventos: Cambios de configuración, firmas, exportaciones, impersonaciones, asignaciones.
- **Versionado:** Todo activo (curso, documento, pregunta) debe mantener historial completo de versiones con sus propios hashes para auditoría.

## 4. GESTIÓN DE CONTENIDO Y EVALUACIONES
- **Repositorio:** Almacenamiento de archivos físicos en CDN/Object Storage; BD solo guarda metadatos (id, url, hash, mime, versión).
- **Evaluaciones:** Editor WYSIWYG, soporte multimedia, pesos editables y cálculo de tiempo de respuesta (ms).
- **Dashboards:** Exportación de XLSX profesional vía procesos en segundo plano (Redis/Celery) para no bloquear la interfaz.

## 5. DASHBOARDS Y OBSERVABILIDAD
- **Dashboard Ejecutivo:** Estilo minimalista, controlado por variables.css. Widgets dinámicos (KPIs, filtros, alertas de atraso).
- **Observabilidad:** Integración de OpenTelemetry/Prometheus/Grafana para métricas y logs en tiempo real.

## 6. ROADMAP Y EVOLUTIVOS (FASE 2)
1. **Gamificación:** Sistema de insignias (badges) al completar certificaciones.
2. **Notificaciones:** Sistema de disparadores automáticos (triggers) por correo (vencimientos, reprobación).

## 7. REGLAS DE ORO PARA ROO (AGENTE)
1. **IDIOMA:** 100% ESPAÑOL (Logs, UI, código, comentarios).
2. **ROLES (RBAC):** Definición granular de permisos (no solo por rol). Impersonación requiere motivo obligatorio, registro de IP, fecha y duración.
3. **SEGURIDAD:** MFA obligatorio para administradores, políticas de contraseña robustas y protección contra intentos fallidos.
4. **MODULARIDAD:** Código desacoplado; cambios en un módulo no deben afectar la integridad de otros.

## 8. Stack Tecnológico
- Backend: Java.
- Frontend: HTMX.
- Estilos: CSS Estático (para facilitar cambios de colores globales).
- Diseño Home: [Describe aquí los colores, ej: Primario #001f3f, Secundario #f4f4f4].

### Directrices de Diseño
- Mantener diseño minimalista y corporativo.
- Toda la configuración de estilos debe estar en archivos CSS estáticos separados, nunca hardcoded en el HTML.