$ErrorActionPreference = 'Stop'

$searchRoots = @(
    'C:\Users\lenovo\Downloads',
    'C:\Program Files',
    'C:\Program Files\Apache Maven',
    'C:\Program Files\NetBeans',
    'C:\Program Files\Common Files\Oracle\Java',
    'D:\'
)

$mvn = $null
foreach ($root in $searchRoots) {
    if (Test-Path $root) {
        $candidate = Get-ChildItem -Path $root -Filter 'mvn.cmd' -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($candidate) {
            $mvn = $candidate.FullName
            break
        }
    }
}

if (-not $mvn) {
    Write-Error 'Maven was not found on this machine. Install Maven or add it to PATH before running this project.'
    exit 1
}

$env:PORT = if ($env:PORT) { $env:PORT } else { '8081' }

Write-Host "Using Maven: $mvn"
Write-Host "Starting SOC Sentinel on http://localhost:$env:PORT ..."

& $mvn spring-boot:run
