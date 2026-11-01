# 🐛 Reporte: Bug 000 — Ejemplo de la metodología

> Reporte generado automáticamente por el agente basado en la conversación con el usuario.

---

## 1. Síntoma

*Lo que el usuario dijo que vio:*
"Cuando intento iniciar sesión con un usuario válido, la página se queda cargando y nunca redirige al dashboard."

*Pasos para reproducir:*
1. Ir a `http://localhost:8080/login`
2. Ingresar email `admin@lms.cl` y contraseña correcta
3. Hacer clic en "Iniciar Sesión"
4. El botón se queda en "Procesando..." y nunca redirige

---

## 2. Comportamiento esperado

Después de hacer clic en "Iniciar Sesión", debe redirigir al dashboard (`/dashboard`) en menos de 3 segundos.

---

## 3. Comportamiento actual

El botón se queda en "Procesando..." indefinidamente. En consola F12 se ve:
```
POST /login 302
XMLHttpRequest cannot load /login. Redirect to /login?error=timeout
```

---

## 4. Evidencia recolectada por el agente

- **Logs del servidor:** `ERROR o.s.s.w.s.HttpSessionRequestCache - No se pueed guardar la request`
- **Código HTTP:** 302 (redirect loop)
- **Archivos relevantes identificados:**

| Archivo | Línea(s) | Por qué |
|---|---|---|
| `src/main/java/com/lms/security/FormLoginSuccessHandler.java` | 58-65 | El handler de éxito redirige pero la sesión no se persiste |
| `src/main/java/com/lms/security/SecurityConfig.java` | 42-50 | La cadena de filtros no incluye el handler correctamente |

---

## 5. Causa raíz identificada

El `FormLoginSuccessHandler` usa `new AntPathRequestMatcher("/dashboard/**")` para verificar la URL guardada, pero Spring Security 6 cambió el comportamiento de `RequestCache` y la URL guardada nunca coincide porque el path original (`/login`) es diferente al destino (`/dashboard`).

---

## 6. Contexto

- **URL o página:** `/login`
- **Navegador:** Chrome
- **Rol del usuario:** ADMIN
- **Rama del proyecto:** main
- **Fecha y hora:** 2026-07-28 02:00
