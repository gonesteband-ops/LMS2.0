# Contrato de Mejora: Bug #007

> Generado del reporte `bugs/reportes/bug-007.md`

---

## 1. Errores detectados

### Error 1 — Consentimientos no cargan en privacidad
El fetch a `/api/v1/consentimiento/mis-preferencias` falla. Causa probable: el endpoint requiere autenticacion y el JWT no se envia correctamente con fetch (falta `credentials: 'include'`).

### Error 2 — UX de /consentimiento sin diferenciar edicion de visualizacion
Al entrar a `/consentimiento` con consentimientos ya otorgados, los checkboxes se ven directamente. No hay una pantalla intermedia que permita elegir entre "solo ver" y "editar".

---

## 2. Archivos y cambios quirurgicos

| Archivo | Cambio |
|---------|--------|
| `privacidad.html:193` | Agregar `credentials: 'include'` al fetch de consentimientos |
| `privacidad.html:193` | Agregar log de error para diagnosticar fallos |
| `consentimiento.html:37-45` | Agregar pantalla de 3 botones si ya hay consentimientos |
| `consentimiento.html:72-114` | Mostrar checkboxes solo si se hace click en "Editar" |
| `HomeController.java:88-94` | Pasar bandera `mostrarFormulario` al modelo |

---

## 3. Tareas detalladas

### Paso 1 — Fix fetch en privacidad.html

**Ruta:** `src/main/resources/templates/dashboard/privacidad.html`
**Linea ~193:** Cambiar el fetch a:
```javascript
fetch("/api/v1/consentimiento/mis-preferencias", {credentials:"include"}).then(function(r){
```
Y en el catch, mostrar el error real:
```javascript
}).catch(function(e){
document.getElementById("consentimientos-list").innerHTML="<p style=\"color:#dc2626;\">Error: no se pudieron cargar los consentimientos. <button onclick=\"location.reload()\">Reintentar</button></p>";
document.getElementById("finalidades-list").innerHTML="<p style=\"color:var(--text-muted);\">Error al cargar.</p>";
});
```

### Paso 2 — UX de /consentimiento con 3 botones

**Ruta:** `src/main/resources/templates/auth/consentimiento.html`

Agregar una capa inicial (oculta por defecto si no hay consentimiento, visible si ya lo hay) con 3 botones:
- "Ir al Panel Principal" → /dashboard
- "Ver Mi Perfil" → /dashboard/perfil
- "Editar Preferencias de Privacidad" → oculta los botones y muestra los checkboxes

**Estructura:**
```html
<div id="consent-actions" th:if="${consentimientoOtorgado}">
    <div style="text-align:center;padding:var(--spacing-2xl);">
        <h2>Preferencias de Privacidad</h2>
        <p>Ya tienes tus preferencias guardadas. Puedes revisarlas o editarlas.</p>
        <div style="display:flex;flex-direction:column;gap:var(--spacing-sm);">
            <a href="/dashboard" class="btn btn-primary">Ir al Panel Principal</a>
            <a href="/dashboard/perfil" class="btn btn-secondary">Ver Mi Perfil</a>
            <button class="btn btn-accent" onclick="document.getElementById('consent-actions').style.display='none';document.getElementById('consent-form-container').style.display='block';">
                Editar Preferencias de Privacidad
            </button>
        </div>
    </div>
</div>
```

El formulario actual (`consent-form-container`) debe estar oculto inicialmente si ya hay consentimientos:
```html
<div id="consent-form-container" style="padding:var(--spacing-2xl);display:none;"
     th:unless="${consentimientoOtorgado}">
```

### Paso 3 — HomeController

No requiere cambios (ya pasa `consentimientoOtorgado` al modelo).

---

## 4. Criterios de aceptacion

- [ ] Finalidades del tratamiento muestran check ✅ / X ❌ segun estado real
- [ ] Consentimientos otorgados muestra tabla con datos (no error)
- [ ] Al entrar a /consentimiento con consentimientos dados, se ven 3 botones
- [ ] Click en "Editar Preferencias de Privacidad" muestra los checkboxes
- [ ] `mvn compile -DskipTests` sin errores
