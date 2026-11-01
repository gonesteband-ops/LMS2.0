# 🧾 Contrato de Mejora: Bug #001

> Generado del reporte `bugs/reportes/bug-001.md`

---

## 1. Error detectado

**Síntoma (qué ve el usuario):**  
Al navegar entre secciones del dashboard, el link "Mis Datos (ARCOP)" del menú lateral aparece en `/dashboard` pero desaparece al ir a `/dashboard/perfil`, `/dashboard/firmas`, `/cursos/listado`, etc.

**Causa raíz (qué falla en el código):**  
El menú lateral está duplicado manualmente en 14 templates HTML diferentes. Cuando se agrega un nuevo link (ARCOP), solo se actualizan algunos templates, quedando otros inconsistentes.

**Impacto (qué funcionalidad se rompe):**  
El usuario no puede acceder a "Mis Datos (ARCOP)" desde la mayoría de las páginas del dashboard.

---

## 2. Archivos involucrados

| Archivo | Rol | Acción |
|---------|-----|--------|
| `src/main/resources/templates/fragments/sidebar.html` | NUEVO fragmento | Crear con el sidebar completo |
| `src/main/resources/templates/dashboard/index.html` | Template | Reemplazar sidebar inline por th:replace |
| `src/main/resources/templates/dashboard/perfil.html` | Template | Reemplazar sidebar inline por th:replace |
| `src/main/resources/templates/dashboard/firmas.html` | Template | Reemplazar sidebar inline por th:replace |
| `src/main/resources/templates/dashboard/privacidad.html` | Template | Reemplazar sidebar inline por th:replace |
| `src/main/resources/templates/dashboard/documentos.html` | Template | Reemplazar sidebar inline por th:replace |
| `src/main/resources/templates/dashboard/arcop.html` | Template | Reemplazar sidebar inline por th:replace |
| `src/main/resources/templates/dashboard/exportaciones.html` | Template | Reemplazar sidebar inline por th:replace |
| `src/main/resources/templates/cursos/listado.html` | Template | Reemplazar sidebar inline por th:replace |
| `src/main/resources/templates/cursos/participantes.html` | Template | Reemplazar sidebar inline por th:replace |
| `src/main/resources/templates/evaluaciones/listado.html` | Template | Reemplazar sidebar inline por th:replace |
| `src/main/resources/templates/admin/usuarios.html` | Template | Reemplazar sidebar inline por th:replace |
| `src/main/resources/templates/admin/roles.html` | Template | Reemplazar sidebar inline por th:replace |
| `src/main/resources/templates/admin/auditoria.html` | Template | Reemplazar sidebar inline por th:replace |
| `src/main/resources/templates/admin/incidentes.html` | Template | Reemplazar sidebar inline por th:replace |

---

## 3. Criterios de aceptación (checklist)

- [ ] **AC1:** "Mis Datos (ARCOP)" aparece en todas las páginas del dashboard
- [ ] **AC2:** Los permisos `sec:authorize` se mantienen (Exportaciones solo ADMIN/INSTRUCTOR/SUPERADMIN, Administración solo ADMIN/SUPERADMIN)
- [ ] **AC3:** El link activo se marca correctamente en cada página (clase `sidebar-link active`)
- [ ] **AC4:** No hay regresión visual (los estilos SVG y espaciado se mantienen)
- [ ] **AC5:** `mvn compile -DskipTests` sin errores

---

## 4. Tareas para el agente

### 4.1 Crear fragmento compartido

**Archivo:** `src/main/resources/templates/fragments/sidebar.html`

**Qué hacer:** Crear un fragmento Thymeleaf que reciba un parámetro `seccion` y renderice el sidebar completo. El link activo se marca comparando `${seccion}` con el nombre de cada ruta.

**Estructura del fragmento:**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<div th:fragment="sidebar(seccion)">
    <aside class="dashboard-sidebar" role="navigation">
        <nav style="flex:1;padding:var(--spacing-lg) var(--spacing-sm);overflow-y:auto;">
            <div style="padding:0 var(--spacing-sm) var(--spacing-md);">
                <p class="sidebar-section-title">Navegación</p>
            </div>
            <!-- Panel Principal -->
            <a href="/dashboard" th:classappend="${seccion == 'dashboard'} ? 'active' : ''" class="sidebar-link">
                <svg class="sidebar-link-icon" ...>...</svg>
                Panel Principal
            </a>
            <!-- Mis Cursos -->
            <a href="/cursos" th:classappend="${seccion == 'cursos'} ? 'active' : ''" class="sidebar-link">
                ...
                Mis Cursos
            </a>
            <!-- ... todos los links ... -->
        </nav>
    </aside>
</div>
</html>
```

**Links incluir:**
1. Panel Principal (`/dashboard`) — seccion: 'dashboard'
2. Mis Cursos (`/cursos`) — seccion: 'cursos'
3. Documentos (`/dashboard/documentos`) — seccion: 'documentos'
4. Consentimiento (`/consentimiento`) — seccion: 'consentimiento'
5. Privacidad (`/dashboard/privacidad`) — seccion: 'privacidad'
6. **Mis Datos (ARCOP)** (`/dashboard/arcop`) — seccion: 'arcop'
7. Mis Firmas (`/dashboard/firmas`) — seccion: 'firmas'
8. Mi Perfil (`/dashboard/perfil`) — seccion: 'perfil'
9. Exportaciones (`/dashboard/exportaciones`) — seccion: 'exportaciones', con `sec:authorize="hasAnyRole('ADMIN', 'INSTRUCTOR', 'SUPERADMIN')"`
10. Administración (sección) — con `sec:authorize="hasAnyRole('ADMIN', 'SUPERADMIN')"`:
    - Usuarios (`/admin/usuarios`) — seccion: 'admin-usuarios'
    - Roles (`/admin/roles`) — seccion: 'admin-roles'
    - Auditoría (`/admin/auditoria`) — seccion: 'admin-auditoria'

### 4.2 Modificar templates existentes

Para cada template en la lista de archivos, buscar el bloque:
```html
<aside class="dashboard-sidebar" role="navigation">
    ...
</aside>
```

Y REEMPLAZARLO por:
```html
<div th:replace="~{fragments/sidebar :: sidebar(seccion='xxx')}"></div>
```

Donde `xxx` es el nombre de la sección correspondiente:
- `dashboard/index.html` → `seccion='dashboard'`
- `dashboard/perfil.html` → `seccion='perfil'`
- `dashboard/firmas.html` → `seccion='firmas'`
- `dashboard/privacidad.html` → `seccion='privacidad'`
- `dashboard/documentos.html` → `seccion='documentos'`
- `dashboard/arcop.html` → `seccion='arcop'`
- `dashboard/exportaciones.html` → `seccion='exportaciones'`
- `cursos/listado.html` → `seccion='cursos'`
- `cursos/participantes.html` → `seccion='cursos'`
- `evaluaciones/listado.html` → `seccion='evaluaciones'`
- `admin/usuarios.html` → `seccion='admin-usuarios'`
- `admin/roles.html` → `seccion='admin-roles'`
- `admin/auditoria.html` → `seccion='admin-auditoria'`
- `admin/incidentes.html` → `seccion='admin-incidentes'`

### 4.3 Verificar

- [ ] **Compilación:** `mvn compile -DskipTests` sin errores
- [ ] **Lectura de archivos:** mostrar líneas exactas modificadas
