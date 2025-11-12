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
$sqliteJar = Join-Path $libDir "sqlite-jdbc.jar"

javac --release 17 -cp $sqliteJar -d $outDir $javaFiles

Write-Host "Iniciando servidor..."

$sqliteJarRun = Join-Path $PSScriptRoot "lib\sqlite-jdbc.jar"
$slf4jApi = Join-Path $PSScriptRoot "lib\slf4j-api.jar"
$slf4jNop = Join-Path $PSScriptRoot "lib\slf4j-nop.jar"
$cp = "$outDir;${sqliteJarRun};${slf4jApi};${slf4jNop}"
java --enable-native-access=ALL-UNNAMED -cp $cp com.hospital.tickets.Main