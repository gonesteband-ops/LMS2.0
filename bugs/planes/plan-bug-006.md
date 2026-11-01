# Contrato de Mejora: Bug #006

> Generado del reporte `bugs/reportes/bug-006.md`

---

## 1. Resumen

5 bugs en `/dashboard/privacidad`. Todos los cambios son en templates HTML y 1 controlador.

---

## 2. Archivos y cambios

| Archivo | Lineas | Bug | Cambio |
|---------|--------|-----|--------|
| `privacidad.html` | 61-78 | 1 | Reemplazar SVGs estaticos por dinamicos via JS |
| `privacidad.html` | 81-92 | 2 | Agregar fetch para cargar historial de consentimientos |
| `privacidad.html` | 91 | 3 | Cambiar texto boton "Gestionar consentimientos" |
| `privacidad.html` | 101-108 | 4 | Quitar disabled del JSON, conectar a /api/v1/arcop/portabilidad |
| `privacidad.html` | 127 | 5 | Quitar disabled, conectar a DELETE /api/v1/arcop/suprimir |
| `consentimiento.html` | 39 | 3 | Mostrar formulario con checkboxes pre-marcados si ya hay consentimientos |
| `HomeController.java` | 88-94 | 3 | Pasar estado de consentimientos al modelo |

---

## 3. Tareas detalladas

### Bug 1 â€” Finalidades dinamicas

**Archivo:** `privacidad.html`
**Lineas 61-78:** Reemplazar el `<ul>` estatico por un `<div id="finalidades-list">` que se llene via JS.

Agregar al final del body:
```javascript
fetch('/api/v1/consentimiento/mis-preferencias')
    .then(r => r.json())
    .then(data => {
        var html = '';
        var items = [
            { key: 'CUENTA', label: 'Crear y gestionar tu cuenta de usuario' },
            { key: 'MARKETING', label: 'Comunicaciones comerciales sobre cursos y promociones' },
            { key: 'ANALISIS', label: 'Mejorar la experiencia de usuario basado en uso' },
            { key: 'COMPARTIR_TERCEROS', label: 'Compartir datos con instituciones educativas asociadas' }
        ];
        items.forEach(function(item) {
            var activo = data[item.key];
            var icon = activo
                ? '<svg width="16" height="16" fill="none" stroke="#16a34a" viewBox="0 0 24 24" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5"/></svg>'
                : '<svg width="16" height="16" fill="none" stroke="#dc2626" viewBox="0 0 24 24" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>';
            html += '<li style="display:flex;align-items:center;gap:var(--spacing-sm);padding:var(--spacing-sm) var(--spacing-md);background:var(--bg-body);border-radius:var(--border-radius-lg);">'
                + icon + '<span><strong>' + item.key + ':</strong> ' + item.label + '</span></li>';
        });
        document.getElementById('finalidades-list').innerHTML = html;
    });
```

### Bug 2 â€” Consentimientos otorgados desde API

**Archivo:** `privacidad.html`
**Lineas 88-90:** Reemplazar el texto estatico por datos de la API.

Agregar en el mismo bloque JS del Bug 1:
```javascript
fetch('/api/v1/consentimiento/mis-preferencias')
    .then(r => r.json())
    .then(data => {
        var html = '<ul style="list-style:none;padding:0;margin:0;display:grid;gap:var(--spacing-sm);">';
        var items = ['CUENTA','MARKETING','ANALISIS','COMPARTIR_TERCEROS'];
        items.forEach(function(k) {
            var estado = data[k] ? 'Activo' : 'Revocado';
            var color = data[k] ? '#16a34a' : '#dc2626';
            html += '<li style="display:flex;justify-content:space-between;padding:var(--spacing-sm) var(--spacing-md);background:var(--bg-body);border-radius:var(--border-radius-lg);">'
                + '<span style="color:var(--text-secondary);">' + k + '</span>'
                + '<span style="color:' + color + ';font-weight:500;">' + estado + '</span></li>';
        });
        html += '</ul>';
        document.getElementById('consentimientos-list').innerHTML = html;
    });
```

### Bug 3 â€” Editar consentimientos

**Archivo:** `HomeController.java:88-94`

Ademas de `consentimientoOtorgado`, pasar al modelo el estado de cada finalidad:
```java
UUID userId = securityUtils.getCurrentUserId(authentication);
boolean otorgado = consentimientoService.haOtorgadoConsentimiento(userId, "CUENTA");
model.addAttribute("consentimientoOtorgado", otorgado);
// Pasar estado individual de cada finalidad
model.addAttribute("consentimientoCUENTA", consentimientoService.haOtorgadoConsentimiento(userId, "CUENTA"));
model.addAttribute("consentimientoMARKETING", consentimientoService.haOtorgadoConsentimiento(userId, "MARKETING"));
model.addAttribute("consentimientoANALISIS", consentimientoService.haOtorgadoConsentimiento(userId, "ANALISIS"));
model.addAttribute("consentimientoCOMPARTIR_TERCEROS", consentimientoService.haOtorgadoConsentimiento(userId, "COMPARTIR_TERCEROS"));
```

**Archivo:** `consentimiento.html:39`

