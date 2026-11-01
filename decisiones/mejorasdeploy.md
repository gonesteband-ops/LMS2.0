
# Mejoras de Deploy - 19-Jul-2026

## 1. Exportaciones fallan con 500 (RedisTemplate null) [ ]
- Archivo: ExportacionService.java, lineas 165 y 178
- redisTemplate.convertAndSend() necesita null check antes de llamar
- Envolver con: if (redisTemplate != null) { ... }

## 2. Evaluacion: notaMinima no se persiste o se ignora [ ]
- Sintoma: Estudiante responde 1/3 preguntas (10%), sistema dice 'Aprobado con 10%'
- Deberia decir 'Reprobado' porque 10% < 60% (notaMinima)
- Causa posible: EvaluacionService no persiste notaMinima o getNotaMinima() devuelve 0
- Verificar: EvaluacionService.crearEvaluacion() y actualizarEvaluacion()
- Verificar: getNotaMinima() en IntentoEvaluacionService.entregarIntento()
- Verificar en BD: select nota_minima from evaluaciones where codigo = 'EVA00X';

## 3. Boton Enviar sin estado de carga (doble clic) [ ]
- Archivo: rendir.html - boton de enviar respuestas
- Al hacer clic, btn.disabled=true + texto cambia a 'Enviando...' + spinner
- En caso de error, restaurar boton a estado original

## 4. Fix: null check en redisTemplate lineas 165 y 178 [ ]
- Archivo: ExportacionService.java
- Envolver redisTemplate.convertAndSend() con if (redisTemplate != null)

## 5. Fix: notaMinima no se persiste [ ]
- Verificar EvaluacionService.crearEvaluacion() y actualizarEvaluacion()
- Verificar en BD: SELECT codigo, nota_minima FROM evaluaciones
