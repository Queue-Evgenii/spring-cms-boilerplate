param(
    [Parameter(Position = 0)][ValidateSet('start', 'stop', 'status')][string]$Action = 'status',
    [switch]$Backend,
    [switch]$Frontend,
    [switch]$All
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$Root         =$PSScriptRoot
$BackendDir   = "$Root\backend"
$FrontendDir  = "$Root\frontend-admin"
$PgBin        = "$env:USERPROFILE\scoop\apps\postgresql\current\bin"
$PgData       = "$env:USERPROFILE\scoop\apps\postgresql\current\data"

function Get-BackendProcess {
    Get-CimInstance Win32_Process | Where-Object { 
        $_.CommandLine -match [regex]::Escape($BackendDir) -and$_.Name -match 'java.exe'
    }
}

function Get-FrontendProcess {
    Get-CimInstance Win32_Process | Where-Object { 
        $_.CommandLine -match [regex]::Escape($FrontendDir) -and$_.Name -match 'node.exe'
    }
}

function Start-Postgres {
    if (Get-Process postgres -ErrorAction SilentlyContinue) { 
        Write-Host "PostgreSQL: уже работает" -ForegroundColor DarkGray
        return 
    }
    Write-Host "PostgreSQL: запускаю..." -ForegroundColor Yellow
    Start-Process "$PgBin\pg_ctl.exe" -ArgumentList "-D `"$PgData`" start" -WindowStyle Hidden
    
    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Milliseconds 500
        if (Get-NetTCPConnection -LocalPort 5432 -State Listen -ErrorAction SilentlyContinue) { break }
    }
    Write-Host "PostgreSQL: запущен (порт 5432)" -ForegroundColor Green
}

function Stop-Postgres {
    if (Get-Process postgres -ErrorAction SilentlyContinue) {
        Write-Host "PostgreSQL: останавливаю..." -ForegroundColor Yellow
        Start-Process "$PgBin\pg_ctl.exe" -ArgumentList "-D `"$PgData`" stop" -WindowStyle Hidden
        Write-Host "PostgreSQL: остановлен" -ForegroundColor Red
    }
}

switch ($Action) {
    'start' {
        $runAll = -not $Backend -and -not$Frontend
        
        Start-Postgres

        if ($runAll -or$Backend) {
            if (Get-BackendProcess) {
                Write-Host "Backend: уже запущен" -ForegroundColor DarkGray
            } else {
                Write-Host "Backend: запускаю Gradle bootRun..." -ForegroundColor Yellow
                Start-Process powershell -ArgumentList '-NoExit', '-Command', "Set-Location '$BackendDir'; .\gradlew bootRun" -WorkingDirectory $BackendDir
                Write-Host "Backend стартует на http://localhost:8080/" -ForegroundColor Green
            }
        }

        if ($runAll -or$Frontend) {
            if (Test-Path "$FrontendDir\package.json") {
                if (Get-FrontendProcess) {
                    Write-Host "Frontend: уже запущен" -ForegroundColor DarkGray
                } else {
                    Write-Host "Frontend: запускаю npm run dev..." -ForegroundColor Yellow
                    Start-Process powershell -ArgumentList '-NoExit', '-Command', "Set-Location '$FrontendDir'; npm run dev" -WorkingDirectory $FrontendDir
                    Write-Host "Frontend стартует на http://localhost:5173/" -ForegroundColor Green
                }
            } else {
                Write-Host "Frontend: папка $FrontendDir еще не создана." -ForegroundColor DarkYellow
            }
        }
    }

    'stop' {
        $bProc = Get-BackendProcess
        if ($bProc) {
            Stop-Process -Id $bProc.ProcessId -Force -ErrorAction SilentlyContinue
            Write-Host "Backend: остановлен" -ForegroundColor Red
        } else {
            Write-Host "Backend: не был запущен" -ForegroundColor DarkGray
        }

        $fProc = Get-FrontendProcess
        if ($fProc) {
            Stop-Process -Id $fProc.ProcessId -Force -ErrorAction SilentlyContinue
            Write-Host "Frontend: остановлен" -ForegroundColor Red
        } else {
            Write-Host "Frontend: не был запущен" -ForegroundColor DarkGray
        }

        if ($All) {
            Stop-Postgres
        }
    }

    'status' {
        Write-Host "--- Статус сервисов ---" -ForegroundColor Cyan
        
        $pgState = if (Get-Process postgres -ErrorAction SilentlyContinue) { "запущен (5432)" } else { "остановлен" }
        $pgColor = if ($pgState -match 'запущен') { 'Green' } else { 'DarkGray' }
        Write-Host "PostgreSQL: $pgState" -ForegroundColor $pgColor

        $bState = if (Get-BackendProcess) { "запущен -> http://localhost:8080/" } else { "остановлен" }
        $bColor = if ($bState -match 'запущен') { 'Green' } else { 'DarkGray' }
        Write-Host "Backend:    $bState" -ForegroundColor $bColor

        $fState = if (Get-FrontendProcess) { "запущен -> http://localhost:5173/" } else { "остановлен" }
        $fColor = if ($fState -match 'запущен') { 'Green' } else { 'DarkGray' }
        Write-Host "Frontend:   $fState" -ForegroundColor $fColor
    }
}