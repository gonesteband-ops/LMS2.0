# Contrato de Mejora: Bug #004

> Generado del reporte `bugs/reportes/bug-004.md`

---

## 1. Errores detectados

### Error 1 — Inscribir usuario individual da 403 + ERR_EMPTY_RESPONSE
POST a `/api/v1/cursos/{cursoId}/inscribir` devuelve 403 y el servidor corta la conexion. El `@PreAuthorize` usa `hasAuthority('INSCRIPCION_CREAR')` que puede no coincidir con las authorities del admin.

### Error 2 — Carga masiva CSV no encuentra RUTs
`inscribirMasivo()` intenta `UUID.fromString(linea)` para cada linea del CSV. Los RUTs como `11111111-1` no son UUIDs validos, fallan y se marcan como "no encontrados". El metodo NO reusa `resolverIdentificador()` que ya soporta RUT, email y UUID.

### Error 3 — Sin plantilla CSV descargable
El usuario no tiene un formato de referencia para saber que columnas usar en el CSV.

---

## 2. Archivos y cambios quirurgicos

| Archivo | Lineas | Cambio |
|---------|--------|--------|
| `InscripcionController.java` | 48 | Cambiar `@PreAuthorize("hasAuthority(...)")` por `hasAnyRole('ADMIN','INSTRUCTOR','SUPERADMIN')` |
| `InscripcionController.java` | 47-58 | Agregar try-catch para evitar ERR_EMPTY_RESPONSE |
| `InscripcionService.java` | 247-264 | Reemplazar logica de resolucion por llamada a `resolverIdentificador()` |
| `InscripcionService.java` | — | Agregar metodo `generarPlantillaCSV()` |
| `InscripcionController.java` | — | Agregar endpoint GET para descargar plantilla CSV |
| `participantes.html` | — | Agregar boton "Descargar plantilla CSV" y mejorar formato de nombre de archivo |

---

## 3. Tareas detalladas

### Paso 1 — Fix @PreAuthorize en InscripcionController.java
**Ruta:** `src/main/java/com/lms/controller/InscripcionController.java`

**Linea 48:** Cambiar:
```java
@PreAuthorize("hasAuthority('INSCRIPCION_CREAR')")
```
A:
```java
@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'SUPERADMIN')")
```

### Paso 2 — Agregar try-catch en endpoint inscribir
**Ruta:** `src/main/java/com/lms/controller/InscripcionController.java`
**Lineas 47-58:** Agregar bloque try-catch:
```java
@PostMapping("/cursos/{cursoId}/inscribir")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'SUPERADMIN')")
public ResponseEntity<?> inscribir(
        @PathVariable UUID cursoId,
        @Valid @RequestBody InscripcionRequest request,
        HttpServletRequest httpRequest) {
    try {
        UUID usuarioId = inscripcionService.resolverIdentificador(request.usuarioId());
        String ipCliente = httpRequest.getRemoteAddr();
        LocalDateTime timestamp = LocalDateTime.now();
        return ResponseEntity.ok(inscripcionService.inscribirUsuarioConFirma(
                cursoId, usuarioId, request.declaracionJuradaAceptada(), ipCliente, timestamp));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
```
Nota: cambiar tipo de retorno a `ResponseEntity<?>` para permitir error en vez de solo success.

### Paso 3 — Fix carga masiva para soportar RUT
**Ruta:** `src/main/java/com/lms/service/InscripcionService.java`
**Lineas 247-264:** Reemplazar el bloque de resolucion por:

```java
List<String> lineas = reader.lines()
        .map(String::trim)
        .filter(l -> !l.isEmpty() && !l.startsWith("#") && !l.startsWith("email") && !l.startsWith("usuario"))
        .collect(Collectors.toList());

for (String linea : lineas) {
    try {
        UUID usuarioId = resolverIdentificador(linea);
        InscripcionResponse response = inscribirUsuario(cursoId, usuarioId);
        exitosos.add(response);
    } catch (IllegalStateException e) {
        yaInscritos.add(linea);
    } catch (IllegalArgumentException e) {
        noEncontrados.add(linea);
    } catch (Exception e) {
        errores.add(linea);
    }
}
```

### Paso 4 — Agregar endpoint para descargar plantilla CSV
**Ruta:** `src/main/java/com/lms/controller/InscripcionController.java`

Agregar nuevo endpoint:
```java
@GetMapping("/plantilla-inscripcion")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<Resource> descargarPlantilla() {
    String csv = "usuarioId\n16.478.280-8\n12345678-9\n";
    byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
    ByteArrayResource resource = new ByteArrayResource(bytes);
    return ResponseEntity.ok()
            .header("Content-Type", "text/csv;charset=UTF-8")
            .header("Content-Disposition", "attachment; filename=plantilla-inscripcion.csv")
            .body(resource);
}
```

### Paso 5 — Agregar boton y mejorar UX en participantes.html
**Ruta:** `src/main/resources/templates/cursos/participantes.html`

Agregar boton "Descargar plantilla CSV" junto al area de drag-and-drop:
```html
<a href="/api/v1/plantilla-inscripcion" class="btn btn-secondary" style="margin-top:var(--spacing-sm);">
    Descargar plantilla CSV
</a>
```

Mejorar formato del nombre de archivo seleccionado (agregar negrita y tamano):
En el JavaScript que maneja el cambio de archivo, cambiar el innerHTML para que el nombre sea:
```html
<span style="font-weight:700;font-size:1.1rem;">{nombre-archivo}</span>
```

---

## 4. Criterios de aceptacion

- [ ] POST `/api/v1/cursos/{id}/inscribir` funciona con admin sin 403
- [ ] POST `/api/v1/cursos/{id}/inscribir` no crashea (no ERR_EMPTY_RESPONSE)
- [ ] Carga masiva CSV con RUTs funciona correctamente (encuentra usuarios)
- [ ] Carga masiva CSV con UUIDs sigue funcionando
- [ ] Carga masiva CSV con emails sigue funcionando
- [ ] Hay un boton "Descargar plantilla CSV" que descarga un archivo valido
- [ ] El nombre del archivo seleccionado se ve en negrita y mas grande
- [ ] `mvn compile -DskipTests` sin errores
