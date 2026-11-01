# 🧾 Plan: Bug 000 — Login no redirige al dashboard

> Generado del reporte `reportes/bug-000-ejemplo.md`

---

## 1. Resumen del problema

El `FormLoginSuccessHandler` usa un `AntPathRequestMatcher` incompatible con Spring Security 6, causando un redirect loop al iniciar sesión.

---

## 2. Archivos a modificar

| Archivo | Cambio | Tipo |
|---|---|---|
| `src/main/java/com/lms/security/FormLoginSuccessHandler.java:60` | Reemplazar `AntPathRequestMatcher` por `DefaultSavedRequest` | Controller |
| `src/main/java/com/lms/security/SecurityConfig.java:45` | Agregar `.successHandler(formLoginSuccessHandler)` a la cadena de filtros | Config |

---

## 3. Orden de implementación

1. Modificar `FormLoginSuccessHandler.java` — cambiar la lógica de obtención de URL guardada
2. Modificar `SecurityConfig.java` — registrar el handler en la cadena de login
3. Ejecutar tests de verificación

---

## 4. Criterios de aceptación

- [ ] **AC1:** Login con credenciales válidas redirige a `/dashboard` en menos de 3 segundos
- [ ] **AC2:** Login con credenciales inválidas muestra mensaje de error (no rompe)
- [ ] **AC3:** `mvn compile` sin errores
- [ ] **AC4:** Todos los tests en `tests/test-bug-000-ejemplo.md` pasan
