param(
  [string]$ApiBase = "http://localhost:8080"
)

$ErrorActionPreference = 'Stop'

# Resolve repo root relative to this script and locate frontend directory
$repoRoot = Split-Path -Parent $PSScriptRoot
$frontendDir = Join-Path $repoRoot 'frontend'
if (!(Test-Path $frontendDir)) { throw "Frontend directory not found: $frontendDir" }

$envPath = Join-Path $frontendDir '.env'

Write-Host "Ensuring frontend .env points to: $ApiBase (preserving other vars)" -ForegroundColor Cyan
if (Test-Path $envPath) {
  $raw = Get-Content -Raw $envPath
  if ($raw -match "(?m)^\s*VITE_API_BASE_URL=.*$") {
    $updated = [System.Text.RegularExpressions.Regex]::Replace($raw, "(?m)^\s*VITE_API_BASE_URL=.*$", "VITE_API_BASE_URL=$ApiBase")
  } else {
    $nl = if ($raw.EndsWith("`n")) { '' } else { "`r`n" }
    $updated = $raw + $nl + "VITE_API_BASE_URL=$ApiBase`r`n"
  }
  Set-Content -Path $envPath -Value $updated -Encoding UTF8
} else {
  "VITE_API_BASE_URL=$ApiBase" | Out-File -FilePath $envPath -Encoding UTF8 -Force
}

Push-Location $frontendDir
if (Test-Path (Join-Path $frontendDir 'node_modules')) {
  Write-Host "node_modules present; skipping npm ci to save time/space" -ForegroundColor Yellow
} else {
  Write-Host "Installing frontend dependencies (npm ci)..."
  & npm.cmd ci --no-fund --no-audit
}
Write-Host "Starting Vite dev server on http://localhost:5173 ..."
& npm.cmd run dev
Pop-Location
