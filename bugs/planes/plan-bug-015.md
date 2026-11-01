# Plan BUG-015: Mejoras en admin del Gestor de Contenidos BPO

> Generado basado en `bugs/reportes/bug-015.md`.

---

## 1. Errores detectados

**Sintomas (que ve el usuario):**
- **Error A:** En `/gestores/{id}` no hay boton "+ Crear nueva carpeta" para agregar subcarpetas
- **Error B:** En `/gestores/{id}/subcarpetas/{subId}` cada publicacion solo tiene boton "Eliminar", falta "Editar"
- **Error C:** Al crear una publicacion con archivo adjunto, da error 403 Forbidden (CSRF)
- **Error D:** Las rutas usan UUIDs feas (`a1000000-0000-...`) en vez de codigos legibles (`GESTOR_EJEMPLO`)

**Causa raiz (que falla en el codigo):**
- **A:** El template `gestor-detalle.html` no incluye el boton y no existen endpoints ni formulario para crear subcarpetas
- **B:** El template `subcarpeta-publicaciones.html` solo tiene boton Eliminar, no hay Editar
- **C:** El formulario de publicacion usa `enctype="multipart/form-data"` y Spring Security rechaza el POST por falta de token CSRF en multipart
- **D:** Las plantillas generan URLs con `gestor.id` (UUID) en vez de `gestor.codigo`, y SubServicioBPO no tenia campo codigo

**Impacto (que funcionalidad se rompe):**
- **A:** El admin no puede organizar contenidos en subcarpetas dentro de un gestor
- **B:** El admin no puede corregir o actualizar una publicacion existente
- **C:** No se pueden crear publicaciones con archivos adjuntos
- **D:** URLs poco profesionales y dificiles de recordar/compartir

---

## 2. Archivos involucrados

| Archivo | Rol | Cambio |
|---------|-----|--------|
| `src/main/resources/templates/bpo/admin/gestor-detalle.html` | Template | Agregar boton "+ Crear nueva carpeta" + cambiar UUIDs por codigos |
| `src/main/resources/templates/bpo/admin/subcarpeta-publicaciones.html` | Template | Agregar boton "Editar" + cambiar UUIDs por codigos |
| `src/main/resources/templates/bpo/admin/formulario-publicacion.html` | Template | Agregar campo CSRF oculto + cambiar UUIDs por codigos |
| `src/main/resources/templates/bpo/admin/formulario-subcarpeta.html` | **NUEVO** | Template para crear subcarpeta |
| `src/main/resources/templates/bpo/admin/gestores.html` | Template | Cambiar `g.id` por `g.codigo` |
| `src/main/java/com/lms/controller/bpo/AdminBPOController.java` | Controller | Endpoints para nueva subcarpeta, editar publicacion, aceptar codigos |
| `src/main/java/com/lms/service/bpo/CuentaBPOService.java` | Service | Metodo `crearSubcarpeta(UUID, String)` |
| `src/main/java/com/lms/service/bpo/ContenidoBPOService.java` | Service | Metodo `actualizarPublicacion(UUID, DTO, UUID)` |
| `src/main/resources/db/migration/V25__agregar_codigo_subservicios_bpo.sql` | **NUEVO** | Migracion para columna codigo en subservicios_bpo |

---

## 3. Criterios de aceptacion (checklist)

- [ ] **AC1:** El boton "+ Crear nueva carpeta" aparece en `gestor-detalle.html` con clase `btn btn-primary`
- [ ] **AC2:** Al hacer clic en "+ Crear nueva carpeta" se abre formulario con campo "Nombre de la carpeta"
- [ ] **AC3:** Al guardar una subcarpeta, se redirige al detalle del gestor y la nueva subcarpeta aparece en la lista
- [ ] **AC4:** Cada publicacion en `subcarpeta-publicaciones.html` tiene botones "Editar" y "Eliminar"
- [ ] **AC5:** Al hacer clic en "Editar" se abre formulario con datos precargados
- [ ] **AC6:** Al actualizar una publicacion, los cambios se guardan y se redirige a la subcarpeta
- [ ] **AC7:** Crear publicacion con archivo adjunto funciona sin error 403
- [ ] **AC8:** Las rutas usan codigos legibles (`/gestores/GESTOR_EJEMPLO`) en vez de UUIDs
- [ ] **AC9:** Las rutas viejas con UUIDs siguen funcionando (fallback)
- [ ] **AC10:** `mvn compile -DskipTests` sin errores
- [ ] **AC11:** Todos los tests en `bugs/test/` pasan en verde

