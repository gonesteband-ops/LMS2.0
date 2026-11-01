# Test 01: Verificar que existe el metodo crearCuenta en CuentaBPOService
# Este test falla ANTES de la correccion y pasa DESPUES

$ErrorActionPreference = "Stop"
$serviceFile = "src/main/java/com/lms/service/bpo/CuentaBPOService.java"
$fullPath = Join-Path $env:WORKSPACE_ROOT $serviceFile

if (-not (Test-Path $fullPath)) {
    Write-Host "FAIL: No se encuentra $serviceFile" -ForegroundColor Red
    exit 1
}

$content = Get-Content $fullPath -Raw

# Test 1.1: Verificar que existe el metodo crearCuenta
if ($content -match 'public\s+CuentaBPO\s+crearCuenta\s*\(\s*CuentaBPO\s+\w+\s*\)') {
    Write-Host "PASS: metodo crearCuenta(CuentaBPO) existe" -ForegroundColor Green
} else {
    Write-Host "FAIL: metodo crearCuenta(CuentaBPO) NO existe" -ForegroundColor Red
    Write-Host "  Buscaba: public CuentaBPO crearCuenta(CuentaBPO ...)"
    exit 1
}

# Test 1.2: Verificar que llama a cuentaRepository.save()
if ($content -match 'cuentaRepository\.save\s*\(\s*\w+\s*\)') {
    Write-Host "PASS: crearCuenta llama a cuentaRepository.save()" -ForegroundColor Green
} else {
    Write-Host "FAIL: crearCuenta NO llama a cuentaRepository.save()" -ForegroundColor Red
    exit 1
}

Write-Host "`nRESUMEN: Todos los tests de crearCuenta PASARON" -ForegroundColor Green
exit 0
