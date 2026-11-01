# Contrato de Mejora: Bug #002

> Generado del reporte ugs/reportes/bug-002.md

---

## 1. Error detectado

**Sintoma:** Al editar un curso, al guardar da error 403 Forbidden. El estado siempre vuelve a "borrador".

**Causa raiz (3 problemas):**
1. **403 Forbidden:** etch() PUT a /api/v1/cursos/{id} podria no enviar credenciales JWT correctamente.
2. **Estado siempre "borrador":** guardarCurso() arma payload sin campo estado. CursoRequest.java no tiene campo estado. CursoService.actualizarCurso() nunca actualiza estado.
3. **duracionEstimada en blanco:** El campo duracionEstimada no existe en el modelo Curso.java ni se carga en cargarCurso().

**Impacto:** No se puede editar cursos correctamente. Los cambios de estado no se guardan.

---

## 2. Archivos involucrados

| Archivo | Rol | Lineas clave |
|---------|-----|--------------|
| src/main/resources/templates/cursos/editor.html | Frontend | 685-693 (payload), 698-701 (fetch) |
| src/main/java/com/lms/dto/CursoRequest.java | DTO | 9-27 (falta campo estado) |
| src/main/java/com/lms/service/CursoService.java | Service | 108-127 (no actualiza estado) |

---

## 3. Criterios de aceptacion

- [ ] **AC1:** PUT a /api/v1/cursos/{id} responde 200 OK en vez de 403
- [ ] **AC2:** Al editar un curso, el select de estado muestra el valor actual
- [ ] **AC3:** Al guardar con estado "publicado", el curso queda como "Publicado"
- [ ] **AC4:** mvn compile -DskipTests sin errores

---

## 4. Tareas para el agente

### Paso 1 - editor.html (Frontend)
**Ruta:** src/main/resources/templates/cursos/editor.html

1a. Linea 692: Agregar estado: estado, al payload JSON:
`javascript
var payload = {
    titulo: titulo,
    descripcion: descripcion,
    categoria: categoria,
    estado: estado,
    ...
};
`

1b. Linea 700: Agregar credentials: 'include' en fetch:
`javascript
fetch(url, {
    method: metodo,
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(payload)
})
`

### Paso 2 - CursoRequest.java (DTO)
**Ruta:** src/main/java/com/lms/dto/CursoRequest.java

Agregar String estado como ultimo campo del record.

### Paso 3 - CursoService.java (Backend)
**Ruta:** src/main/java/com/lms/service/CursoService.java

3a. Linea 70: Cambiar .estado("borrador") por .estado(request.estado() != null ? request.estado() : "borrador").

3b. Linea ~124: Agregar curso.setEstado(request.estado()); despues de setVersionActual.

### Verificacion
- [ ] mvn compile -DskipTests sin errores
- [ ] Mostrar lineas exactas modificadas
