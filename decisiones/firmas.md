# Plan: Flujo de Firmas Electronicas - Implementacion Quirurgica v2

**Reglas de oro:**
- Cada sub-tarea debe verificarse ANTES de marcarse [X]
- Boton agregado != tarea completa. El boton debe FUNCIONAR (probar en navegador)
- mvn compile -q sin errores tras cada tarea
- No alterar variables, funciones ni dependencias existentes
- Solo agregar codigo, nunca borrar lo que ya funciona

**Contexto tecnico:**
- FirmaController: @RequestMapping("/api/v1/firmas") - POST para firmar
- DocumentoController: @RequestMapping("/api/v1/documentos") - donde se agregan T1 y T3
- cursos/documentos.html: template donde se agrega el boton Firmar
- DocumentoController NO tiene firmaService inyectado - se debe agregar
- SecurityUtils YA esta inyectado en DocumentoController

---

## T1 - Endpoint GET que devuelve hash del documento [ ]

**Archivo:** src/main/java/com/lms/controller/DocumentoController.java

**Paso 1.1 - Verificar import:** buscar si ya existe 'import java.util.Map;'
Si no existe, agregarlo junto a los otros imports.

**Paso 1.2 - Agregar metodo** (dentro de la clase, antes del ultimo '}'):
```
@GetMapping("/{id}/hash")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<Map<String, String>> obtenerHash(@PathVariable UUID id) {
    Documento doc = documentoService.buscarPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado"));
    return ResponseEntity.ok(Map.of(
        "hashDocumento", doc.getHashSha256(),
        "codigo", doc.getCodigo()
    ));
}
```
Nota: documentoService ya existe en el controller.

**Verificacion T1:**
- [ ] mvn compile -q sin errores
- [ ] Acceder como admin, luego en otra pestana: /api/v1/documentos/{UUID}/hash devuelve JSON

---

## T2 - Boton Firmar en lista de documentos del curso [ ]

**Archivo:** src/main/resources/templates/cursos/documentos.html

**Paso 2.1 - Agregar boton HTML** junto a cada documento (en la columna de acciones, dentro del bucle forEach de documentos, junto al boton de descarga si existe). Buscar el bucle forEach(doc) que renderiza cada fila:

Agregar dentro de la celda de acciones:
```html
<button type="button" class="btn-firmar-doc"
    data-doc-id="${doc.id}"
    style="display:inline-flex;align-items:center;gap:4px;padding:4px 10px;
    background:var(--color-accento);color:white;border:none;border-radius:6px;
    cursor:pointer;font-size:0.75rem;font-weight:500;">
    <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931z"/></svg>
    Firmar
</button>
```

**Paso 2.2 - Agregar handler JS** al final del script existente (antes de </script>). El handler usa event delegation sobre el document:
```javascript
// Handler para boton Firmar documento
document.addEventListener('click', function(e) {
    var btn = e.target.closest('.btn-firmar-doc');
    if (!btn) return;
    e.preventDefault();
    var docId = btn.getAttribute('data-doc-id');
    if (!docId) return;
    btn.disabled = true;
    btn.innerHTML = 'Firmando...';
    fetch('/api/v1/documentos/' + docId + '/hash')
        .then(function(r) { if (!r.ok) throw new Error('Error al obtener hash'); return r.json(); })
        .then(function(data) {
            return fetch('/api/v1/firmas', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ documentoId: docId, hashDocumento: data.hashDocumento })
            });
        })
        .then(function(r) { if (!r.ok) throw new Error('Error al firmar'); return r.json(); })
        .then(function() {
            btn.innerHTML = '<svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg> Firmado';
            btn.style.background = 'var(--color-success)';
            btn.disabled = false;
            btn.classList.remove('btn-firmar-doc');
        })
        .catch(function(err) {
            alert('Error al firmar: ' + err.message);
            btn.disabled = false;
            btn.innerHTML = '<svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931z"/></svg> Firmar';
        });
});
```

**Verificacion T2:**
- [ ] Boton Firmar visible junto a cada documento en el curso
- [ ] Clic en Firmar -> boton cambia a "Firmando..." y luego a check verde "Firmado"
- [ ] KPI Firmas Electronicas en dashboard = 1 (despues de firmar)
- [ ] Sin errores en consola F12
- [ ] Si hay error de red, muestra alert con mensaje y boton vuelve a estado inicial

