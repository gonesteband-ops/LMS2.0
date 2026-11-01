# Test 06: Test funcional de endpoints HTTP del Gestor de Contenidos BPO
# Requiere servidor en http://localhost:8080
# Nota: Los endpoints requieren autenticacion, pero verificamos que respondan

$ErrorActionPreference = "Stop"
$BASE = "http://localhost:8080"

$allPassed = $true

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Tests funcionales - Gestor de Contenidos" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Test 1: Listado de gestores responde
try {
    $r = Invoke-WebRequest -Uri "$BASE/admin/bpo/gestores" -UseBasicParsing -TimeoutSec 10
    if ($r.StatusCode -eq 200) {
        Write-Host "PASS [HTTP 200] GET /admin/bpo/gestores" -ForegroundColor Green
    } else {
        Write-Host "FAIL [HTTP $($r.StatusCode)] GET /admin/bpo/gestores" -ForegroundColor Red; $allPassed = $false
    }
} catch { Write-Host "FAIL [ERROR] GET /admin/bpo/gestores: $($_.Exception.Message)" -ForegroundColor Red; $allPassed = $false }

# Test 2: Formulario nuevo gestor responde
try {
    $r = Invoke-WebRequest -Uri "$BASE/admin/bpo/gestores/nuevo" -UseBasicParsing -TimeoutSec 10
    if ($r.StatusCode -eq 200) {
        Write-Host "PASS [HTTP 200] GET /admin/bpo/gestores/nuevo" -ForegroundColor Green
    } else {
        Write-Host "FAIL [HTTP $($r.StatusCode)] GET /admin/bpo/gestores/nuevo" -ForegroundColor Red; $allPassed = $false
    }
} catch { Write-Host "FAIL [ERROR] GET /admin/bpo/gestores/nuevo" -ForegroundColor Red; $allPassed = $false }

# Test 3: Vista detalle gestor responde
try {
    $r = Invoke-WebRequest -Uri "$BASE/admin/bpo/gestores/a1000000-0000-0000-0000-000000000001" -UseBasicParsing -TimeoutSec 10
    if ($r.StatusCode -eq 200) {
        Write-Host "PASS [HTTP 200] GET /gestores/{id}" -ForegroundColor Green
    } else {
        Write-Host "FAIL [HTTP $($r.StatusCode)] GET /gestores/{id}" -ForegroundColor Red; $allPassed = $false
    }
} catch { Write-Host "FAIL [ERROR] GET /gestores/{id}" -ForegroundColor Red; $allPassed = $false }

# Test 4: Subcarpeta publicaciones responde
try {
    $r = Invoke-WebRequest -Uri "$BASE/admin/bpo/gestores/a1000000-0000-0000-0000-000000000001/subcarpetas/b2000000-0000-0000-0000-000000000001" -UseBasicParsing -TimeoutSec 10
    if ($r.StatusCode -eq 200) {
        Write-Host "PASS [HTTP 200] GET /gestores/{id}/subcarpetas/{subId}" -ForegroundColor Green
    } else {
        Write-Host "FAIL [HTTP $($r.StatusCode)] GET /gestores/{id}/subcarpetas/{subId}" -ForegroundColor Red; $allPassed = $false
    }
} catch { Write-Host "FAIL [ERROR] GET /gestores/{id}/subcarpetas/{subId}" -ForegroundColor Red; $allPassed = $false }

# Test 5: Formulario nueva publicacion responde
try {
    $r = Invoke-WebRequest -Uri "$BASE/admin/bpo/gestores/a1000000-0000-0000-0000-000000000001/subcarpetas/b2000000-0000-0000-0000-000000000001/nuevo" -UseBasicParsing -TimeoutSec 10
    if ($r.StatusCode -eq 200) {
        Write-Host "PASS [HTTP 200] GET /gestores/{id}/subcarpetas/{subId}/nuevo" -ForegroundColor Green
    } else {
        Write-Host "FAIL [HTTP $($r.StatusCode)] GET /gestores/{id}/subcarpetas/{subId}/nuevo" -ForegroundColor Red; $allPassed = $false
    }
} catch { Write-Host "FAIL [ERROR] GET /gestores/{id}/subcarpetas/{subId}/nuevo" -ForegroundColor Red; $allPassed = $false }

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Verificacion de estilos CSS (codigo fuente)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Test 6: gestor-detalle.html tiene btn-primary
$gd = Get-Content "src/main/resources/templates/bpo/admin/gestor-detalle.html" -Raw
if ($gd -match 'class="btn btn-primary"') {
    Write-Host "PASS [CSS] gestor-detalle.html: btn-primary en Volver" -ForegroundColor Green
} else { Write-Host "FAIL [CSS] gestor-detalle.html: falta btn-primary" -ForegroundColor Red; $allPassed = $false }

