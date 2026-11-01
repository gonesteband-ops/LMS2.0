# Test 05: Verificar correcciones en formulario-publicacion.html
# - th:object="${publicacion}" en vez de th:object=""
# - Sin th:href duplicado
# - URL de Cancelar con parametros correctos

$ErrorActionPreference = "Stop"
$templateFile = "src/main/resources/templates/bpo/admin/formulario-publicacion.html"
$fullPath = Join-Path $env:WORKSPACE_ROOT $templateFile

if (-not (Test-Path $fullPath)) {
    Write-Host "FAIL: No se encuentra $templateFile" -ForegroundColor Red
    exit 1
}

$content = Get-Content $fullPath

$allPassed = $true

# Test 5.1: th:object debe ser "${publicacion}"
$line22 = $content[21]  # 0-indexed, linea 22
if ($line22 -match 'th:object=""') {
    Write-Host "FAIL [Linea 22]: th:object=\"\" AUN esta vacio" -ForegroundColor Red
    $allPassed = $false
} elseif ($line22 -match 'th:object="\$\{publicacion\}"') {
    Write-Host "PASS [Linea 22]: th:object apunta a publicacion" -ForegroundColor Green
} else {
    Write-Host "WARN [Linea 22]: th:object no se pudo verificar" -ForegroundColor Yellow
    Write-Host "  Contenido: $line22"
}

# Test 5.2: No debe tener th:href duplicado
$line14 = $content[13]  # 0-indexed, linea 14
$hrefCount = [regex]::Matches($line14, 'th:href').Count
if ($hrefCount -gt 1) {
    Write-Host "FAIL [Linea 14]: Tiene $hrefCount th:href duplicados" -ForegroundColor Red
    $allPassed = $false
} elseif ($hrefCount -eq 1) {
    Write-Host "PASS [Linea 14]: Un solo th:href" -ForegroundColor Green
} else {
    Write-Host "WARN [Linea 14]: No se encontro th:href" -ForegroundColor Yellow
}

# Test 5.3: URL del boton Cancelar debe tener parametros
$line53 = $content[52]  # 0-indexed, linea 53
if ($line53 -match 'gid=,sid=') {
    Write-Host "FAIL [Linea 53]: Cancelar tiene parametros VACIOS (gid=,sid=)" -ForegroundColor Red
    $allPassed = $false
} elseif ($line53 -match 'gid=\$\{gestor\.id\},sid=\$\{subcarpeta\.id\}') {
    Write-Host "PASS [Linea 53]: Cancelar tiene parametros correctos" -ForegroundColor Green
} else {
    Write-Host "WARN [Linea 53]: No se pudo verificar parametros de Cancelar" -ForegroundColor Yellow
}

if ($allPassed) {
    Write-Host "`nRESUMEN: Todos los tests del Formulario PASARON" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`nRESUMEN: Algunos tests del Formulario FALLARON" -ForegroundColor Red
    exit 1
}
