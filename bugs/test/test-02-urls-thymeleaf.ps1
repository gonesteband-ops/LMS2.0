# Test 02: Verificar que las URLs Thymeleaf usan sintaxis correcta {var}(var=${...})
$ErrorActionPreference = "Stop"
$templateFile = "src/main/resources/templates/bpo/admin/subcarpeta-publicaciones.html"
$fullPath = Join-Path $env:WORKSPACE_ROOT $templateFile

if (-not (Test-Path $fullPath)) {
    Write-Host "FAIL: No se encuentra $templateFile" -ForegroundColor Red
    exit 1
}

$lines = Get-Content $fullPath
$allPassed = $true

# Linea 14 - breadcrumb gestor
$l14 = $lines[13]
if ($l14 -match '\\$\\{gestor\\.id\\}') {
    Write-Host "FAIL [Linea 14]: Usa sintaxis INVALIDA" -ForegroundColor Red
    $allPassed = $false
} elseif ($l14 -match 'id=\\${gestor\\.id\\}') {
    Write-Host "PASS [Linea 14]: Sintaxis correcta" -ForegroundColor Green
} else {
    Write-Host "WARN [Linea 14]: No se pudo determinar" -ForegroundColor Yellow
}

# Linea 20 - boton Nueva Publicacion
$l20 = $lines[19]
if ($l20 -match '\\$\\{gestor\\.id\\}/subcarpetas/\\$\\{') {
    Write-Host "FAIL [Linea 20]: Usa sintaxis INVALIDA" -ForegroundColor Red
    $allPassed = $false
} elseif ($l20 -match 'gid=\\${gestor\\.id\\},sid=\\${subcarpeta\\.id\\}') {
    Write-Host "PASS [Linea 20]: Sintaxis correcta" -ForegroundColor Green
} else {
    Write-Host "WARN [Linea 20]: No se pudo determinar" -ForegroundColor Yellow
}

# Linea 26 - link "Crear la primera"
$l26 = $lines[25]
if ($l26 -match '\\$\\{gestor\\.id\\}/subcarpetas/\\$\\{') {
    Write-Host "FAIL [Linea 26]: Usa sintaxis INVALIDA" -ForegroundColor Red
    $allPassed = $false
} elseif ($l26 -match 'gid=\\${gestor\\.id\\},sid=\\${subcarpeta\\.id\\}') {
    Write-Host "PASS [Linea 26]: Sintaxis correcta" -ForegroundColor Green
} else {
    Write-Host "WARN [Linea 26]: No se pudo determinar" -ForegroundColor Yellow
}

# Linea 36 - form eliminar
$l36 = $lines[35]
if ($l36 -match 'publicaciones/\\$\\{') {
    Write-Host "FAIL [Linea 36]: Usa sintaxis INVALIDA" -ForegroundColor Red
    $allPassed = $false
} elseif ($l36 -match 'publicaciones/\\{id\\}\\(id=\\${c\\.id\\}\\)') {
    Write-Host "PASS [Linea 36]: Sintaxis correcta" -ForegroundColor Green
} else {
    Write-Host "WARN [Linea 36]: No se pudo determinar" -ForegroundColor Yellow
}

if ($allPassed) {
    Write-Host "`nRESUMEN: Todos los tests de URLs PASARON" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`nRESUMEN: Algunos tests FALLARON" -ForegroundColor Red
    exit 1
}
