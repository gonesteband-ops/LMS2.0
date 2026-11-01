# Test 03: Verificar @ModelAttribute con nombre "publicacion" en AdminBPOController
$ErrorActionPreference = "Stop"
$controllerFile = "src/main/java/com/lms/controller/bpo/AdminBPOController.java"
$fullPath = Join-Path $env:WORKSPACE_ROOT $controllerFile

if (-not (Test-Path $fullPath)) {
    Write-Host "FAIL: No se encuentra $controllerFile" -ForegroundColor Red
    exit 1
}

$content = Get-Content $fullPath -Raw
$allPassed = $true

# Test 3.1: @ModelAttribute("publicacion") 
if ($content -match '@ModelAttribute\("publicacion"\)\s+ContenidoBPOCreateDTO\s+dto') {
    Write-Host "PASS: @ModelAttribute(\"publicacion\") existe" -ForegroundColor Green
} else {
    Write-Host "FAIL: @ModelAttribute(\"publicacion\") NO encontrado" -ForegroundColor Red
    $allPassed = $false
}

# Test 3.2: Sin @ModelAttribute sin nombre
if ($content -match '(?<!@ModelAttribute\("publicacion"\))\s+@ModelAttribute\s+ContenidoBPOCreateDTO\s+dto') {
    Write-Host "FAIL: AUN existe @ModelAttribute sin nombre" -ForegroundColor Red
    $allPassed = $false
} else {
    Write-Host "PASS: No hay @ModelAttribute sin nombre" -ForegroundColor Green
}

# Test 3.3: POST /gestores existe
if ($content -match 'PostMapping\("/gestores"\)') {
    Write-Host "PASS: POST /gestores existe" -ForegroundColor Green
} else {
    Write-Host "FAIL: POST /gestores NO existe" -ForegroundColor Red
    $allPassed = $false
}

# Test 3.4: crearGestor llama a crearCuenta
if ($content -match 'cuentaService\.crearCuenta\(') {
    Write-Host "PASS: crearGestor llama a cuentaService.crearCuenta()" -ForegroundColor Green
} else {
    Write-Host "FAIL: crearGestor NO llama a crearCuenta()" -ForegroundColor Red
    $allPassed = $false
}

if ($allPassed) {
    Write-Host "`nRESUMEN: Tests del Controller PASARON" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`nRESUMEN: Tests del Controller FALLARON" -ForegroundColor Red
    exit 1
}
