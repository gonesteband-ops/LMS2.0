# 🐛 Bugs — Flujo de Mejora Continua

Esta carpeta contiene todo lo necesario para reportar errores y planificar mejoras en la aplicación LMS LegalTech.

---

## Cómo usar este sistema

### Paso 1 — Detectas un error
Ves algo que no funciona: un botón que se queda pegado, un dato que no se guarda, un error en pantalla, etc.

### Paso 2 — Reportas el error
1. Copia el archivo `template-reporte.md` a la carpeta `reportes/` con el nombre `bug-XXX.md` (donde XXX es el número siguiente).
2. Llena los campos: qué pasó, dónde pasó, qué esperabas.
3. Si puedes, agrega una captura de pantalla o el error de la consola (F12).

### Paso 3 — Le pides al agente que lo procese
Escribe en el chat:
> *"Procesa el reporte bugs/reportes/bug-001.md"*

### Paso 4 — El agente genera un plan de mejora
El agente:
1. Lee tu reporte.
2. Escanea los archivos del proyecto relacionados.
3. Genera un plan detallado en `bugs/planes/plan-bug-XXX.md` usando la plantilla `template-contrato.md`.
4. Te lo muestra para que lo revises.

### Paso 5 — Apruebas y el agente implementa
- Si el plan te parece correcto, dices: *"Implementa el plan bugs/planes/plan-bug-XXX.md"*.
- El agente ejecuta cada tarea con TDD, compila, ejecuta tests y te muestra evidencias.
- Si algo falla, se detiene y corrige antes de seguir.

---

## Estructura de la carpeta

```text
bugs/
├── README.md                 ← Este archivo
├── template-reporte.md        ← Plantilla para reportar errores
├── template-contrato.md       ← Plantilla del plan de mejora (TDD)
├── reportes/                  ← Aquí guardas tus reportes
│   └── (bug-001.md, bug-002.md...)
└── planes/                    ← Aquí el agente guarda los planes
    └── (plan-bug-001.md, plan-bug-002.md...)
```

---

## Reglas importantes

- **No modifiques archivos fuera de `bugs/`** con estas plantillas. Son solo para reportar y planificar.
- **Los reportes son solo tuyos.** El agente los lee pero no los modifica.
- **Los planes los genera el agente.** Tú solo apruebas o rechazas.
- **TDD obligatorio:** Cada tarea del plan debe tener un test que falle antes y pase después.
