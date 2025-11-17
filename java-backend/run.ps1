Param()

$ErrorActionPreference = "Stop"

Write-Host "Compilando Java backend..."

$srcDir = Join-Path $PSScriptRoot "src"
$outDir = Join-Path $PSScriptRoot "out"

if (-Not (Test-Path $outDir)) {
  New-Item -ItemType Directory -Path $outDir | Out-Null
}

$javaFiles = Get-ChildItem -Path $srcDir -Filter *.java -Recurse | ForEach-Object { $_.FullName }

$libDir = Join-Path $PSScriptRoot "lib"
if (-Not (Test-Path $libDir)) { New-Item -ItemType Directory -Path $libDir | Out-Null }
$pgJar = Join-Path $libDir "postgresql.jar"
if (-Not (Test-Path $pgJar)) {
  Write-Host "Driver PostgreSQL não encontrado em $pgJar" -ForegroundColor Red
  Write-Host "Baixe o JDBC e salve como 'java-backend/lib/postgresql.jar' (ex.: 42.7.x)" -ForegroundColor Yellow
  throw "Arquivo postgresql.jar ausente"
}

javac --release 17 -cp $pgJar -d $outDir $javaFiles

Write-Host "Iniciando servidor..."

$pgJarRun = Join-Path $PSScriptRoot "lib\postgresql.jar"
$slf4jApi = Join-Path $PSScriptRoot "lib\slf4j-api.jar"
$slf4jNop = Join-Path $PSScriptRoot "lib\slf4j-nop.jar"
$cp = "$outDir;${pgJarRun};${slf4jApi};${slf4jNop}"
if (-Not $env:PG_URL -and (-Not $env:PG_HOST -or -Not $env:PG_DB)) {
  Write-Host "Variáveis PG_URL ou PG_HOST/PG_DB não definidas" -ForegroundColor Yellow
  Write-Host "Defina PG_URL=jdbc:postgresql://host:port/db ou PG_HOST/PG_PORT/PG_DB/PG_USER/PG_PASSWORD" -ForegroundColor Yellow
}
if (-Not $env:PG_PASSWORD) {
  Write-Host "PG_PASSWORD não definido. Se o servidor exigir senha, a conexão falhará." -ForegroundColor Yellow
}
java --enable-native-access=ALL-UNNAMED -cp $cp com.hospital.tickets.Main