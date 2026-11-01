# ✅ Tests: Bug 000 — Login no redirige al dashboard

> Tests de verificación TDD. Deben fallar ANTES de implementar y pasar DESPUÉS.
> El agente NO entrega hasta que TODOS los tests pasen.

---

## Tests

### Test 1 — Login exitoso redirige a /dashboard
```bash
curl -v -c cookies.txt -b cookies.txt \
  -X POST http://localhost:8080/login \
  -d "username=admin@lms.cl&password=correcta" \
  2>&1 | grep -E "Location:|HTTP/"
```

**Antes:** `Location: /login?error` (redirect loop)
**Después:** `Location: http://localhost:8080/dashboard`
**Tipo:** Bash

### Test 2 — Login fallido muestra error
```bash
curl -v -c cookies.txt -b cookies.txt \
  -X POST http://localhost:8080/login \
  -d "username=admin@lms.cl&password=mala" \
  2>&1 | grep -E "error|Location:"
```

**Antes:** Misma respuesta que login exitoso (no distingue)
**Después:** `Location: /login?error` con mensaje visible
**Tipo:** Bash

### Test 3 — Sesión se mantiene después de login
```bash
curl -v -c cookies.txt -b cookies.txt \
  -X POST http://localhost:8080/login \
  -d "username=admin@lms.cl&password=correcta" && \
curl -v -b cookies.txt http://localhost:8080/dashboard \
  2>&1 | grep -E "HTTP/|200 OK"
```

**Antes:** 302 redirect al intentar acceder a dashboard (sesión no persistió)
**Después:** 200 OK (sesión funciona)
**Tipo:** Bash

### Test 4 — Compilación sin errores
```bash
mvn compile -q
```

**Antes:** BUILD SUCCESS (no hay error de compilación)
**Después:** BUILD SUCCESS
**Tipo:** Bash

### Test 5 — Smoke test manual
1. Abrir `http://localhost:8080/login` en Chrome
2. Ingresar `admin@lms.cl` con contraseña correcta
3. Hacer clic en "Iniciar Sesión"
4. Verificar que redirige a `/dashboard`

**Antes:** Se queda en "Procesando..."
**Después:** Redirige al dashboard en <3 segundos
**Tipo:** Smoke

---

## Checklist de ejecución

- [ ] Test 1 (Bash — login exitoso): ❌ / ✅
- [ ] Test 2 (Bash — login fallido): ❌ / ✅
- [ ] Test 3 (Bash — sesión persiste): ❌ / ✅
- [ ] Test 4 (Bash — compilación): ❌ / ✅
- [ ] Test 5 (Smoke manual): ❌ / ✅

> **Regla:** Si algún test queda como ❌, el agente NO entrega. Debe corregir y repetir.