---

## 4. Tareas para el agente

### 4.1 Orden de ejecucion (NO CAMBIAR EL ORDEN)

Cada paso debe completarse y verificarse antes de pasar al siguiente:

### FASE 1: CSRF (Error C) - 1 archivo
- [ ] **Agregar token CSRF en `formulario-publicacion.html`** — Linea 22, dentro del `<form>` agregar: `<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />`

### FASE 2: Crear subcarpeta (Error A) - 3 archivos
- [ ] **Crear migracion Flyway V25** — Ver `especificacion V25` abajo
- [ ] **Agregar metodo `crearSubcarpeta` en `CuentaBPOService.java`** — Buscar cuenta por ID, crear SubServicioBPO con nombre, guardar
- [ ] **Crear endpoint GET `/gestores/{gestorId}/subcarpetas/nuevo` en `AdminBPOController.java`** — Mostrar formulario con objeto subcarpeta vacio
- [ ] **Crear endpoint POST `/gestores/{gestorId}/subcarpetas` en `AdminBPOController.java`** — Recibir nombre, crear subcarpeta, redirect
- [ ] **Crear template `formulario-subcarpeta.html`** — Card con campo "Nombre", botones Guardar/Cancelar
- [ ] **Agregar boton "+ Crear nueva carpeta" en `gestor-detalle.html`** — Cabecera, al lado de "Volver", con `btn btn-primary`

### FASE 3: Editar publicacion (Error B) - 3 archivos
- [ ] **Agregar metodo `actualizarPublicacion` en `ContenidoBPOService.java`** — Ver `especificacion actualizarPublicacion` abajo
- [ ] **Crear endpoint GET `/gestores/{gestorId}/subcarpetas/{subId}/publicaciones/{pubId}/editar` en `AdminBPOController.java`** — Precargar datos de la publicacion en el formulario
- [ ] **Crear endpoint POST `/gestores/{gestorId}/subcarpetas/{subId}/publicaciones/{pubId}` en `AdminBPOController.java`** — Recibir datos, actualizar, redirect
- [ ] **Agregar boton "Editar" en `subcarpeta-publicaciones.html`** — Al lado de "Eliminar", con clase `btn btn-sm btn-secondary`

### FASE 4: Rutas limpias (Error D) - ORDEN CRITICO
- [ ] **PASO 4.1:** Migracion V25 (ya creada en FASE 2)
- [ ] **PASO 4.2:** Actualizar SOLO plantillas para usar codigos (NO tocar controlador aun):
  - `gestores.html`: `g.id` → `g.codigo`
  - `gestor-detalle.html`: `${gestor.id}` y `${s.id}` → codigos
  - `subcarpeta-publicaciones.html`: `${gestor.id}` y `${subcarpeta.id}` → codigos
  - `formulario-publicacion.html`: `${gestor.id}` y `${subcarpeta.id}` → codigos
- [ ] **PASO 4.3:** Compilar y probar (controlador aun acepta UUIDs → todo funciona)
- [ ] **PASO 4.4:** Actualizar controlador: `@PathVariable UUID` → `@PathVariable String` con fallback a UUID
- [ ] **PASO 4.5:** Compilar y probar (URLs viejas con UUIDs aun funcionan)

---

## 5. Especificaciones tecnicas detalladas

### Especificacion V25: Migracion Flyway
```sql
ALTER TABLE subservicios_bpo ADD COLUMN IF NOT EXISTS codigo VARCHAR(50);
UPDATE subservicios_bpo SET codigo = LOWER(REGEXP_REPLACE(TRIM(nombre), '[^a-zA-Z0-9]+', '-', 'g')) WHERE codigo IS NULL;
ALTER TABLE subservicios_bpo ALTER COLUMN codigo SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_subservicios_codigo_cuenta ON subservicios_bpo(cuenta_id, codigo);
```

