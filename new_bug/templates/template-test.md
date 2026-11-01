# ✅ Tests: Bug [ID] — [Título corto]

> Tests de verificación TDD. Deben fallar ANTES de implementar y pasar DESPUÉS.
> El agente NO entrega hasta que TODOS los tests pasen.

---

## Categorías de tests

| Tipo | Qué verifica |
|---|---|
| **SQL** | Estado de la base de datos después de migraciones |
| **Grep** | Que no queden referencias a código eliminado |
| **JUnit** | Comportamiento de endpoints y servicios |
| **Compilación** | `mvn compile` sin errores |
| **Smoke** | Prueba manual de rutas clave |

---

## Tests

### Test [ID] — [Nombre]
```sql
-- Código del test (SQL, bash, Java, etc.)
```

**Antes:** [resultado esperado antes de la corrección]
**Después:** [resultado esperado después de la corrección]
**Tipo:** SQL / Grep / JUnit / Bash / Smoke

---

## Checklist de ejecución

- [ ] Test 1: ❌ / ✅
- [ ] Test 2: ❌ / ✅
- [ ] Test 3: ❌ / ✅
- [ ] ...
- [ ] `mvn compile`: ❌ / ✅
- [ ] Smoke test manual: ❌ / ✅

> **Regla:** Si algún test queda como ❌, el agente NO entrega. Debe corregir y repetir.
