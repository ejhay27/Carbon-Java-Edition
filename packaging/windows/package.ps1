param(
  [ValidateSet('app-image', 'exe', 'msi')]
  [string]$Type = 'app-image',
  [switch]$SkipTests
)
$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $Root
if (-not $IsWindows -and $env:OS -ne 'Windows_NT') {
  throw 'Windows jpackage output must be built on Windows. Cross-building an EXE/MSI on Linux is not supported.'
}
foreach ($command in @('mvn', 'jpackage')) {
  if (-not (Get-Command $command -ErrorAction SilentlyContinue)) { throw "$command is required on PATH (JDK 21+ and Maven 3.9+)." }
}
$verify = if ($SkipTests) { 'package' } else { 'verify' }
& mvn -B clean $verify
if ($LASTEXITCODE -ne 0) { throw "Maven failed with exit code $LASTEXITCODE" }
$Input = Join-Path $Root 'target\jpackage'
$MainJar = Join-Path $Input 'Carbon-Java-Edition.jar'
New-Item -ItemType Directory -Force -Path $Input | Out-Null
Copy-Item (Join-Path $Root 'target\Carbon-Java-Edition.jar') $MainJar -Force
if (-not (Test-Path $MainJar)) { throw 'Main application JAR was not staged.' }
$Dist = Join-Path $Root 'target\dist'
New-Item -ItemType Directory -Force -Path $Dist | Out-Null
$arguments = @(
  '--type', $Type,
  '--name', 'Carbon-Java-Edition',
  '--app-version', '7.9.9.1',
  '--vendor', 'Carbon',
  '--description', 'Carbon: Java Edition - local-first drawing studio',
  '--input', $Input,
  '--main-jar', 'Carbon-Java-Edition.jar',
  '--main-class', 'com.carbon.studio.CarbonLauncher',
  '--dest', $Dist,
  '--icon', (Join-Path $Root 'packaging\windows\carbon.ico'),
  '--add-modules', 'java.base,java.desktop,java.logging,java.sql,java.xml,jdk.unsupported',
  '--verbose'
)
if ($Type -in @('exe', 'msi')) {
  $arguments += @('--win-menu', '--win-menu-group', 'Carbon', '--win-shortcut', '--win-dir-chooser')
}
& jpackage @arguments
if ($LASTEXITCODE -ne 0) { throw "jpackage failed with exit code $LASTEXITCODE" }
if ($Type -eq 'app-image') {
  $Zip = Join-Path $Dist 'Carbon-Java-Edition-windows-x64-app-image.zip'
  if (Test-Path $Zip) { Remove-Item $Zip -Force }
  Compress-Archive -Path (Join-Path $Dist 'Carbon-Java-Edition\*') -DestinationPath $Zip
  Write-Host "Application image: $Zip"
} else {
  Write-Host "Installer output: $Dist"
}
