# Tareas de Mejora — Contrato 17-Jul-2026

**Reglas:**
- Cada tarea debe marcarse [X] al completarse
- Al finalizar, actualizar project-memo.md con resumen
- mvn compile -q sin errores antes de marcar completada
- No alterar funciones, variables ni dependencias existentes
- Iconos: SVG monocromatico (paleta azul/gris de variables.css)

---

## Tarea 1 — Tarjeta Evaluaciones pendientes [X]

### 1.1 Backend [X]
Archivo: src/main/java/com/lms/controller/DashboardController.java
Dentro de obtenerResumen(), despues de long firmas:
  long evaluacionesPendientes = intentoEvaluacionRepository
      .countByUsuarioIdAndEstadoNotAndDeletedAtIsNull(usuarioId, COMPLETADO);
Agregar evaluacionesPendientes al Map de respuesta.

### 1.2 Repositorio [X]
Archivo: src/main/java/com/lms/repository/IntentoEvaluacionRepository.java
Agregar metodo:
  long countByUsuarioIdAndEstadoNotAndDeletedAtIsNull(UUID usuarioId, String estado);

### 1.3 Frontend HTML [X]
Archivo: src/main/resources/templates/dashboard/index.html
Copiar estructura KPI existente, reemplazar id y texto.
SVG icono: reloj o checklist (stroke-width=2, fill=none).

### 1.4 Frontend JS [X]
Mismo archivo, funcion que procesa /api/v1/dashboard/resumen:
  document.getElementById('kpi-evaluaciones-pendientes').textContent = data.evaluacionesPendientes || 0;

---

## Tarea 2 — Mejorar Actividad Reciente [X]

### 2.1 Frontend [X]
Archivo: src/main/resources/templates/dashboard/index.html
Funcion cargarActividad(), dentro de xhr.onload (~linea 235).
Agregar funcion formatearActividad(ev) que mapee tipoEvento a texto contextual:
  DOCUMENTO_SUBIDO -> Documento subido: (metadata.nombreOriginal o codigo)
  CURSO_CREADO -> Curso creado: (metadata.titulo)
  EVALUACION_CREADA -> Evaluacion creada: (metadata.codigo)
  USUARIO_REGISTRADO -> Usuario registrado: (metadata.email)
  USUARIO_CREADO_ADMIN -> Usuario creado: (metadata.email)
  EXPORTACION_SOLICITADA -> Exportacion solicitada
  INSCRIPCION_CREADA -> Inscripcion realizada
  CURSO_COMPLETADO -> Curso completado
  SCORM_SUBIDO -> SCORM subido: (metadata.codigo)
  default -> ev.tipoEvento + ': ' + ev.accion
Iconos SVG monocromaticos segun categoria.
Fecha formateada DD/MM HH:MM.

### 2.2 Verificacion [X]
Admin ve: Curso creado: Ventas, Documento subido: manual.pdf
Estudiante ve: Evaluacion respondida: EVA001
Sin regresion en otros modulos.

---

## Tarea 3 — Multimedia badge [X]
Eliminado de cursos/listado.html. SCORM intacto.

---
## Cierre [ ]
Todas las tareas [X], mvn compile -q OK, project-memo.md actualizado.
