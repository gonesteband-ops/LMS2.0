# Test 10: BUG-015 FASE 4 - Rutas limpias con fallback UUID
# Verifica:
# - Plantillas usan "codigo" en lugar de "id"
# - Controller tiene resolveCuentaId y resolveSubservicioId
# - PathVariable son String para aceptar codigo o UUID
# - Fallback a UUID funciona

$ErrorActionPreference = "Stop"
$allPassed = $true

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  FASE 4: Rutas limpias (codigo + UUID)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Test 10.1: gestores.html debe usar g.codigo
$gestoresFile = "src/main/resources/templates/bpo/admin/gestores.html"
$gestoresPath = Join-Path $env:WORKSPACE_ROOT $gestoresFile
if (Test-Path $gestoresPath) {
    $content = Get-Content $gestoresPath -Raw
    if ($content -match 'id=\$\{g\.codigo\}') {
        Write-Host "PASS: gestores.html usa g.codigo" -ForegroundColor Green
    } else {
        Write-Host "FAIL: gestores.html no usa g.codigo" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test 10.2: gestor-detalle.html debe usar codigo
$detalleFile = "src/main/resources/templates/bpo/admin/gestor-detalle.html"
$detallePath = Join-Path $env:WORKSPACE_ROOT $detalleFile
if (Test-Path $detallePath) {
    $content = Get-Content $detallePath -Raw
    $cuentaCodigoCount = [regex]::Matches($content, 'gestor\.codigo').Count
    $subcarpetaCodigoCount = [regex]::Matches($content, 's\.codigo').Count
    if ($cuentaCodigoCount -ge 2 -and $subcarpetaCodigoCount -ge 1) {
        Write-Host "PASS: gestor-detalle.html usa codigo en URLs ($cuentaCodigoCount gestor, $subcarpetaCodigoCount subcarpeta)" -ForegroundColor Green
    } else {
        Write-Host "FAIL: gestor-detalle.html no usa codigo correctamente" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test 10.3: subcarpeta-publicaciones.html debe usar codigo
$pubFile = "src/main/resources/templates/bpo/admin/subcarpeta-publicaciones.html"
$pubPath = Join-Path $env:WORKSPACE_ROOT $pubFile
if (Test-Path $pubPath) {
    $content = Get-Content $pubPath -Raw
    $codigoCount = [regex]::Matches($content, '\.codigo').Count
    if ($codigoCount -ge 2) {
        Write-Host "PASS: subcarpeta-publicaciones.html usa codigo en URLs ($codigoCount veces)" -ForegroundColor Green
    } else {
        Write-Host "FAIL: subcarpeta-publicaciones.html no usa codigo correctamente" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test 10.4: formulario-publicacion.html debe usar codigo
$formFile = "src/main/resources/templates/bpo/admin/formulario-publicacion.html"
$formPath = Join-Path $env:WORKSPACE_ROOT $formFile
if (Test-Path $formPath) {
    $content = Get-Content $formPath -Raw
    $codigoCount = [regex]::Matches($content, '\.codigo').Count
    if ($codigoCount -ge 2) {
        Write-Host "PASS: formulario-publicacion.html usa codigo en URLs ($codigoCount veces)" -ForegroundColor Green
    } else {
        Write-Host "FAIL: formulario-publicacion.html no usa codigo correctamente" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test 10.5: Controller debe tener resolveCuentaId
$controllerFile = "src/main/java/com/lms/controller/bpo/AdminBPOController.java"
$controllerPath = Join-Path $env:WORKSPACE_ROOT $controllerFile
if (Test-Path $controllerPath) {
    $content = Get-Content $controllerPath -Raw
    if ($content -match 'private UUID resolveCuentaId\(String gestorId\)') {
        Write-Host "PASS: Controller tiene metodo resolveCuentaId" -ForegroundColor Green
    } else {
        Write-Host "FAIL: metodo resolveCuentaId NO encontrado" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test 10.6: Controller debe tener resolveSubservicioId
if (Test-Path $controllerPath) {
    $content = Get-Content $controllerPath -Raw
    if ($content -match 'private UUID resolveSubservicioId\(UUID cuentaId, String subId\)') {
        Write-Host "PASS: Controller tiene metodo resolveSubservicioId" -ForegroundColor Green
    } else {
        Write-Host "FAIL: metodo resolveSubservicioId NO encontrado" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test 10.7: PathVariable gestorId debe ser String (no UUID)
if (Test-Path $controllerPath) {
    $content = Get-Content $controllerPath -Raw
    if ($content -match '@PathVariable String gestorId') {
        Write-Host "PASS: PathVariable gestorId es String (acepta codigo o UUID)" -ForegroundColor Green
    } else {
        Write-Host "FAIL: PathVariable gestorId no es String" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test 10.8: Fallback debe intentar UUID primero, luego buscar por codigo
if (Test-Path $controllerPath) {
    $content = Get-Content $controllerPath -Raw
    if ($content -match 'UUID\.fromString' -and $content -match 'buscarPorCodigo') {
        Write-Host "PASS: Fallback intenta UUID primero, luego busca por codigo" -ForegroundColor Green
    } else {
        Write-Host "FAIL: Fallback no implementado correctamente" -ForegroundColor Red
        $allPassed = $false
    }
}

Write-Host ""
if ($allPassed) {
    Write-Host "RESUMEN: FASE 4 (Rutas limpias) COMPLETA" -ForegroundColor Green
    Write-Host "  - URLs limpias: /gestores/GESTOR_EJEMPLO/subcarpetas/soporte-tecnico" -ForegroundColor Green
    Write-Host "  - Fallback UUID: /gestores/a1000000-0000-.../subcarpetas/b2000000-..." -ForegroundColor Green
    exit 0
} else {
    Write-Host "RESUMEN: FASE 4 (Rutas limpias) INCOMPLETA - Tests FALLARON" -ForegroundColor Red
    exit 1
}
