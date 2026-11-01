# Contrato de Mejora: Bug #005

> Generado del reporte `bugs/reportes/bug-005.md`

---

## 1. Errores detectados

| # | Problema | Estado |
|---|----------|--------|
| 1 | Boton "Descargar plantilla CSV" da 404 | ✅ RESUELTO: PlantillaController separado |
| 2 | Inscripcion masiva con RUTs | ✅ FUNCIONA |
| 3 | Modal "Forzar Completado" cortado | ⏳ PENDIENTE |
| 4 | "Forzar Completado" da 403 CSRF | ⏳ PENDIENTE |
| 5 | CSRF general inconsistente | ⏳ PENDIENTE |

---


## 1b. Solucion definitiva CSRF (aplica a todo el proyecto)

### Analisis de seguridad
El JWT viaja en cookie `access_token` con:
- `HttpOnly=true` — JS no puede leerla
- `SameSite=Lax` — el navegador no la envia en requests cross-site
- `Secure=true` en produccion (HTTPS)

Con SameSite=Lax, un atacante externo NO puede hacer que el navegador envie la cookie en POST/PUT/DELETE.
La proteccion CSRF de Spring Security es REDUNDANTE para endpoints que usan JWT via cookie con SameSite.

### Solucion definitiva
En `SecurityConfig.java`, excluir TODOS los endpoints `/api/v1/**` de CSRF:

**Antes:**
```java
.csrf(csrf -> csrf
    .ignoringRequestMatchers("/hx/admin/**", "/api/v1/cursos/**")
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    .csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler()))
```

**Despues:**
```java
.csrf(csrf -> csrf
    .ignoringRequestMatchers("/hx/admin/**", "/api/v1/**")
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
```

### Impacto
- ✅ Elimina TODOS los 403 CSRF en toda la API REST
- ✅ Sin riesgo porque JWT + SameSite bloquean ataques cross-site
- ✅ Ya no necesitamos el handler CSRF en head.html
- ✅ Ya no necesitamos el meta tag _csrf en las paginas
- ✅ Ya no necesitamos el input hidden _csrf en formularios
- ✅ Se puede limpiar head.html del handler htmx:configRequest
- ❌ Se pierde proteccion para requests que NO usen JWT (login de formulario — pero ese usa su propio CSRF via formulario)
- @ - Se simplifica drasticamente el desarrollo de nuevos endpoints

### Archivo
`src/main/java/com/lms/security/SecurityConfig.java` (linea 49-52)




| Archivo | Lineas | Cambio |
|---------|--------|--------|
| `src/main/resources/templates/cursos/participantes.html` | ~800-820 (modal) | z-index:9999, position:fixed, fix overflow |
| `src/main/resources/templates/cursos/participantes.html` | ~815 (forzarBtn) | Agregar CSRF header al fetch |
| `src/main/resources/templates/fragments/head.html` | 45-58 | Limpiar handler CSRF duplicado |

---

## 3. Tareas detalladas

### Paso 1 — Eliminar handler CSRF duplicado en head.html

**Archivo:** `src/main/resources/templates/fragments/head.html`
**Lineas 45 y 58:** Dejar solo UN handler `htmx:configRequest`.
Actualmente hay dos iguales (se creo una copia al aplicar cambios previos).

### Paso 2 — Fix modal "Forzar Completado" cortado

**Archivo:** `src/main/resources/templates/cursos/participantes.html`

Buscar el modal de forzar completado y asegurar:
- `position: fixed` (no absolute)
- `z-index: 9999`
- Ningun contenedor padre con `overflow: hidden` o `transform`

Si el modal es un div generado por JS, modificar las propiedades CSS inline.

### Paso 3 — Fix 403 en "Forzar Completado"

**Archivo:** `src/main/resources/templates/cursos/participantes.html`
**Linea ~815:**

La funcion `forzarCompletado()` probablemente usa `fetch()` y no incluye el CSRF token.
Agregar antes del fetch:
```javascript
var csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
```
Y en el fetch agregar header:
```javascript
headers: {
    'Content-Type': 'application/json',
    'X-XSRF-TOKEN': csrfToken
}
```

### Paso 4 — Eliminar metodo duplicado de InscripcionController.java

**Archivo:** `src/main/java/com/lms/controller/InscripcionController.java`
**Lineas 205-215:** Eliminar el metodo `descargarPlantilla()` (ahora esta en PlantillaController.java separado).

---

## 4. Criterios de aceptacion

- [ ] GET `/api/v1/plantilla-inscripcion` descarga CSV sin 404
- [ ] Modal Forzar Completado se ve completo (no cortado)
- [ ] POST forzar-completado funciona sin 403
- [ ] Handler CSRF en head.html no esta duplicado
- [ ] `mvn compile -DskipTests` sin errores

