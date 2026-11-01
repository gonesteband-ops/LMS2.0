# Test 07: BUG-015 FASE 1 - CSRF token en formulario-publicacion.html
# Verifica que el token CSRF este agregado al formulario multipart

$ErrorActionPreference = "Stop"
$templateFile = "src/main/resources/templates/bpo/admin/formulario-publicacion.html"
$fullPath = Join-Path $env:WORKSPACE_ROOT $templateFile

if (-not (Test-Path $fullPath)) {
    Write-Host "FAIL: No se encuentra $templateFile" -ForegroundColor Red
    exit 1
}

$content = Get-Content $fullPath -Raw
$allPassed = $true

# Test 7.1: Debe tener enctype="multipart/form-data"
if ($content -match 'enctype="multipart/form-data"') {
    Write-Host "PASS: formulario tiene enctype multipart" -ForegroundColor Green
} else {
    Write-Host "FAIL: formulario sin enctype multipart" -ForegroundColor Red
    $allPassed = $false
}

# Test 7.2: Debe tener input type hidden para CSRF
if ($content -match 'type="hidden".*th:name="\$\{_csrf\.parameterName\}".*th:value="\$\{_csrf\.token\}"') {
    Write-Host "PASS: Token CSRF presente en formulario" -ForegroundColor Green
} else {
    Write-Host "FAIL: Token CSRF NO encontrado" -ForegroundColor Red
    $allPassed = $false
}

# Test 7.3: El token debe estar DENTRO del formulario (antes de los campos)
$formStart = $content.IndexOf('<form')
$formEnd = $content.IndexOf('</form>')
if ($formStart -gt -1 -and $formEnd -gt -1) {
    $formContent = $content.Substring($formStart, $formEnd - $formStart)
    if ($formContent -match 'type="hidden".*_csrf') {
        Write-Host "PASS: Token CSRF esta dentro del formulario" -ForegroundColor Green
    } else {
        Write-Host "FAIL: Token CSRF no esta dentro del formulario" -ForegroundColor Red
        $allPassed = $false
    }
}

if ($allPassed) {
    Write-Host "`nRESUMEN: FASE 1 (CSRF) COMPLETA - Todos los tests PASARON" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`nRESUMEN: FASE 1 (CSRF) INCOMPLETA - Tests FALLARON" -ForegroundColor Red
    exit 1
}
