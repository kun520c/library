param([string]$Path = '.env')

$ErrorActionPreference = 'Stop'
# Deliberately supports the plain KEY=VALUE format of .env.example (no shell evaluation).
foreach ($line in Get-Content -LiteralPath $Path) {
    $trimmed = $line.Trim()
    if (!$trimmed -or $trimmed.StartsWith('#')) { continue }
    $parts = $trimmed -split '=', 2
    if ($parts.Count -ne 2 -or $parts[0] -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
        throw 'Expected plain KEY=VALUE entries in the local environment file.'
    }
    [Environment]::SetEnvironmentVariable($parts[0], $parts[1], 'Process')
}
