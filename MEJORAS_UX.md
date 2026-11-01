# 🎯 MEJORAS UX — Alinear con estándar SaaS corporativo

> Basado en `UX_ejemplo/proyecto A` y `UX_ejemplo/proyecto B`
> Objetivo: Que el LMS se vea y comporte como las otras apps del ecosistema

---

## 📋 INSTRUCCIONES PARA EL AGENTE

Lee este archivo COMPLETO primero. Luego ejecuta cada fase en orden.
Después de cada tarea, marca con `[X]` y haz commit si aplica.
NO modifiques templates HTML, backend, ni migraciones. Solo CSS.

---

## FASE 1: PALETA DE COLORES (variables.css)

### T1.1 — Cambiar colores principales
`[X]` `--color-primario: #0f172a` → `#2F4DAA`
`[X]` `--color-primario-light: #1e293b` → `#2A469D`
`[X]` `--color-primario-dark: #020617` → `#1a2d6e`

### T1.2 — Cambiar acento a azul (reemplazar amber)
`[X]` `--color-accento: #f59e0b` → `#2F4DAA`
`[X]` `--color-accento-light: #fbbf24` → `#3d5fc4`
`[X]` `--color-accento-dark: #d97706` → `#1a2d6e`

### T1.3 — Cambiar colores de estado
`[X]` `--color-success: #10b981` → `#4CAA37`
`[X]` `--color-danger: #ef4444` → `#F95E50`
`[X]` `--color-warning: #f59e0b` → `#FFC107`
`[X]` `--color-info: #3b82f6` → `#2F4DAA`

### T1.4 — Cambiar fondos y bordes
`[X]` `--color-secundario: #f8fafc` → `#F0F1F3`
`[X]` `--color-secundario-dark: #e2e8f0` → `#D1D5DB`

### T1.5 — Aumentar intensidad de sombras
`[X]` `--shadow-sm: 0 1px 2px rgba(0,0,0,0.04)` → `0 1px 2px rgba(0,0,0,0.05)`
`[X]` `--shadow-md: 0 4px 6px -1px rgba(0,0,0,0.06)` → `0 4px 6px -1px rgba(0,0,0,0.1)`
`[X]` `--shadow-lg: 0 10px 15px -3px rgba(0,0,0,0.08)` → `0 10px 15px -3px rgba(0,0,0,0.1)`
`[X]` `--shadow-card: 0 1px 3px rgba(0,0,0,0.04)` → `0 4px 6px -1px rgba(0,0,0,0.1)`
`[X]` `--shadow-card-hover: 0 10px 40px -10px rgba(0,0,0,0.1)` → `0 10px 15px -3px rgba(0,0,0,0.1)`

### T1.6 — Ajustar dark mode para nueva paleta
`[X]` Actualizar colores en `[data-theme="dark"]` para que calcen con azul corporativo

---

## FASE 2: TIPOGRAFÍA (base.css + variables.css)

### T2.1 — Reducir font-size base
`[X]` `--font-size-base: 1rem` → `0.875rem` (en variables.css)

### T2.2 — Ajustar line-height body
`[X]` `body { line-height: 1.6 }` → `line-height: 1.5` (en base.css)

---

## FASE 3: BOTONES (components.css)

### T3.1 — Botón base `.btn`
`[X]` Cambiar `padding: 0.625rem 1.25rem` → `0.75rem 1rem`
`[X]` Cambiar `box-shadow` → `0 4px 6px -1px rgba(0,0,0,0.1)`
`[X]` Cambiar `transition` → `all 0.2s`
`[X]` Agregar `:active { transform: scale(0.95) }`

### T3.2 — Botón primario `.btn-primary`
`[X]` Quitar `background: linear-gradient(...)` → `background: #2F4DAA`
`[X]` Quitar gradiente en hover → `background: #2A469D`
`[X]` Quitar `transform: translateY(-1px)` en hover
`[X]` Aumentar shadow hover → `0 10px 15px -3px rgba(0,0,0,0.1)`

