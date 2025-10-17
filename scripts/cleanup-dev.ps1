param(
  [switch]$PurgeCaches = $false,
  [switch]$DockerPrune = $false
)

$ErrorActionPreference = 'Stop'

Write-Host "Cleaning project build artifacts..." -ForegroundColor Cyan

# Resolve paths
$repoRoot = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $repoRoot 'backend'
$frontend = Join-Path $repoRoot 'frontend'

function Remove-IfExists($path) {
  if (Test-Path $path) {
    Write-Host "Removing $path" -ForegroundColor DarkGray
    Remove-Item -Recurse -Force $path -ErrorAction SilentlyContinue
  }
}

# Backend artifacts
Remove-IfExists (Join-Path $backend 'target')
Remove-IfExists (Join-Path $backend 'logs\*')

# Frontend artifacts (keep node_modules; drop vite cache and build output)
Remove-IfExists (Join-Path $frontend 'node_modules\.vite')
Remove-IfExists (Join-Path $frontend 'dist')

if ($PurgeCaches) {
  Write-Host "Purging local caches (npm, Maven) ..." -ForegroundColor Yellow
  try { npm cache clean --force | Out-Null } catch {}
  $npmCache = Join-Path $env:LOCALAPPDATA 'npm-cache'
  Remove-IfExists $npmCache

  $m2 = Join-Path $env:USERPROFILE '.m2\repository'
  Remove-IfExists $m2
}

if ($DockerPrune) {
  Write-Host "Pruning Docker unused images/containers/volumes ..." -ForegroundColor Yellow
  try { docker system prune -af --volumes } catch { Write-Warning $_ }
}

Write-Host "Done."

