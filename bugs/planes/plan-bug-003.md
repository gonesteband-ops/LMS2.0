# Contrato de Mejora: Bug #003

> Generado del reporte `bugs/reportes/bug-003.md`

---

## 1. Resumen del error

Todos los botones HTMX del modulo ARCOP fallan con 403 Forbidden porque el token CSRF no se envia. Ademas, la UX es deficiente (datos en JSON plano) y la logica de negocio de eliminacion/bloqueo no cumple Ley 21.719.

---

## 2. Archivos y cambios quirurgicos

| Archivo | Cambio |
|---------|--------|
| `head.html:44` | Agregar handler htmx:configRequest para CSRF |
| `arcop.html:17-18` | Reemplazar <pre> por <div> para tabla formateada |
| `arcop.html:34` | Agregar hx-confirm al boton de eliminacion |
| `ArcopService.java:89-95` | Anonimizar datos en solicitarSupresion() |
| `ArcopService.java:105-110` | Setear estadoTratamiento en bloquear() |
| `JwtAuthenticationFilter.java:75` | Validar bloqueo antes de autenticar |

---

## 3. Tareas detalladas

### 3.1 head.html — Handler CSRF (linea 44)

**Que:** Agregar despues de `htmx.config.globalIndicator`:
```javascript
document.body.addEventListener("htmx:configRequest",function(evt){if(evt.detail.verb==="get")return;var t=document.querySelector('meta[name="_csrf"]'),h=document.querySelector('meta[name="_csrf_header"]');if(!t||!h)return;var tk=t.getAttribute("content"),hd=h.getAttribute("content");if(tk&&hd)evt.detail.headers[hd]=tk;});
```

### 3.2 arcop.html — Ver mis datos (linea 17-18)

Cambiar:
```html
<button class="btn btn-primary" hx-get="/api/v1/arcop/acceso" hx-target="#acceso-data" hx-swap="innerHTML">Ver mis datos</button>
<pre id="acceso-data">...</pre>
```
A:
```html
<button class="btn btn-primary" hx-get="/api/v1/arcop/acceso" hx-target="#acceso-data" hx-swap="innerHTML">Ver mis datos</button>
<div id="acceso-data" style="font-size:var(--font-size-sm);"></div>
```

### 3.3 arcop.html — Confirmacion eliminacion (linea 34)

Cambiar:
```html
<button class="btn btn-danger" hx-delete="/api/v1/arcop/suprimir" hx-target="#supresion-msg">Solicitar eliminacion</button>
```
A:
```html
<button class="btn btn-danger" hx-delete="/api/v1/arcop/suprimir" hx-target="#supresion-msg" hx-confirm="Confirma la eliminacion de tu cuenta? Se anonimizaran tus datos personales. Tienes 30 dias para cancelar.">Solicitar eliminacion</button>
```

### 3.4 ArcopService.solicitarSupresion() — Anonimizar

En lugar de solo `setDeletedAt`, agregar anonimizacion:
```java
u.setNombreCompleto("Usuario Anonimizado");
u.setEmail("anon-" + usuarioId + "@deleted.local");
u.setRut(null);
u.setFechaNacimiento(null);
u.setEstadoTratamiento("ANONIMIZADO");
u.setBloqueadoHasta(LocalDateTime.now());
```

### 3.5 ArcopService.bloquear() — Estado tratamiento

Agregar: `u.setEstadoTratamiento("BLOQUEADO");`

### 3.6 JwtAuthenticationFilter — Validar bloqueo

Despues de `tokenValido` (linea 75), antes de setear autenticacion:
```java
var usuario = usuarioRepository.findByEmailAndDeletedAtIsNull(email);
if (usuario.isPresent() && usuario.get().getBloqueadoHasta() != null && usuario.get().getBloqueadoHasta().isAfter(java.time.LocalDateTime.now())) {
    log.warn("[JWT Filter] Usuario bloqueado hasta: {}", usuario.get().getBloqueadoHasta());
    filterChain.doFilter(request, response);
    return;
}
```
Requiere inyectar `UsuarioRepository` en el constructor.

---

## 4. Criterios de aceptacion

- [ ] Rectificar datos funciona sin 403
- [ ] Solicitar eliminacion pide confirmacion y funciona sin 403
- [ ] Bloquear cuenta funciona sin 403
- [ ] Ver mis datos muestra tabla (no JSON)
- [ ] Cuenta bloqueada no permite login
- [ ] mvn compile sin errores
