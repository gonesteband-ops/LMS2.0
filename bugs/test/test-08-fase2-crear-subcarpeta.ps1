# Test 08: BUG-015 FASE 2 - Crear subcarpeta
# Verifica:
# - Template formulario-subcarpeta.html existe
# - Boton "+ Crear nueva carpeta" en gestor-detalle.html
# - Metodo crearSubcarpeta en CuentaBPOService
# - Endpoints GET y POST en AdminBPOController

$ErrorActionPreference = "Stop"
$allPassed = $true

# Test 8.1: formulario-subcarpeta.html debe existir
$templateFile = "src/main/resources/templates/bpo/admin/formulario-subcarpeta.html"
$fullPath = Join-Path $env:WORKSPACE_ROOT $templateFile
if (Test-Path $fullPath) {
    Write-Host "PASS: Template formulario-subcarpeta.html existe" -ForegroundColor Green
} else {
    Write-Host "FAIL: Template formulario-subcarpeta.html NO existe" -ForegroundColor Red
    $allPassed = $false
}

# Test 8.2: Boton "+ Crear nueva carpeta" debe estar en gestor-detalle.html
$gestorDetalleFile = "src/main/resources/templates/bpo/admin/gestor-detalle.html"
$gestorDetallePath = Join-Path $env:WORKSPACE_ROOT $gestorDetalleFile
if (Test-Path $gestorDetallePath) {
    $content = Get-Content $gestorDetallePath -Raw
    if ($content -match 'Crear nueva carpeta') {
        Write-Host "PASS: Boton 'Crear nueva carpeta' existe en gestor-detalle.html" -ForegroundColor Green
    } else {
        Write-Host "FAIL: Boton 'Crear nueva carpeta' NO encontrado" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test 8.3: Metodo crearSubcarpeta en CuentaBPOService
$serviceFile = "src/main/java/com/lms/service/bpo/CuentaBPOService.java"
$servicePath = Join-Path $env:WORKSPACE_ROOT $serviceFile
if (Test-Path $servicePath) {
    $serviceContent = Get-Content $servicePath -Raw
    if ($serviceContent -match 'public SubServicioBPO crearSubcarpeta') {
        Write-Host "PASS: Metodo crearSubcarpeta existe en CuentaBPOService" -ForegroundColor Green
    } else {
        Write-Host "FAIL: Metodo crearSubcarpeta NO encontrado" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test 8.4: Endpoint GET /gestores/{gestorId}/subcarpetas/nuevo
$controllerFile = "src/main/java/com/lms/controller/bpo/AdminBPOController.java"
$controllerPath = Join-Path $env:WORKSPACE_ROOT $controllerFile
if (Test-Path $controllerPath) {
    $ctrlContent = Get-Content $controllerPath -Raw
    if ($ctrlContent -match 'formularioNuevaSubcarpeta') {
        Write-Host "PASS: Endpoint GET subcarpetas/nuevo existe" -ForegroundColor Green
    } else {
        Write-Host "FAIL: Endpoint GET subcarpetas/nuevo NO encontrado" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test 8.5: Endpoint POST /gestores/{gestorId}/subcarpetas
if (Test-Path $controllerPath) {
    $ctrlContent = Get-Content $controllerPath -Raw
    if ($ctrlContent -match 'public String crearSubcarpeta') {
        Write-Host "PASS: Endpoint POST subcarpetas existe" -ForegroundColor Green
    } else {
        Write-Host "FAIL: Endpoint POST subcarpetas NO encontrado" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test 8.6: Migracion V25 debe existir
$migrationFile = "src/main/resources/db/migration/V25__agregar_codigo_subservicios_bpo.sql"
$migrationPath = Join-Path $env:WORKSPACE_ROOT $migrationFile
if (Test-Path $migrationPath) {
    Write-Host "PASS: Migracion V25 existe" -ForegroundColor Green
} else {
    Write-Host "FAIL: Migracion V25 NO existe" -ForegroundColor Red
    $allPassed = $false
}

# Test 8.7: formulario-subcarpeta.html debe tener token CSRF (BUG-016)
if (Test-Path $fullPath) {
    $content = Get-Content $fullPath -Raw
    if ($content -match 'type="hidden".*th:name="\$\{_csrf\.parameterName\}".*th:value="\$\{_csrf\.token\}"') {
        Write-Host "PASS: Token CSRF presente en formulario-subcarpeta.html" -ForegroundColor Green
    } else {
        Write-Host "FAIL: Token CSRF NO encontrado en formulario-subcarpeta.html" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test 8.8: formulario-subcarpeta.html debe usar gestor.codigo (no gestor.id) (BUG-016)
if (Test-Path $fullPath) {
    $content = Get-Content $fullPath -Raw
    if ($content -match 'id=\$\{gestor\.codigo\}' -and -not ($content -match 'id=\$\{gestor\.id\}')) {
        Write-Host "PASS: formulario-subcarpeta.html usa gestor.codigo (no gestor.id)" -ForegroundColor Green
    } else {
        Write-Host "FAIL: formulario-subcarpeta.html usa gestor.id en lugar de gestor.codigo" -ForegroundColor Red
        $allPassed = $false
    }
}

if ($allPassed) {
    Write-Host "`nRESUMEN: FASE 2 (Crear subcarpeta) COMPLETA - Todos los tests PASARON" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`nRESUMEN: FASE 2 (Crear subcarpeta) INCOMPLETA - Tests FALLARON" -ForegroundColor Red
    exit 1
}
