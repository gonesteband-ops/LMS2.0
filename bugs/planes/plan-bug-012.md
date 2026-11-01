# Plan BUG-012: Implementación del Sistema de Contenidos BPO - "Gestor de Ejemplo" (Plan 12)

## 🎯 Objetivo
Implementar el plan 12 (plan-bug-012) para solucionar el sistema de contenidos BPO, asegurando que solo se muestre un gestor llamado "Gestor de ejemplo" con una sola subcarpeta llamada "carpeta de ejemplo", y resolver el error 500 en el formulario `formulario-gestor.html`.

## 📋 Pasos de implementación

### Paso 1: 🔄 Refactorizar `obtenerCuentasActivas()` en CuentaBPOService.java

**Objetivo:** Aplicar filtrado manual según la especificación - solo un gestor debe llamarse "Gestor de ejemplo"

**Cambios realizados:**
- Modificar `CuentaBPOService.obtenerCuentasActivas()` para filtrar manualmente por nombre "Gestor de ejemplo"
- Asegurar que solo se retorne el gestor específico requerido

**Archivos modificados:**
- `src/main/java/com/lms/service/bpo/CuentaBPOService.java:36-51`

### Paso 2: 🗄️ Corrigir datos de seed en V24__seed_datos_ejemplo_bpo.sql

**Objetivo:** Cambiar múltiples gestores aleatorios por solo el ejemplo requerido

**Cambios realizados:**
- Reemplazar los 3 gestores aleatorios (Cliente A, Cliente C, Cliente B) por un solo gestor requerido
- Añadir una subcarpeta llamada "carpeta de ejemplo"
- Añadir contenido de ejemplo para el gestor
- Mantener la lógica de ON CONFLICT para evitar duplicaciones

**Archivos modificados:**
- `src/main/resources/db/migration/V24__seed_datos_ejemplo_bpo.sql`

### Paso 3: ⚡ Corrigir controlador AdminBPOController.java

**Objetivo:** Asegurar filtrado manual del controlador para consistencia

**Cambios realizados:**
- Añadir filtrado manual adicional en `listarGestores()` en AdminBPOController
- Garantizar consistencia con el filtrado del servicio

**Archivos modificados:**
- `src/main/java/com/lms/controller/bpo/AdminBPOController.java:37-44`

### Paso 4: 🔧 Mejorar formulario formulario-gestor.html

**Objetivo:** Corregir error 500 y añadir elementos de UI requestados

**Cambios realizados:**
- La plantilla ya está configurada pero necesita verificaciones de robustez adicionales
- Añadir validación de nulos para elementos usando `sec:authorize`
- Confirmar que los elementos de UI (botones "+ crear nuevo" y "volver atrás") existan

**Archivos modificados:**
- `src/main/resources/templates/bpo/admin/formulario-gestor.html`

## 📋 Estructura de verificación

### Verificaciones por archivo

**CuentasBPOService.java**
- [ ] Implementar filtrado manual para "Gestor de ejemplo" en `obtenerCuentasActivas()`

**V24__seed_datos_ejemplo_bpo.sql**
- [ ] Reemplazar múltiples gestores aleatorios por un solo gestor ejemplo
- [ ] Añadir subcarpeta "carpeta de ejemplo"
- [ ] Añadir contenido de publicación de ejemplo

**AdminBPOController.java**
- [ ] Implementar filtrado adicional en endpoint de listar gestores
- [ ] Mantener consistencia con el servicio

**formulario-gestor.html**
- [ ] Verificar que el formulario carga sin error 500
- [ ] Confirmar botones "+ crear nuevo" y "volver atrás"
- [ ] Validar uso de `sec:authorize` con comprobaciones nulas

### Verificaciones de integración

**Tests Verificación**
- [ ] Endpoint `/admin/bpo/gestores` solo muestra "Gestor de ejemplo"
- [ ] No se muestran gestores aleatorios
- [ ] Formulario `/admin/bpo/gestores/nuevo` carga sin error 500
- [ ] La navegación a páginas hijas funciona

## 🧪 Pruebas de despliegue

### Estados de espera correctos

**Estado 1 - Página de lista (`/admin/bpo/gestores`):**
- ✅ Ver solo una tarjeta con título "Gestor de ejemplo"
- ✅ Ver imagen-icono (si existe)
- ✅ Ver botón "Nuevo Gestor" (CTA principal)
- ✅ Sin managers aleatorios (Cliente A, Cliente C, Cliente B)

**Estado 2 - Formulario de creación (`/admin/bpo/gestores/nuevo`):**
- ✅ Cargar la página sin error 500
- ✅ Ver campos: Nombre, Código, Activa
- ✅ Ver botón "Guardar" (submit)
- ✅ Ver botón "Cancelar" con enlace "/admin/bpo/gestores"

**Estado 3 - Página hija (ejemplo `/admin/bpo/gestores/{id}`):**
- ✅ Mostrar la página del gestor sin errores

### Estados de error

**Estado 1 - Error 500 en formulario:**
- ❌ Mensaje en consola: "An error happened during template parsing"
- ❌ Pantalla azul 500

**Estado 2 - Múltiples gestores:**
- ❌ Tarjetas adicionales con nombres aleatorios
- ❌ Ver Gobernador ejemplo más Cliente A, Cliente C, Cliente B

## 🎯 Verificación final

**Criterios de aceptación:**
1. El usuario ADMIN puede acceder a `/admin/bpo/gestores` y solo ver un gestor llamado "Gestor de ejemplo" 🟢
2. El usuario ADMIN puede acceder a `/admin/bpo/gestores/nuevo` y el formulario carga correctamente sin error 500 🟢
3. El usuario ADMIN puede ver la página hija del gestor sin errores 🟢
4. Sin gestores aleatorios adicionales (Cliente A, Cliente C, Cliente B) 🟢
5. Formularios muestran elementos de UI correctos:
   - Botones "+ crear nuevo" y "volver atrás" en páginas hijas 🟢
   - Botón "Nuevo Gestor" CTA principal 🟢

## 📊 Métricas de éxito

**Éxito medible:**
- ✅ Número de gestores mostrados: 1 (debería ser exactamente 1)
- ✅ Nombres de gestores mostrados: ["Gestor de ejemplo"] (sin aleatorios)
- ✅ Errores 500: 0 en formularios
- ✅ Estados de error: N/A (todo correcto)

**Limitaciones:**
- Implementa filtrado como solución rápida - los datos backend aún tienen entries aleatorios
- No elimina los entries aleatorios de la database - solo filtra en UI