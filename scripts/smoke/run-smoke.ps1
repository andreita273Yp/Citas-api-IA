# Prueba de humo REST contra la app real (Windows PowerShell 5.1), desde cualquier carpeta:
#   powershell -ExecutionPolicy Bypass -File citas-api\scripts\smoke\run-smoke.ps1
# Recrea la base de desarrollo con los datos demo sintéticos (db/seed), levanta la API en citas-api-dev y
# ejecuta 01-base.ps1 … 06-fase6.ps1. Al final deja la base limpia de nuevo. No lee el archivo .env: el nombre
# de la base se toma de la variable MYSQL_DATABASE del contenedor mysql.
$ErrorActionPreference = 'Continue'
$dir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location (Resolve-Path (Join-Path $dir '..\..\..'))
$env:Path = [Environment]::GetEnvironmentVariable('Path', 'Machine') + ';' + [Environment]::GetEnvironmentVariable('Path', 'User')

function Reset-DemoDatabase {
    docker compose restart citas-api-dev 2>&1 | Out-Null
    'DROP DATABASE IF EXISTS `' + $database + '`; CREATE DATABASE `' + $database + '` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;' |
        docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" 2>/dev/null'
    docker compose exec -d citas-api-dev sh -c "mvn -q -B spring-boot:run > /tmp/api.log 2>&1"
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 5
        try { Invoke-WebRequest -UseBasicParsing http://localhost:8080/actuator/health -TimeoutSec 5 | Out-Null; return } catch { }
    }
    throw 'La API no respondió en http://localhost:8080/actuator/health'
}

$database = (docker compose exec -T mysql sh -c 'printf %s "$MYSQL_DATABASE"')
if (-not $database) { throw 'No se pudo leer MYSQL_DATABASE del contenedor mysql (¿docker compose up -d?)' }

# PowerShell 5.1 necesita BOM por las tildes: se une todo en un archivo temporal con BOM.
$parts = Get-ChildItem $dir -Filter '0*.ps1' | Sort-Object Name | ForEach-Object { [IO.File]::ReadAllText($_.FullName, [Text.Encoding]::UTF8) }
$combined = Join-Path $env:TEMP 'citas-smoke-all.ps1'
[IO.File]::WriteAllText($combined, ($parts -join "`n"), (New-Object Text.UTF8Encoding($true)))

Reset-DemoDatabase
try {
    $out = & $combined
    $out
    ''
    'PASS: ' + @($out | Where-Object { $_ -match ' PASS ' }).Count
    'FAIL: ' + @($out | Where-Object { $_ -match ' FAIL ' }).Count
} catch {
    "ERROR SCRIPT: $_ en línea $($_.InvocationInfo.ScriptLineNumber)"
}
'Errores en el log de la API: ' + (docker compose exec -T citas-api-dev sh -c "grep -c ' ERROR ' /tmp/api.log")
Reset-DemoDatabase
'Base de desarrollo restaurada con los datos demo.'
