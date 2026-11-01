---

# Plan: Modulo de Evaluaciones - Mejoras

**Fecha:** 15-Jul-2026

## Decisiones de diseno

1. Intentos: 1 intento. Repeticion solo autorizada por ADMIN/INSTRUCTOR.
2. Nota de aprobacion: Configurable por evaluacion (campo notaMinima).
3. Feedback: Solo nota final (sin detalle de aciertos).
4. Completacion: Si nota >= notaMinima, Inscripcion.completado = true.
5. Tabla nueva: intentos_evaluacion.

---

## Tarea 1 - Entidad IntentoEvaluacion.java

Crear: src/main/java/com/lms/model/IntentoEvaluacion.java
Campos: id(UUID), evaluacion(ManyToOne), inscripcion(ManyToOne), usuario(ManyToOne),
respuestas(JSONB), nota(Double), aprobado(Boolean), estado(String: EN_CURSO/COMPLETADO/REINICIADO),
fechaInicio, fechaEntrega, hashSha256, reiniciadoPor(UUID), motivoReinicio(TEXT), deletedAt, createdAt

- [ ] Entidad creada con JPA + relaciones + soft delete

---

## Tarea 2 - Migracion Flyway V12

Crear: src/main/resources/db/migration/V12__crear_intentos_evaluacion.sql
- Tabla intentos_evaluacion con todos los campos
- ALTER TABLE evaluaciones ADD COLUMN nota_minima

- [ ] Migracion V102 ejecutable
- [ ] Indices creados

---

## Tarea 3 - Campo notaMinima en Evaluacion

- Evaluacion.java: agregar double notaMinima = 4.0
- EvaluacionRequest.java: agregar Double notaMinima
- Editor HTML/JS: campo Nota minima para aprobar

- [ ] notaMinima configurable desde editor
- [ ] Default 4.0 si no se especifica

---

## Tarea 4 - Servicio IntentoEvaluacionService.java

Metodos:
- iniciarIntento(evaluacionId, usuarioId): crea EN_CURSO, rechaza si ya existe COMPLETADO
- entregarIntento(intentoId, respuestas): corrige, calcula nota, marca Inscripcion
- reiniciarIntento(intentoId, adminUserId, motivo): REINICIADO + nuevo intento

Logica de correccion:
- Comparar respuesta alumno vs respuestaCorrecta por pregunta
- nota = sum(peso aciertos) / sum(peso total) x 10
- Si nota >= notaMinima: aprobado=true, inscripcion.completado=true
- Audit: INTENTO_COMPLETADO / INTENTO_APROBADO / INTENTO_REINICIADO

- [ ] Correccion automatica funcional
- [ ] Nota calculada correctamente
- [ ] Inscripcion actualizada si aprobado
- [ ] Segundo intento rechazado sin autorizacion

---

## Tarea 5 - Endpoint reiniciar intento (admin)

POST /api/v1/cursos/{cursoId}/evaluaciones/{evaluacionId}/reiniciar
- @PreAuthorize hasAnyRole ADMIN/INSTRUCTOR/SUPERADMIN
- Body: { motivo: ... }
- Frontend: modal en participantes.html (similar a forzar completado)

- [ ] Admin puede reiniciar intento con trazabilidad
- [ ] Se registra quien autorizo, motivo y fecha
- [ ] Historial del intento anterior queda como REINICIADO

---

## Tarea 6 - Flujo frontend rendir.html

- Al cargar evaluacion: POST /iniciar crea intento EN_CURSO
- Boton Entregar: POST /responder con respuestas
- Respuesta: pantalla con nota y si aprobo
- Si ya hay intento COMPLETADO: redirigir a resultado

- [ ] Estudiante puede rendir evaluacion
- [ ] Al entregar ve pantalla con nota
- [ ] No puede rendir de nuevo si ya completo
- [ ] Resultado muestra nota y si aprobo

---

## Criterios de aceptacion generales

- [ ] T1: Entidad IntentoEvaluacion creada con JPA + relaciones
- [ ] T2: Migracion V12 ejecutable, tabla creada
- [ ] T3: notaMinima configurable desde editor
- [ ] T4: Correccion automatica funcional
- [ ] T5: Admin puede reiniciar intento con trazabilidad
- [ ] T6: Estudiante rinde, recibe nota, no puede rendir de nuevo
- [ ] Event Sourcing, hash encadenado, UUID, soft delete
- [ ] mvn clean compile -q sin errores
