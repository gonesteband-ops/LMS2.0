---
name: quick-server
description: "Inicia el servidor Spring Boot rápidamente liberando el puerto."
---

# Quick Server PowerShell

```powershell
$port = 8080
$connection = Get-NetTCPConnection -LocalPort$port -ErrorAction SilentlyContinue
if ($connection) {
    $pidToKill =$connection.OwningProcess
    Write-Host "Matando proceso anterior en el puerto $port (PID:$pidToKill)..."
    Stop-Process -Id $pidToKill -Force
}
Write-Host "Iniciando Spring Boot..."
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=true"