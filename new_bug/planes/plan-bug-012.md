# 🧾 Plan: Bug 012 — Usuarios: carga masiva + plantilla descargable

> Generado del reporte `reportes/bug-012.md`

---

## 1. Archivos a modificar

| Archivo | Cambio | Tipo |
|---------|--------|------|
| `src/main/java/com/lms/dto/CargaMasivaUsuariosReport.java` | **CREAR** — DTO de reporte (total, exitosos, duplicados, errores, detalles) | DTO |
| `src/main/java/com/lms/service/UsuarioService.java` | Agregar `crearUsuariosMasivo(MultipartFile, UUID)` | Service |
| `src/main/java/com/lms/controller/AdminHtmxController.java` | `GET /usuarios/plantilla-masiva` + `POST /usuarios/masivo` | Controller |
| `src/main/resources/templates/admin/usuarios.html` | Card "Carga Masiva de Usuarios" + input CSV + "Descargar plantilla" | Template |
| `src/main/resources/templates/admin/usuarios-fragment.html` | Reporte post-carga (creados/duplicados/errores) | Template |

---

## 2. Formato CSV

```
rut;nombreCompleto;email;password;rol
11.111.111-1;Juan Pérez;juan.perez@example.cl;Password123!;ESTUDIANTE
```

- Separador: `;`
- Rol: nombre exacto (ESTUDIANTE, INSTRUCTOR, ADMIN, SUPERADMIN)
- BOM de Excel soportado (stripping en primera línea)
- Valida: email duplicado, RUT duplicado, rol inexistente, formato inválido

---

## 3. Criterios de aceptación

- [ ] **AC1:** Plantilla descarga con `Content-Disposition: attachment`
- [ ] **AC2:** Card "Carga Masiva de Usuarios" visible en `/admin/usuarios`
- [ ] **AC3:** Carga CSV crea usuarios y muestra reporte
- [ ] **AC4:** Duplicados se reportan sin crear usuario repetido
- [ ] **AC5:** Errores de línea se reportan con detalle
- [ ] **AC6:** BOM de Excel se maneja correctamente
- [ ] **AC7:** `mvn compile` sin errores
- [ ] **AC8:** Todos los tests en `tests/test-bug-012.md` pasan
