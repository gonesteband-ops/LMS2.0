# Contrato de Mejora: Bug #008

> Generado del reporte bugs/reportes/bug-008.md

---

## 1. Error detectado

La vista SCORM (scorm-upload.html) muestra elementos administrativos al ESTUDIANTE: boton subir, columnas tecnicas, seccion informativa y estado Habilitado.

---

## 2. Archivos y cambios quirurgicos

| Archivo | Cambio |
|---------|--------|
| scorm-upload.html:86 | sec:authorize ya existe (drop zone oculta para ESTUDIANTE) - OK |
| scorm-upload.html:178 | Agregar sec:authorize a la seccion Acerca del Formato |
| scorm-upload.html:336-370 | Modificar JS cargarPaquetesScorm() para ocultar columnas Codigo/Version/Tamano si es ESTUDIANTE |
| scorm-upload.html:350-353 | Estado: mostrar Pendiente/Realizado (desde ScormRuntimeService) si es ESTUDIANTE |
| ScormController.java:91-108 | Agregar datos de progreso por alumno al listado de paquetes |

---

## 3. Tareas detalladas

### Paso 1 - Ocultar Acerca del Formato para ESTUDIANTE

**Archivo:** scorm-upload.html:178

Agregar sec:authorize al div que contiene Acerca del formato:
`html
<div style=... sec:authorize="hasAnyRole('ADMIN','INSTRUCTOR','SUPERADMIN')">
    <h4>Acerca del formato</h4>
    ...
</div>
`"r

### Paso 2 - Modificar JS para ocultar columnas tecnicas si es ESTUDIANTE

**Archivo:** scorm-upload.html:307-418

En cargarPaquetesScorm(), cambiar la construccion de la tabla:

- Si esAdminScorm: mostrar todas las columnas (Codigo, Titulo, Estado, Version, Tamano, Subido, Acciones)
- Si NO esAdminScorm: mostrar solo (Titulo, Estado, Acciones)

### Paso 3 - Estado dinámico para ESTUDIANTE (Pendiente/Realizado)

**Archivo:** scorm-upload.html:350-353

Cuando es ESTUDIANTE, en vez de mostrar Habilitado/Deshabilitado, mostrar:
- Pendiente (span gris) si el alumno aun no completo el SCORM
- Realizado (span verde) si el alumno ya completo el SCORM

**Archivo:** ScormController.java:91-108

Agregar @RequestParam usuarioId opcional. Si se proporciona, incluir el progreso del alumno en la respuesta:
`json
{
  ... paquete ...,
  "completadoPorAlumno": true/false
}
`"r

El ScormRuntimeService tiene metodos para verificar si un alumno completo un paquete (lmsGetValue con cmi.core.lesson_status).

---

## 4. Criterios de aceptacion

- [ ] ESTUDIANTE no ve boton Subir paquete SCORM
- [ ] ESTUDIANTE no ve columnas Codigo, Version, Tamano
- [ ] ESTUDIANTE no ve seccion Acerca del Formato
- [ ] ESTUDIANTE ve estado Pendiente/Realizado segun progreso
- [ ] ADMIN/INSTRUCTOR ve todo como antes
- [ ] mvn compile -DskipTests sin errores
