# Tarea: Arreglar checkboxes y radios invisibles en rendir.html

## Diagnóstico

El CSS global en `components.css` (línea ~290) tiene esta regla:

```css
input[type="checkbox"] {
    position: absolute;
    opacity: 0;
    width: 0;
    height: 0;
}
```

Esto hace que TODOS los checkboxes y radios nativos del sitio sean invisibles y midan 0x0, porque el sistema espera que se use un `<span class="checkbox-custom">` visible en su lugar. Pero en `rendir.html` los checkboxes y radios se generan dinámicamente con JavaScript y **no tienen un `.checkbox-custom` asociado**, por lo que son invisibles e imposibles de clicar.

---

## Tarea 1: Reemplazar el bloque `<style>` en rendir.html

**Archivo**: `src/main/resources/templates/evaluaciones/rendir.html`

**Acción**: Buscar el bloque `<style>` actual que está justo después de la apertura de `<body>` (líneas ~6-15 aprox) y reemplazarlo completamente.

**Bloque actual** (debe desaparecer):

```html
    <style>
        /* Sobrescribir CSS global que oculta checkboxes/radios (components.css:272) */
        .opciones-multiple input[type="checkbox"],
        .opciones-multiple input[type="radio"],
        div[style*="flex-direction:column"] input[type="checkbox"],
        div[style*="flex-direction:column"] input[type="radio"] {
            position: static !important;
            opacity: 1 !important;
            width: 1rem !important;
            height: 1rem !important;
        }
    </style>
```

**Bloque nuevo** (reemplazar por esto):

```html
    <style>
        /* ANULAR checkbox estilizado de components.css SOLO para rendir.html
           components.css:272 tiene: input[type="checkbox"] { position:absolute; opacity:0; width:0; height:0; }
           Esto hace que los checkboxes midan 0x0 y no se puedan clicar.
           Con #rendir-form y !important forzamos la visibilidad. */
        #rendir-form input[type="checkbox"],
        #rendir-form input[type="radio"],
        #preguntas-container input[type="checkbox"],
        #preguntas-container input[type="radio"] {
            position: relative !important;
            opacity: 1 !important;
            width: 1rem !important;
            height: 1rem !important;
            min-width: 1rem !important;
            min-height: 1rem !important;
        }
        #rendir-form label,
        #preguntas-container label {
            cursor: pointer;
        }
    </style>
```

✅ **Verificar**: Después del cambio, abre el archivo y confirma que el bloque `<style>` sea exactamente el nuevo.

---

## Tarea 2: Arreglar style inline de renderOpcionUnica()

**Archivo**: `src/main/resources/templates/evaluaciones/rendir.html`

**Acción**: Buscar la función `renderOpcionUnica` en el JavaScript y modificar el `style` del `<input type="radio">`.

**Buscar esta línea exacta**:

```
'<input type="radio" name="' + name + '" value="' + escapeHtml(val) + '" style="accent-color:var(--color-accento);width:1rem;height:1rem;">' +
```

**Reemplazar por**:

```
'<input type="radio" name="' + name + '" value="' + escapeHtml(val) + '" style="accent-color:var(--color-accento);width:1rem!important;height:1rem!important;min-width:1rem!important;min-height:1rem!important;opacity:1!important;position:relative!important;">' +
```

✅ **Verificar**: Solo se modificó el `style` del input, nada más en la función.

---

## Tarea 3: Arreglar style inline de renderOpcionMultiple()

**Archivo**: `src/main/resources/templates/evaluaciones/rendir.html`

**Acción**: Buscar la función `renderOpcionMultiple` y modificar el `style` del `<input type="checkbox">`.

**Buscar esta línea exacta**:

```
'<input type="checkbox" name="p_' + p.id + '" value="' + escapeHtml(val) + '" style="accent-color:var(--color-accento);width:1rem;height:1rem;position:static!important;opacity:1!important;">' +
```

**Reemplazar por**:

```
'<input type="checkbox" name="p_' + p.id + '" value="' + escapeHtml(val) + '" style="accent-color:var(--color-accento);width:1rem!important;height:1rem!important;min-width:1rem!important;min-height:1rem!important;opacity:1!important;position:relative!important;">' +
```

✅ **Verificar**: Solo se modificó el `style` del input. La línea de `renderOpcionUnica` y `renderOpcionMultiple` ahora tienen styles idénticos (excepto `type="radio"` vs `type="checkbox"`).

---

## Tarea 4: Arreglar styles inline de renderVerdaderoFalso()

**Archivo**: `src/main/resources/templates/evaluaciones/rendir.html`

**Acción**: Buscar la función `renderVerdaderoFalso`. Tiene DOS inputs (`value="true"` y `value="false"`). Ambos deben modificarse.

**Buscar la línea con** `value="true"`:

```
'<input type="radio" name="' + name + '" value="true" style="accent-color:var(--color-success);width:1rem;height:1rem;">' +
```

**Reemplazar por**:

```
'<input type="radio" name="' + name + '" value="true" style="accent-color:var(--color-success);width:1rem!important;height:1rem!important;min-width:1rem!important;min-height:1rem!important;opacity:1!important;position:relative!important;">' +
```

**Buscar la línea con** `value="false"`:

```
'<input type="radio" name="' + name + '" value="false" style="accent-color:var(--color-danger);width:1rem;height:1rem;">' +
```

**Reemplazar por**:

```
'<input type="radio" name="' + name + '" value="false" style="accent-color:var(--color-danger);width:1rem!important;height:1rem!important;min-width:1rem!important;min-height:1rem!important;opacity:1!important;position:relative!important;">' +
```

✅ **Verificar**: Ambos inputs de Verdadero/Falso tienen `!important` en width, height, min-width, min-height, opacity y position.

---

## Resumen de cambios (verificar al final)

| # | Qué cambió | Dónde |
|---|---|---|
| 1 | Bloque `<style>` reemplazado | En `<body>`, justo después del `body` tag |
| 2 | Style del input radio en `renderOpcionUnica()` | Una línea |
| 3 | Style del input checkbox en `renderOpcionMultiple()` | Una línea |
| 4 | Style del input radio (true) en `renderVerdaderoFalso()` | Una línea |
| 5 | Style del input radio (false) en `renderVerdaderoFalso()` | Una línea |

**Regla de oro**: No pasar a la Tarea 2 hasta que la Tarea 1 esté 100% verificada.