### T3.3 — Botón acento `.btn-accent`
`[X]` Cambiar `background` → `#2F4DAA` (mismo que primary)
`[X]` Cambiar hover → `#2A469D`

### T3.4 — Botón danger `.btn-danger`
`[X]` Quitar `background: linear-gradient(...)` → `#F95E50` (sólido)
`[X]` Hover → `#E04A3D`
`[X]` Quitar gradiente

### T3.5 — Botón secundario `.btn-secondary`
`[X]` Cambiar `color` → `var(--text-primary)`
`[X]` Cambiar `border-color` → `#D1D5DB`

---

## FASE 4: INPUTS Y FORMULARIOS (components.css)

### T4.1 — Input `.form-input`
`[X]` Cambiar `padding: 0.625rem 0.875rem` → `0.75rem 1rem`
`[X]` Cambiar `font-size` → `0.875rem`
`[X]` Cambiar `border-color` → `#D1D5DB`
`[X]` Cambiar focus `border-color` → `#2F4DAA`
`[X]` Cambiar focus `box-shadow` → `0 0 0 3px rgba(47,77,170,0.2)`

### T4.2 — Select `.form-select`
`[X]` Cambiar focus `border-color` → `#2F4DAA`
`[X]` Cambiar focus `box-shadow` → `0 0 0 3px rgba(47,77,170,0.2)`

---

## FASE 5: BADGES (components.css)

### T5.1 — Badge base
`[X]` Cambiar `padding: 0.125rem 0.625rem` → `0.25rem 0.75rem`
`[X]` Cambiar `font-size` → `0.875rem`
`[X]` Cambiar `font-weight: 500` → `600`

### T5.2 — Badge de estado (cambiar a fondos sólidos)
`[X]` `.badge-primary` → fondo `#2F4DAA`, color `white`
`[X]` `.badge-success` → fondo `#4CAA37`, color `white`
`[X]` `.badge-warning` → fondo `#FFC107`, color `#1F2937`
`[X]` `.badge-danger` → fondo `#F95E50`, color `white`
`[X]` `.badge-info` → fondo `#2F4DAA`, color `white`
`[X]` Eliminar/actualizar reglas `[data-theme="dark"]` para badges

---

## FASE 6: CARDS (components.css)

### T6.1 — Card base `.card`
`[X]` Cambiar `box-shadow: var(--shadow-card)` → `0 4px 6px -1px rgba(0,0,0,0.1)`
`[X]` Cambiar `border: 1px solid transparent` → `1px solid #e2e8f0`
`[X]` Ajustar hover `box-shadow` → `0 10px 15px -3px rgba(0,0,0,0.1)`

### T6.2 — KPI Card `.kpi-card`
`[X]` Mismos cambios que card base (sombra y borde)

---

## FASE 7: SIDEBAR (layout.css)

### T7.1 — Link activo en sidebar
`[X]` `.sidebar-link.active` → `background: #2F4DAA` (sólido)
`[X]` `.sidebar-link.active` → `color: white`
`[X]` `.sidebar-link.active` → `border-right: 3px solid #3d5fc4`

---

## FASE 8: HEADER (layout.css)

### T8.1 — Header más limpio
`[X]` Eliminar `backdrop-filter: blur(12px)` (opcional)
`[X]` Cambiar `background: rgba(255,255,255,0.9)` → `#ffffff` (sólido)
`[X]` Ajustar dark mode para header sólido

---

## VERIFICACIÓN FINAL

`[X]` La app compila sin errores - CSS estático, no requiere compilación
`[X]` Los botones tienen aspecto corporativo (sólidos, sin gradientes)
`[X]` Los inputs tienen focus ring azul (#2F4DAA)
`[X]` Las cards tienen borde visible (#e2e8f0)
`[X]` El sidebar activo es azul (#2F4DAA) con texto blanco
`[X]` Los badges son sólidos no semitransparentes
`[X]` Los colores se ven consistentes con proyectos A y B (paleta azul corporativa)
