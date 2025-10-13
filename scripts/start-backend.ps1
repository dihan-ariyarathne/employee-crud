param(
  [int]$Port = 8080,
  [string]$JavaHome = "C:\\Users\\HP\\Desktop\\Siyoth\\jdk-21.0.8",
  [string]$MavenHome = "C:\\Users\\HP\\Desktop\\Siyoth\\apache-maven-3.9.11-bin\\apache-maven-3.9.11"
)

$ErrorActionPreference = 'Stop'

# Resolve repo root relative to this script and locate backend directory
$repoRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $repoRoot 'backend'
if (!(Test-Path $backendDir)) { throw "Backend directory not found: $backendDir" }

Write-Host "Configuring JAVA_HOME and Maven..."
if (!(Test-Path $JavaHome)) { throw "JAVA home not found: $JavaHome" }
if (!(Test-Path $MavenHome)) { throw "Maven home not found: $MavenHome" }

$javaBin = Join-Path $JavaHome 'bin'
$mvnBin = Join-Path $MavenHome 'bin'
$env:JAVA_HOME = $JavaHome
$env:MAVEN_HOME = $MavenHome
if (($env:PATH -split ';') -notcontains $javaBin) { $env:PATH = "$javaBin;$env:PATH" }
if (($env:PATH -split ';') -notcontains $mvnBin) { $env:PATH = "$mvnBin;$env:PATH" }

# Load backend/.env (KEY=VALUE lines)
$envFile = Join-Path $backendDir '.env'
if (Test-Path $envFile) {
  Write-Host "Loading env from $envFile"
  Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
      $idx = $line.IndexOf('=')
      $k = $line.Substring(0,$idx)
      $v = $line.Substring($idx+1)
      [Environment]::SetEnvironmentVariable($k, $v, 'Process')
    }
  }
} else {
  Write-Warning "backend/.env not found; relying on system env vars"
}

Write-Host "Using MongoDB URI: $($env:MONGODB_URI)" -ForegroundColor Yellow
Write-Host "Starting backend on port $Port..." -ForegroundColor Cyan

Push-Location $backendDir
# Force TLS 1.2 for outbound TLS (workaround for networks that break TLS1.3)
$env:JAVA_TOOL_OPTIONS = ((($env:JAVA_TOOL_OPTIONS) + ' -Djdk.tls.client.protocols=TLSv1.2 -Dhttps.protocols=TLSv1.2').Trim())
# Set port via environment to avoid quoting/expansion issues
$env:SERVER_PORT = "$Port"

# Configure Firebase Admin credentials for token verification
$firebaseCreds = "C:\Users\HP\Desktop\Siyoth\springboot_emp_crud_v2\siyoth-sb-emp-crud-firebase-adminsdk-fbsvc-76d2aeef62.json"
if (Test-Path $firebaseCreds) {
  $env:GOOGLE_APPLICATION_CREDENTIALS = (Resolve-Path $firebaseCreds).Path
  Write-Host "Using GOOGLE_APPLICATION_CREDENTIALS=$($env:GOOGLE_APPLICATION_CREDENTIALS)"
} else {
  Write-Warning "Firebase credentials not found at $firebaseCreds"
}
# Prefer mvn.cmd and pass -D as quoted args before the goal
$mvnExe = Join-Path $mvnBin 'mvn.cmd'
if (!(Test-Path $mvnExe)) { $mvnExe = 'mvn' }
$args = @("-Dspring-boot.run.profiles=local", "spring-boot:run")
& $mvnExe @args
Pop-Location