### Especificacion crearSubcarpeta en CuentaBPOService
```java
@Transactional
public SubServicioBPO crearSubcarpeta(UUID cuentaId, String nombre) {
    var cuenta = cuentaRepository.findById(cuentaId)
            .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));
    SubServicioBPO sub = SubServicioBPO.builder()
            .nombre(nombre)
            .cuenta(cuenta)
            .activo(true)
            .build();
    return subservicioRepository.save(sub);
}
```

### Especificacion actualizarPublicacion en ContenidoBPOService
```java
@Transactional
public ContenidoBPO actualizarPublicacion(UUID id, ContenidoBPOCreateDTO dto, UUID usuarioId) {
    ContenidoBPO contenido = contenidoRepository.findById(id)
            .orElseThrow(() -> new BPOContenidoNoEncontradoException("Publicacion no encontrada"));
    contenido.setTitulo(dto.getTitulo());
    contenido.setDescripcionCorta(dto.getDescripcionCorta());
    contenido.setContenidoHtml(dto.getContenidoHtml());
    if (dto.getArchivo() != null && !dto.getArchivo().isEmpty()) {
        var archivoInfo = fileUploadService.subirArchivo(dto.getArchivo(), "bpo");
        contenido.setArchivoUrl(archivoInfo.url());
        contenido.setArchivoNombre(archivoInfo.nombre());
        contenido.setArchivoMime(archivoInfo.mime());
        contenido.setArchivoTamano(archivoInfo.tamano());
    }
    contenido.setActualizadoPor(usuarioId);
    contenido.setUpdatedAt(LocalDateTime.now());
    ContenidoBPO guardado = contenidoRepository.save(contenido);
    auditLogger.registrarEvento("BPO_CONTENIDO_ACTUALIZADO", usuarioId, "ACTUALIZAR",
            "BPO_CONTENIDO", id, Map.of("titulo", dto.getTitulo()));
    return guardado;
}
```

---

## 6. Tests de validacion

Los tests estan en `bugs/test/` y deben ejecutarse con:
```powershell
cd <RUTA_DEL_PROYECTO>
powershell -File bugs/test/run-all-tests.ps1
```

**Tests existentes que deben seguir pasando:**
- `test-01-crear-cuenta.ps1` — Service: metodo crearCuenta
- `test-02-urls-thymeleaf.ps1` — URLs con sintaxis correcta
- `test-03-controller.ps1` — @ModelAttribute("publicacion")
- `test-04-botones.ps1` — Botones con btn-primary
- `test-05-formulario-publicacion.ps1` — Formulario correcto
- `test-06-funcional.ps1` — Endpoints HTTP 200 + estilos CSS

---

## 7. Evidencia de finalizacion

*El agente debe llenar esto al finalizar CADA FASE antes de pasar a la siguiente:*

```
### FASE 1 (CSRF) completada:
[ ] formulario-publicacion.html – linea 22, token CSRF agregado
[ ] Compilacion: BUILD SUCCESS
[ ] AC7 verificado: POST sin error 403

### FASE 2 (Crear subcarpeta) completada:
[ ] V25 migracion creada
[ ] CuentaBPOService.java – metodo crearSubcarpeta
[ ] AdminBPOController.java – GET y POST subcarpetas
[ ] formulario-subcarpeta.html – NUEVO template
[ ] gestor-detalle.html – boton "+ Crear nueva carpeta"
[ ] Compilacion: BUILD SUCCESS
[ ] AC1, AC2, AC3 verificados

### FASE 3 (Editar publicacion) completada:
[ ] ContenidoBPOService.java – metodo actualizarPublicacion
[ ] AdminBPOController.java – GET y POST editar
[ ] subcarpeta-publicaciones.html – boton "Editar"
[ ] Compilacion: BUILD SUCCESS
[ ] AC4, AC5, AC6 verificados

### FASE 4 (Rutas limpias) completada:
[ ] Plantillas actualizadas con codigos
[ ] Controlador actualizado con fallback
[ ] Compilacion: BUILD SUCCESS
[ ] AC8, AC9 verificados

### Tests finales:
[ ] run-all-tests.ps1 – 6/6 tests pasan
[ ] mvn compile -DskipTests – BUILD SUCCESS
[ ] AC10, AC11 verificados
```
