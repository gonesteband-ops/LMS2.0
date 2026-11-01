# Script para ejecutar todos los tests de validacion del Bug-014
# Modo de uso: powershell -File run-all-tests.ps1

$ErrorActionPreference = "Continue"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$workspaceRoot = Resolve-Path "$scriptDir/../.."

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Ejecutando tests de validacion BUG-014" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Workspace: $workspaceRoot" -ForegroundColor Gray
Write-Host ""

$env:WORKSPACE_ROOT = $workspaceRoot

$tests = @(
    @{Name="01 - crearCuenta en Service"; File="test-01-crear-cuenta.ps1"},
    @{Name="02 - URLs Thymeleaf"; File="test-02-urls-thymeleaf.ps1"},
    @{Name="03 - @ModelAttribute Controller"; File="test-03-controller.ps1"},
    @{Name="04 - Botones btn-primary"; File="test-04-botones.ps1"},
    @{Name="05 - Formulario publicacion"; File="test-05-formulario-publicacion.ps1"},
    @{Name="06 - Funcional (HTTP + CSS)"; File="test-06-funcional.ps1"}
)

$passed = 0
$failed = 0

foreach ($test in $tests) {
    Write-Host "----------------------------------------" -ForegroundColor Gray
    Write-Host "[TEST] $($test.Name)" -ForegroundColor Yellow
    $testPath = Join-Path $scriptDir $test.File
    
    if (-not (Test-Path $testPath)) {
        Write-Host "  ERROR: No se encuentra el test: $($test.File)" -ForegroundColor Red
        $failed++
        continue
    }
    
    & powershell -File $testPath 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        $passed++
    } else {
        $failed++
    }
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  RESULTADOS FINALES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Tests pasados: $passed" -ForegroundColor Green
Write-Host "  Tests fallados: $failed" -ForegroundColor Red
Write-Host "  Total: $($tests.Count)" -ForegroundColor White
Write-Host ""

if ($failed -eq 0) {
    Write-Host "  TODOS LOS TESTS PASARON - Cambios listos para commit" -ForegroundColor Green
    exit 0
} else {
    Write-Host "  HAY TESTS FALLADOS - Revisa las correcciones pendientes" -ForegroundColor Red
    exit 1
}
