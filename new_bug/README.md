# 🐛 Sistema de Reporte de Bugs — Metodología 3 Capas

> **Filosofía:** Tú reportas en lenguaje natural. El agente estructura, planifica y verifica. Tú solo validas.

---

## Flujo completo

```
TÚ (chat)                    AGENTE                         TÚ validas
   │                            │                              │
   ├─ "no funciona X" ────────► │                              │
   │                            ├─ bugs/reportes/bug-XXX.md    │
   │                            ├─ bugs/planes/plan-bug-XXX.md │
   │                            ├─ bugs/tests/test-bug-XXX.md  │
   │                            │                              │
   │                            │◄────── validas los 3 ────────┤
   │                            │                              │
   │                            ├─ Implementa                  │
   │                            ├─ Ejecuta tests               │
   │                            │  └─ Si falla → corrige       │
   │                            │  └─ Si pasa → entrega        │
   │◄─────── resultado ────────┤                              │
```

---

## Los 3 archivos

| Archivo | Quién lo escribe | Qué contiene | Ejemplo |
|---|---|---|---|
| `reportes/bug-XXX.md` | **Agente** (de tu chat) | El problema documentado: síntomas, pasos, capturas, contexto | `bug-021.md` |
| `planes/plan-bug-XXX.md` | **Agente** | La solución: archivos a modificar, línea por línea, orden de implementación | `plan-bug-021.md` |
| `tests/test-bug-XXX.md` | **Agente** | La verificación TDD: tests SQL, grep, JUnit, bash y smoke que deben pasar | `test-bug-021.md` |

---

## Reglas de oro

1. **Tú nunca escribes un archivo.** Solo hablas en el chat. El agente estructura.
2. **Los 3 archivos se crean antes de implementar.** Primero se define, después se ejecuta.
3. **El agente no entrega hasta que todos los tests pasen.** Si un test falla, corrige y repite.
4. **Tú validas los 3 archivos antes de dar la orden de implementar.** Si algo no te gusta, lo corregimos antes de tocar código.
5. **Los tests son el contrato.** Lo que no está en `tests/test-bug-XXX.md` no se verifica. Lo que está, se cumple sí o sí.

---

## Cómo empezar

Solo escribe en el chat algo como:

> *"No funciona la página de incidentes, se ve como texto feo"*

El agente:
1. Investiga el código y los logs
2. Crea `reportes/bug-021.md` con lo que encontró
3. Crea `planes/plan-bug-021.md` con la solución
4. Crea `tests/test-bug-021.md` con los tests

Tú solo dices:
- *"Está bien, implementa"* → el agente ejecuta
- *"Falta esto..."* → el agente ajusta los archivos
- *"No me gusta este enfoque"* → se discute y se cambia

Nunca se implementa nada que no haya pasado por los 3 archivos y tu validación.