---

## T3 - Mostrar estado "Firmado" al cargar la pagina [ ]

**Archivo:** src/main/java/com/lms/controller/DocumentoController.java

**Paso 3.0 - Inyectar FirmaService en DocumentoController:**
Agregar en imports:
```
import com.lms.service.FirmaService;
```
Agregar campo:
```
private final FirmaService firmaService;
```
Agregar en constructor (parametro + this.firmaService = firmaService;)

**Paso 3.1 - Agregar endpoint:**
```
@GetMapping("/{id}/firmado")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<Map<String, Boolean>> verificarFirma(
        @PathVariable UUID id, Authentication auth) {
    UUID usuarioId = securityUtils.getCurrentUserId(auth);
    boolean firmado = firmaService.verificarFirma(usuarioId, id);
    return ResponseEntity.ok(Map.of("firmado", firmado));
}
```

**Archivo:** src/main/java/com/lms/service/FirmaService.java
Agregar metodo:
```
public boolean verificarFirma(UUID usuarioId, UUID documentoId) {
    return firmaRepository.existsByUsuarioIdAndDocumentoId(usuarioId, documentoId);
}
```

**Archivo:** src/main/java/com/lms/repository/FirmaElectronicaRepository.java
Agregar metodo (Spring Data lo implementa automaticamente):
```
boolean existsByUsuarioIdAndDocumentoId(UUID usuarioId, UUID documentoId);
```

**Paso 3.2 - Modificar cursos/documentos.html:**
En el bucle de renderizado de documentos, ANTES de renderizar el boton Firmar, hacer fetch a /api/v1/documentos/{doc.id}/firmado. Si firmado==true, renderizar badge verde en vez del boton:
```html
<span style="display:inline-flex;align-items:center;gap:4px;padding:4px 10px;
    background:rgba(16,185,129,0.1);border:1px solid rgba(16,185,129,0.2);border-radius:6px;
    font-size:0.75rem;font-weight:500;color:var(--color-success);">
    <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
    Firmado
</span>
```

**Verificacion T3:**
- [ ] mvn compile -q sin errores
- [ ] Al recargar pagina de documentos del curso, los ya firmados muestran check verde "Firmado"
- [ ] Los no firmados muestran el boton "Firmar"
- [ ] F12 sin errores

---

## T4 - Sidebar "Mis Firmas" muestra historial [ ]

**Archivo:** src/main/resources/templates/dashboard/firmas.html

Reemplazar el contenido actual del formulario por una tabla de historial:
- Titulo: "Historial de Firmas"
- Tabla con columnas: Documento, Codigo, Fecha, IP
- Fetch a GET /api/v1/firmas (verificar si existe endpoint GET; si no, crear uno en FirmaController que devuelva List<FirmaResponse> del usuario autenticado)

Si no existe GET /api/v1/firmas, agregar en FirmaController:
```
@GetMapping
@PreAuthorize("isAuthenticated()")
public ResponseEntity<List<FirmaResponse>> listar(Authentication auth) {
    UUID usuarioId = securityUtils.getCurrentUserId(auth);
    return ResponseEntity.ok(firmaService.listarPorUsuario(usuarioId));
}
```
Y en FirmaService:
```
public List<FirmaResponse> listarPorUsuario(UUID usuarioId) {
    return firmaRepository.findByUsuarioId(usuarioId).stream()
            .map(f -> new FirmaResponse(f.getId(), f.getDocumento().getCodigo(),
                    f.getDocumento().getNombreOriginal(), f.getTimestamp(), f.getIpOrigen()))
            .toList();
}
```

**Verificacion T4:**
- [ ] /dashboard/firmas muestra tabla con historial (vacia si no hay firmas)
- [ ] Al firmar un documento, aparece en el historial al refrescar
- [ ] Columnas: Documento, Codigo, Fecha, IP

---

## T5 - Verificacion final [ ]

- [ ] Crear documento -> visible en curso con boton "Firmar"
- [ ] Clic en Firmar -> boton cambia a check verde "Firmado"
- [ ] KPI Firmas Electronicas en dashboard = 1
- [ ] Refrescar pagina del curso -> el documento sigue mostrando "Firmado"
- [ ] Sidebar Mis Firmas -> historial muestra el documento firmado
- [ ] mvn compile -q OK
- [ ] Sin regresion en otros modulos