# Test 7: subcarpeta-publicaciones.html usa btn-primary
$sp = Get-Content "src/main/resources/templates/bpo/admin/subcarpeta-publicaciones.html" -Raw
if ($sp -match 'class="btn btn-primary"') {
    Write-Host "PASS [CSS] subcarpeta-publicaciones.html: btn-primary en boton" -ForegroundColor Green
} else { Write-Host "FAIL [CSS] subcarpeta-publicaciones.html: falta btn-primary" -ForegroundColor Red; $allPassed = $false }

# Test 8: formulario-gestor.html usa btn-primary  
$fg = Get-Content "src/main/resources/templates/bpo/admin/formulario-gestor.html" -Raw
if ($fg -match 'class="btn btn-primary"') {
    Write-Host "PASS [CSS] formulario-gestor.html: btn-primary en Guardar" -ForegroundColor Green
} else { Write-Host "FAIL [CSS] formulario-gestor.html: falta btn-primary" -ForegroundColor Red; $allPassed = $false }

# Test 9: formulario-publicacion.html usa btn-primary
$fp = Get-Content "src/main/resources/templates/bpo/admin/formulario-publicacion.html" -Raw
if ($fp -match 'class="btn btn-primary"') {
    Write-Host "PASS [CSS] formulario-publicacion.html: btn-primary en Publicar" -ForegroundColor Green
} else { Write-Host "FAIL [CSS] formulario-publicacion.html: falta btn-primary" -ForegroundColor Red; $allPassed = $false }

# Test 10: formulario-publicacion.html tiene th:object correcto
if ($fp -match 'th:object="\$\{publicacion\}"') {
    Write-Host "PASS [FORM] formulario-publicacion.html: th:object correcto" -ForegroundColor Green
} else { Write-Host "FAIL [FORM] formulario-publicacion.html: th:object incorrecto" -ForegroundColor Red; $allPassed = $false }

# Test 11: Verificar URLs correctas en subcarpeta-publicaciones.html
$sp = Get-Content "src/main/resources/templates/bpo/admin/subcarpeta-publicaciones.html" -Raw
$correctas = 0
if ($sp -match 'th:href="@\{/admin/bpo/gestores/\{id\}\(id=\$\{gestor\.id\}\)}"') { $correctas++ }
if ($sp -match 'th:href="@\{/admin/bpo/gestores/\{gid\}/subcarpetas/\{sid\}/nuevo\(gid=\$\{gestor\.id\},sid=\$\{subcarpeta\.id\}\)}"') { $correctas++ }
if ($sp -match 'th:action="@\{/admin/bpo/publicaciones/\{id\}/eliminar\(id=\$\{c\.id\}\)}"') { $correctas++ }
if ($correctas -ge 3) {
    Write-Host "PASS [URL] $correctas/3 URLs con sintaxis correcta" -ForegroundColor Green
} else {
    Write-Host "FAIL [URL] Solo $correctas/3 URLs correctas" -ForegroundColor Red; $allPassed = $false
}

# Test 12: Tarjetas de publicacion tienen borde redondeado (diseño card)
if ($sp -match 'border-radius:0\.75rem') {
    Write-Host "PASS [CARD] Publicaciones con diseño de tarjeta" -ForegroundColor Green
} else { Write-Host "WARN [CARD] No se detecto diseño de tarjeta" -ForegroundColor Yellow }

# Test 13: CuentaBPOService tiene crearCuenta
$cs = Get-Content "src/main/java/com/lms/service/bpo/CuentaBPOService.java" -Raw
if ($cs -match 'public CuentaBPO crearCuenta') {
    Write-Host "PASS [SERVICE] CuentaBPOService.crearCuenta() existe" -ForegroundColor Green
} else { Write-Host "FAIL [SERVICE] Falta crearCuenta()" -ForegroundColor Red; $allPassed = $false }

# Test 14: Controller usa @ModelAttribute("publicacion")
$ctrl = Get-Content "src/main/java/com/lms/controller/bpo/AdminBPOController.java" -Raw
if ($ctrl -match '@ModelAttribute\("publicacion"\)') {
    Write-Host "PASS [CTRL] @ModelAttribute(\"publicacion\") existe" -ForegroundColor Green
} else { Write-Host "FAIL [CTRL] Falta @ModelAttribute(\"publicacion\")" -ForegroundColor Red; $allPassed = $false }

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  RESULTADOS FINALES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
if ($allPassed) {
    Write-Host "  TODOS LOS TESTS FUNCIONALES PASARON" -ForegroundColor Green
    exit 0
} else {
    Write-Host "  HAY TESTS FUNCIONALES FALLADOS" -ForegroundColor Red
    exit 1
}
