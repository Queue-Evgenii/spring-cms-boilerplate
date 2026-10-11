param(
    [Parameter(Position = 0)][ValidateSet('start', 'stop', 'status', 'test')][string]$Action = 'status',
    [switch]$Backend,
    [switch]$Frontend,
    [switch]$All,
    [string]$Filter
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$Root         =$PSScriptRoot
$BackendDir   = "$Root\backend"
$FrontendDir  = "$Root\frontend-admin"
$PgBin        = "$env:USERPROFILE\scoop\apps\postgresql\current\bin"
$PgData       = "$env:USERPROFILE\scoop\apps\postgresql\current\data"
$RedisBin     = "$env:USERPROFILE\scoop\apps\redis\current"
$RedisData    = "$env:USERPROFILE\scoop\persist\redis\data"

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

function Ensure-TestDatabase {
    $psql = "$PgBin\psql.exe"
    $exists = & $psql -U postgres -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = 'cms_boilerplate_test'"
    if ($exists) { return }
    Write-Host "Тестовая БД: создаю cms_boilerplate_test..." -ForegroundColor Yellow
    & $psql -U postgres -d postgres -c "CREATE DATABASE cms_boilerplate_test" | Out-Null
    Write-Host "Тестовая БД: создана" -ForegroundColor Green
}

function Start-Redis {
    if (Get-Process redis-server -ErrorAction SilentlyContinue) {
        Write-Host "Redis: уже работает" -ForegroundColor DarkGray
        return
    }
    Write-Host "Redis: запускаю..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Force $RedisData | Out-Null
    Start-Process "$RedisBin\redis-server.exe" -ArgumentList "--bind 127.0.0.1 --dir `"$RedisData`"" -WindowStyle Hidden

    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Milliseconds 500
        if (Get-NetTCPConnection -LocalPort 6379 -State Listen -ErrorAction SilentlyContinue) { break }
    }
    Write-Host "Redis: запущен (порт 6379)" -ForegroundColor Green
}

function Stop-Redis {
    if (Get-Process redis-server -ErrorAction SilentlyContinue) {
        Write-Host "Redis: останавливаю..." -ForegroundColor Yellow
        & "$RedisBin\redis-cli.exe" shutdown | Out-Null
        Write-Host "Redis: остановлен" -ForegroundColor Red
    }
}

switch ($Action) {
    'start' {
        $runAll = -not $Backend -and -not$Frontend

        Start-Postgres
        Start-Redis

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

    'test' {
        Start-Postgres
        Start-Redis
        Ensure-TestDatabase

        # cleanTest сбрасывает кэш Gradle, иначе при неизменном коде тесты пропускаются как UP-TO-DATE
        $gradleArgs = @('cleanTest', 'test')
        if ($Filter) { $gradleArgs += @('--tests', $Filter) }

        Write-Host "Backend: запускаю тесты..." -ForegroundColor Yellow
        Push-Location $BackendDir
        try {
            & .\gradlew @gradleArgs
            $exitCode = $LASTEXITCODE
        } finally {
            Pop-Location
        }

        if ($exitCode -eq 0) {
            Write-Host "Тесты: прошли" -ForegroundColor Green
        } else {
            Write-Host "Тесты: есть ошибки (отчёт: backend\build\reports\tests\test\index.html)" -ForegroundColor Red
        }
        exit $exitCode
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
            Stop-Redis
            Stop-Postgres
        }
    }

    'status' {
        Write-Host "--- Статус сервисов ---" -ForegroundColor Cyan
        
        $pgState = if (Get-Process postgres -ErrorAction SilentlyContinue) { "запущен (5432)" } else { "остановлен" }
        $pgColor = if ($pgState -match 'запущен') { 'Green' } else { 'DarkGray' }
        Write-Host "PostgreSQL: $pgState" -ForegroundColor $pgColor

        $rState = if (Get-Process redis-server -ErrorAction SilentlyContinue) { "запущен (6379)" } else { "остановлен" }
        $rColor = if ($rState -match 'запущен') { 'Green' } else { 'DarkGray' }
        Write-Host "Redis:      $rState" -ForegroundColor $rColor

        $bState = if (Get-BackendProcess) { "запущен -> http://localhost:8080/" } else { "остановлен" }
        $bColor = if ($bState -match 'запущен') { 'Green' } else { 'DarkGray' }
        Write-Host "Backend:    $bState" -ForegroundColor $bColor

        $fState = if (Get-FrontendProcess) { "запущен -> http://localhost:5173/" } else { "остановлен" }
        $fColor = if ($fState -match 'запущен') { 'Green' } else { 'DarkGray' }
        Write-Host "Frontend:   $fState" -ForegroundColor $fColor
    }
}