# Plan: Detalle de Evaluacion - Feedback y Reporte de Resultados

**Fecha:** 17-Jul-2026

## Objetivo

1. **Estudiante:** al rendir una evaluacion, ver que respuestas acerto y cuales fallo.
2. **Admin/Instructor:** exportar reporte XLSX con aprobados/reprobados + detalle por alumno.

---

## T1 - Backend: Almacenar detalle por pregunta al entregar

Modificar IntentoEvaluacionService.entregarIntento() para guardar en respuestas un array de objetos:
```json
[{ \"preguntaId\": \"uuid\", \"respuestaAlumno\": \"4\", \"correcta\": false, \"peso\": 10 }, ...]
```

- [ ] El campo respuestas del IntentoEvaluacion incluye per-question correct/incorrect + peso
- [ ] No se rompe la correccion actual (nota se sigue calculando igual)

---

## T2 - Backend: Endpoint GET /{evaluacionId}/resultado-detalle

Nuevo endpoint en EvaluacionController que devuelva:
- nota, aprobado
- preguntas (enunciado, peso, respuesta del alumno, si fue correcta)
- Solo accesible si el intento esta COMPLETADO

- [ ] Endpoint funcional con autenticacion de estudiante
- [ ] Devuelve detalle por pregunta

---

## T3 - Frontend: Mostrar detalle al estudiante post-entrega

Modificar rendir.html:
- Despues de entregar, llamar al endpoint de detalle
- Mostrar cada pregunta con icono (check) o (x) segun si acerto
- Mostrar nota total y %

- [ ] Estudiante ve que respuestas acerto/fallo
- [ ] Si ya rindio, al volver a la pagina de evaluacion ve el resultado

---

## T4 - Backend: Nuevo tipo de reporte XLSX 'resultados'

Modificar ExportacionService y exportaciones.html:
- Agregar tipo 'resultados' al select de reportes
- Metodo generarReporteResultados(): por cada estudiante con intento completado:
  - Nombre, email, nota, aprobado
  - Columnas por pregunta: (check)/(x) + peso
- Filtrable por codigo EVA001

- [ ] Reporte 'resultados' disponible en el combo
- [ ] XLSX generado con aprobados/reprobados y detalle por pregunta

---

## Archivos estimados a tocar

| Archivo | Tarea |
|---------|-------|
| IntentoEvaluacionService.java | T1 - Guardar detalle por pregunta |
| EvaluacionController.java | T2 - Endpoint resultado-detalle |
| rendir.html | T3 - Mostrar detalle al estudiante |
| listado.html | T3 - Link a detalle desde listado |
| ExportacionService.java | T4 - Reporte resultados XLSX |
| exportaciones.html | T4 - Nuevo tipo en el combo |
