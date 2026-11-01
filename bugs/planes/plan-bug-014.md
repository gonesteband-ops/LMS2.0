# Plan BUG-014: Implementacion de correcciones en admin de Gestor de Contenidos BPO

> Generado automaticamente basado en el reporte `bugs/reportes/bug-014.md`.

---

## 1. Error detectado

**Sintoma (que ve el usuario):**
Al usar la seccion admin de Gestores de Contenido:
- "/gestores/nuevo" muestra pagina en blanco
- El boton "Volver" tiene estilo diferente al resto de la app
- "+ Nueva Publicacion" da error 500
- Las publicaciones se ven como texto plano, no como tarjetas
- El formulario de nueva publicacion no funciona

**Causa raiz (que falla en el codigo):**
1. El controlador no agrega objeto `gestor` al modelo para el formulario nuevo, ademas llama a `crearCuenta` que no existe en el Service
2. Las URLs Thymeleaf usan `${}` dentro de `@{}` en vez de la sintaxis de variables `{var}(var=${})` en 4 lugares
3. El `@ModelAttribute` en el POST de publicacion usa el nombre por defecto en vez de "publicacion"
4. Faltan estilos CSS consistentes (btn-primary en vez de btn-ghost / bpo-btn)
5. Las publicaciones no tienen estructura de tarjeta ni vista de detalle

**Impacto (que funcionalidad se rompe):**
- No se pueden crear nuevos gestores
- No se pueden crear nuevas publicaciones
- La navegacion y la experiencia visual es inconsistente
- El alumno no puede ver los contenidos correctamente

---

## 2. Archivos involucrados

| Archivo | Rol | Lineas clave |
|---------|-----|--------------|
| `src/main/java/com/lms/controller/bpo/AdminBPOController.java` | Controller | 49-54, 56-60, 98-105 |
| `src/main/java/com/lms/service/bpo/CuentaBPOService.java` | Service | 30-77 (falta metodo) |
| `src/main/resources/templates/bpo/admin/formulario-gestor.html` | Template | 19 (th:object) |
| `src/main/resources/templates/bpo/admin/gestor-detalle.html` | Template | 18 (btn-ghost) |
| `src/main/resources/templates/bpo/admin/subcarpeta-publicaciones.html` | Template | 14, 20, 26, 36 (sintaxis URL) |
| `src/main/resources/templates/bpo/admin/formulario-publicacion.html` | Template | 14, 22, 53 (errores varios) |

---

## 3. Criterios de aceptacion (checklist)

- [ ] **AC1:** El formulario `/gestores/nuevo` carga con campos Nombre, Codigo, Activa y botones Guardar/Cancelar
- [ ] **AC2:** El boton "Volver" en gestor-detalle.html usa clase `btn btn-primary` (azul, texto blanco)
- [ ] **AC3:** El boton "+ Nueva Publicacion" lleva al formulario sin error 500
- [ ] **AC4:** Las 4 URLs en subcarpeta-publicaciones.html usan sintaxis Thymeleaf correcta
- [ ] **AC5:** El formulario de publicacion tiene `th:object="${publicacion}"` y funciona al enviar
- [ ] **AC6:** El `@ModelAttribute` en el POST usa el nombre "publicacion"
- [ ] **AC7:** `mvn compile -DskipTests` sin errores

---

## 4. Tareas para el agente

### 4.1 Implementacion

- [ ] **Agregar metodo `crearCuenta` en `CuentaBPOService.java`** -- Crear metodo que guarde una nueva cuenta usando `cuentaRepository.save(gestor)`
- [ ] **Corregir sintaxis Thymeleaf en `subcarpeta-publicaciones.html`** -- Cambiar las 4 URLs de `${}` dentro de `@{}` a la sintaxis `{var}(var=${})`
- [ ] **Agregar objeto gestor al modelo en `AdminBPOController.java`** -- Ya existe `model.addAttribute("gestor", new CuentaBPO())` pero verificar que funciona con el POST
- [ ] **Cambiar `@ModelAttribute` en `AdminBPOController.java`** -- Linea 101: cambiar a `@ModelAttribute("publicacion") ContenidoBPOCreateDTO dto`
- [ ] **Arreglar boton "Volver" en `gestor-detalle.html`** -- Cambiar clase de `btn btn-ghost` a `btn btn-primary`
- [ ] **Arreglar boton "+ Nueva Publicacion" en `subcarpeta-publicaciones.html`** -- Cambiar clase de `bpo-btn bpo-btn-primary` a `btn btn-primary`
- [ ] **Corregir `formulario-publicacion.html`**:
  - Cambiar `th:object=""` a `th:object="${publicacion}"` (linea 22)
  - Eliminar `th:href` duplicado y corregir la URL del breadcrumb (linea 14)
  - Corregir URL del boton Cancelar: `(gid=${gestor.id},sid=${subcarpeta.id})` (linea 53)

### 4.2 Verificacion (el agente debe mostrar evidencia)

- [ ] **Lectura de archivos:** Leer cada archivo modificado y mostrar las lineas exactas.
- [ ] **Compilacion:** Ejecutar `mvn compile -DskipTests` y mostrar log sin errores.

---

## 5. Evidencia de finalizacion

*El agente debe llenar esto al finalizar:*

```
### Lectura de archivos:
[ ] CuentaBPOService.java – metodo crearCuenta agregado
[ ] subcarpeta-publicaciones.html – lineas 14, 20, 26, 36 corregidas
[ ] AdminBPOController.java – linea 101, @ModelAttribute corregido
[ ] gestor-detalle.html – linea 18, clase btn-primary
[ ] formulario-publicacion.html – lineas 14, 22, 53 corregidas

### Compilacion:
[pegar salida de mvn compile]

### Criterios de aceptacion cumplidos:
- [ ] AC1: /gestores/nuevo carga correctamente
- [ ] AC2: Boton Volver con estilo btn-primary
- [ ] AC3: + Nueva Publicacion sin error 500
- [ ] AC4: 4 URLs con sintaxis correcta
- [ ] AC5: Formulario de publicacion funcional
- [ ] AC6: @ModelAttribute con nombre correcto
- [ ] AC7: Compilacion sin errores
```
