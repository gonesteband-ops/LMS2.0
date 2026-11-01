# Test 11: BUG-016 - CSRF en formularios POST (subcarpeta y publicacion)
# Verifica:
# - formulario-subcarpeta.html tiene token CSRF oculto
# - formulario-subcarpeta.html usa gestor.codigo en el th:action
# - formulario-publicacion.html tiene token CSRF
# - formulario-publicacion.html usa codigo (no UUID) en URLs

$ErrorActionPreference = "Stop"
$allPassed = $true

# Test 11.1: formulario-subcarpeta.html debe tener token CSRF
$subcarpetaFile = "src/main/resources/templates/bpo/admin/formulario-subcarpeta.html"
$subcarpetaPath = Join-Path $env:WORKSPACE_ROOT $subcarpetaFile
if (Test-Path $subcarpetaPath) {
    $content = Get-Content $subcarpetaPath -Raw
    
    # Verificar que tenga token CSRF
    if ($content -match 'type="hidden".*th:name="\$\{_csrf\.parameterName\}".*th:value="\$\{_csrf\.token\}"') {
        Write-Host "PASS: Token CSRF presente en formulario-subcarpeta.html" -ForegroundColor Green
    } else {
        Write-Host "FAIL: Token CSRF NO encontrado en formulario-subcarpeta.html" -ForegroundColor Red
        $allPassed = $false
    }
    
    # Verificar que usa gestor.codigo en el action
    if ($content -match 'th:action="@\{/admin/bpo/gestores/\{id\}/subcarpetas\(id=\$\{gestor\.codigo\}\)') {
        Write-Host "PASS: formulario-subcarpeta.html usa gestor.codigo en th:action" -ForegroundColor Green
    } else {
        Write-Host "FAIL: formulario-subcarpeta.html no usa gestor.codigo en th:action" -ForegroundColor Red
        $allPassed = $false
    }
    
    # Verificar que usa gestor.codigo en breadcrumb
    if ($content -match '@\{/admin/bpo/gestores/\{id\}\(id=\$\{gestor\.codigo\}\)' -and -not ($content -match '@\{/admin/bpo/gestores/\{id\}\(id=\$\{gestor\.id\}\)')) {
        Write-Host "PASS: formulario-subcarpeta.html usa gestor.codigo en breadcrumb y enlaces" -ForegroundColor Green
    } else {
        Write-Host "FAIL: formulario-subcarpeta.html aun usa gestor.id en lugar de gestor.codigo" -ForegroundColor Red
        $allPassed = $false
    }
} else {
    Write-Host "FAIL: No se encuentra $subcarpetaFile" -ForegroundColor Red
    $allPassed = $false
}

# Test 11.2: formulario-publicacion.html debe tener token CSRF
$publicacionFile = "src/main/resources/templates/bpo/admin/formulario-publicacion.html"
$publicacionPath = Join-Path $env:WORKSPACE_ROOT $publicacionFile
if (Test-Path $publicacionPath) {
    $content = Get-Content $publicacionPath -Raw
    
    # Verificar que tenga token CSRF
    if ($content -match 'type="hidden".*th:name="\$\{_csrf\.parameterName\}".*th:value="\$\{_csrf\.token\}"') {
        Write-Host "PASS: Token CSRF presente en formulario-publicacion.html" -ForegroundColor Green
    } else {
        Write-Host "FAIL: Token CSRF NO encontrado en formulario-publicacion.html" -ForegroundColor Red
        $allPassed = $false
    }
    
    # Verificar que usa codigo en el action
    if ($content -match 'gid=\$\{gestor\.codigo\}' -and $content -match 'sid=\$\{subcarpeta\.codigo\}') {
        Write-Host "PASS: formulario-publicacion.html usa gestor.codigo y subcarpeta.codigo" -ForegroundColor Green
    } else {
        Write-Host "FAIL: formulario-publicacion.html no usa codigo en th:action" -ForegroundColor Red
        $allPassed = $false
    }
    
    # Verificar que tiene enctype multipart
    if ($content -match 'enctype="multipart/form-data"') {
        Write-Host "PASS: formulario-publicacion.html tiene enctype multipart" -ForegroundColor Green
    } else {
        Write-Host "FAIL: formulario-publicacion.html sin enctype multipart" -ForegroundColor Red
        $allPassed = $false
    }
} else {
    Write-Host "FAIL: No se encuentra $publicacionFile" -ForegroundColor Red
    $allPassed = $false
}

if ($allPassed) {
    Write-Host "`nRESUMEN: BUG-016 (CSRF en POST) VERIFICADO - Todos los tests PASARON" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`nRESUMEN: BUG-016 (CSRF en POST) INCOMPLETO - Tests FALLARON" -ForegroundColor Red
    exit 1
}