Cambiar la condicion para que SIEMPRE muestre el formulario (incluso si ya otorgo consentimiento):
```html
th:if="${consentimientoOtorgado == null || !consentimientoOtorgado}"
```
Por:
```html
th:if="${true}"  <!-- Siempre mostrar el formulario para editar -->
```

Y en los checkboxes, agregar `th:checked` segun el estado:
```html
<input type="checkbox" name="finalidad_CUENTA" id="fin_CUENTA" value="true"
       th:checked="${consentimientoCUENTA}">
```

Hacer lo mismo para MARKETING, ANALISIS, COMPARTIR_TERCEROS.

**Archivo:** `privacidad.html:91`

Cambiar el texto del boton:
```html
Gestionar consentimientos
```
Por:
```html
Editar consentimientos
```

### Bug 4 â€” Portabilidad

**Archivo:** `privacidad.html:101-108`

Reemplazar los botones deshabilitados:
```html
<button class="btn btn-primary" style="justify-content:center;" disabled title="PrÃ³ximamente">Descargar JSON</button>
<button class="btn btn-secondary" style="justify-content:center;" disabled title="PrÃ³ximamente">Descargar CSV</button>
```
Por:
```html
<a href="/api/v1/arcop/portabilidad" class="btn btn-primary" style="justify-content:center;">Descargar JSON</a>
<button class="btn btn-secondary" style="justify-content:center;" disabled title="Proximamente">Descargar CSV</button>
```

### Bug 5 â€” Solicitar eliminacion

**Archivo:** `privacidad.html:127`

Reemplazar:
```html
<button class="btn btn-danger" style="justify-content:center;width:100%;" disabled title="PrÃ³ximamente">
    Solicitar eliminaciÃ³n de cuenta
</button>
```
Por:
```html
<button class="btn btn-danger" style="justify-content:center;width:100%;"
        hx-delete="/api/v1/arcop/suprimir"
        hx-confirm="Confirma la eliminacion de tu cuenta? Se anonimizaran tus datos personales. Tienes 30 dias para cancelar."
        hx-target="#eliminar-msg"
        hx-swap="innerHTML">
    Solicitar eliminaciÃ³n de cuenta
</button>
<div id="eliminar-msg"></div>
```

---

## 4. Criterios de aceptacion

- [ ] Finalidades del tratamiento muestran check verde o X roja segun lo que el usuario eligio
- [ ] Consentimientos otorgados muestra tabla con datos reales
- [ ] Boton "Editar consentimientos" lleva al formulario con checkboxes pre-marcados
- [ ] Descargar JSON descarga el archivo correctamente
- [ ] Solicitar eliminacion pide confirmacion y ejecuta sin 403
- [ ] `mvn compile -DskipTests` sin errores

---

## 5. Observaciones post-implementacion (22-Jul-2026)

### Obs 1 — Consentimientos otorgados no carga datos

**Sintoma:** La card "Consentimientos otorgados" se queda en "Cargando consentimientos..." indefinidamente.
El fetch a `/api/v1/consentimiento/mis-preferencias` puede fallar silenciosamente.

**Fix:** Agregar `.catch()` al fetch que muestre un mensaje de error en vez de "Cargando...".

**Archivo:** `privacidad.html:193-207`
Cambiar:
```javascript
fetch(...).then(...).then(function(d){...});
```
Por:
```javascript
fetch(...).then(function(r){if(!r.ok)throw Error(r.status);return r.json()}).then(function(d){...}).catch(function(e){
document.getElementById("consentimientos-list").innerHTML="<p style=\"color:var(--text-muted);font-size:var(--font-size-sm);\">Error al cargar consentimientos.</p>";
document.getElementById("finalidades-list").innerHTML="<p style=\"color:var(--text-muted);font-size:var(--font-size-sm);\">Error al cargar finalidades.</p>";
});
```

### Obs 2 — Portabilidad CSV deshabilitado

**Estado actual:** Boton "Descargar CSV" tiene `disabled`. No hay endpoint para CSV.
**Decision:** Mantener deshabilitado hasta que se implemente la conversion a CSV. No hay cambios.

### Obs 3 — Mensaje de eliminacion debe mostrar fecha exacta y boton de cancelar

**Sintoma:** Al hacer clic en "Solicitar eliminacion", el servidor devuelve JSON plano:
```json
{"mensaje":"Solicitud recibida...","status":"ok"}
```
Esto se muestra como texto JSON, no como un mensaje formateado.

**Fix:** Modificar el endpoint `DELETE /api/v1/arcop/suprimir` para que cuando el request tenga header `HX-Request: true`, devuelva HTML formateado con:
- Mensaje de exito
- Fecha exacta (hoy + 30 dias) calculada en el servidor
- Boton "Cancelar solicitud" que llame a `POST /api/v1/arcop/cancelar-supresion`

**Archivo:** `src/main/java/com/lms/controller/ArcopController.java`

Cambiar el metodo `suprimir()` para que cuando `hxRequest == true`, retorne HTML con la fecha.

**Archivo:** `src/main/resources/templates/dashboard/privacidad.html` (linea 181)
El `hx-target="#eliminar-msg"` reemplazara el contenido del div con el HTML del servidor.

