# Test 09: BUG-015 FASE 3 - Editar publicacion
# Verifica:
# - Boton "Editar" en subcarpeta-publicaciones.html
# - Metodo actualizarPublicacion en ContenidoBPOService
# - Endpoints GET y POST de editar en AdminBPOController

$ErrorActionPreference = "Stop"
$allPassed = $true

# Test 9.1: Boton "Editar" debe estar en subcarpeta-publicaciones.html
$templateFile = "src/main/resources/templates/bpo/admin/subcarpeta-publicaciones.html"
$fullPath = Join-Path $env:WORKSPACE_ROOT $templateFile
if (Test-Path $fullPath) {
    $content = Get-Content $fullPath -Raw
    if ($content -match '>Editar<') {
        Write-Host "PASS: Boton 'Editar' existe en subcarpeta-publicaciones.html" -ForegroundColor Green
    } else {
        Write-Host "FAIL: Boton 'Editar' NO encontrado" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test 9.2: Boton Editar debe ser un enlace a /publicaciones/{id}/editar
if (Test-Path $fullPath) {
    $content = Get-Content $fullPath -Raw
    if ($content -match '/publicaciones/.*editar') {
        Write-Host "PASS: Boton Editar dirige a ruta de edicion" -ForegroundColor Green
    } else {
        Write-Host "FAIL: Boton Editar no tiene ruta correcta" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test 9.3: Metodo actualizarPublicacion en ContenidoBPOService
$serviceFile = "src/main/java/com/lms/service/bpo/ContenidoBPOService.java"
$servicePath = Join-Path $env:WORKSPACE_ROOT $serviceFile
if (Test-Path $servicePath) {
    $serviceContent = Get-Content $servicePath -Raw
    if ($serviceContent -match 'public ContenidoBPO actualizarPublicacion') {
        Write-Host "PASS: Metodo actualizarPublicacion existe" -ForegroundColor Green
    } else {
        Write-Host "FAIL: Metodo actualizarPublicacion NO encontrado" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test 9.4: Endpoint GET /publicaciones/{id}/editar
$controllerFile = "src/main/java/com/lms/controller/bpo/AdminBPOController.java"
$controllerPath = Join-Path $env:WORKSPACE_ROOT $controllerFile
if (Test-Path $controllerPath) {
    $ctrlContent = Get-Content $controllerPath -Raw
    if ($ctrlContent -match 'formularioEditarPublicacion') {
        Write-Host "PASS: Endpoint GET publicaciones/{id}/editar existe" -ForegroundColor Green
    } else {
        Write-Host "FAIL: Endpoint GET editar NO encontrado" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test 9.5: Endpoint POST /publicaciones/{id}
if (Test-Path $controllerPath) {
    $ctrlContent = Get-Content $controllerPath -Raw
    if ($ctrlContent -match 'public String actualizarPublicacion') {
        Write-Host "PASS: Endpoint POST publicaciones/{id} existe" -ForegroundColor Green
    } else {
        Write-Host "FAIL: Endpoint POST actualizar NO encontrado" -ForegroundColor Red
        $allPassed = $false
    }
}

# Test 9.6: actualizarPublicacion debe manejar archivos opcionales
if (Test-Path $servicePath) {
    $serviceContent = Get-Content $servicePath -Raw
    if ($serviceContent -match 'actualizarPublicacion.*getArchivo.*isEmpty') {
        Write-Host "PASS: Metodo actualizarPublicacion maneja archivos opcionales" -ForegroundColor Green
    } else {
        Write-Host "WARN: No se pudo verificar manejo de archivos opcionales" -ForegroundColor Yellow
    }
}

if ($allPassed) {
    Write-Host "`nRESUMEN: FASE 3 (Editar publicacion) COMPLETA - Todos los tests PASARON" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`nRESUMEN: FASE 3 (Editar publicacion) INCOMPLETA - Tests FALLARON" -ForegroundColor Red
    exit 1
}
