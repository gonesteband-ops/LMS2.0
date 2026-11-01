# Test 04: Verificar que los botones usan btn-primary consistente
# Test 4.1: gestor-detalle.html - boton Volver debe ser btn-primary
# Test 4.2: subcarpeta-publicaciones.html - boton + Nueva Publicacion debe ser btn-primary

$ErrorActionPreference = "Stop"

$allPassed = $true

# Test 4.1: Boton Volver en gestor-detalle.html
$detalleFile = "src/main/resources/templates/bpo/admin/gestor-detalle.html"
$detallePath = Join-Path $env:WORKSPACE_ROOT $detalleFile
$detalleContent = Get-Content $detallePath -Raw

if ($detalleContent -match 'btn-ghost') {
    Write-Host "FAIL [gestor-detalle.html]: AUN usa btn-ghost" -ForegroundColor Red
    $allPassed = $false
} elseif ($detalleContent -match 'class="btn\s+btn-primary"') {
    Write-Host "PASS [gestor-detalle.html]: Boton Volver usa btn btn-primary" -ForegroundColor Green
} else {
    Write-Host "WARN [gestor-detalle.html]: No se encontro ni btn-ghost ni btn-primary" -ForegroundColor Yellow
}

# Test 4.2: Boton + Nueva Publicacion en subcarpeta-publicaciones.html
$subcarpetaFile = "src/main/resources/templates/bpo/admin/subcarpeta-publicaciones.html"
$subcarpetaPath = Join-Path $env:WORKSPACE_ROOT $subcarpetaFile
$subcarpetaContent = Get-Content $subcarpetaPath -Raw

if ($subcarpetaContent -match 'bpo-btn-primary') {
    Write-Host "FAIL [subcarpeta-publicaciones.html]: AUN usa bpo-btn-primary" -ForegroundColor Red
    $allPassed = $false
} elseif ($subcarpetaContent -match 'class="btn\s+btn-primary"') {
    Write-Host "PASS [subcarpeta-publicaciones.html]: Boton + Nueva Publicacion usa btn btn-primary" -ForegroundColor Green
} else {
    Write-Host "WARN [subcarpeta-publicaciones.html]: No se encontro btn-primary" -ForegroundColor Yellow
}

if ($allPassed) {
    Write-Host "`nRESUMEN: Todos los tests de Botones PASARON" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`nRESUMEN: Algunos tests de Botones FALLARON" -ForegroundColor Red
    exit 1
}
