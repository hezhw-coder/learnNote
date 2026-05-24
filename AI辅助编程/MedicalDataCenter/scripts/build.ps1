param(
    [switch]$SkipDockerBuild,
    [switch]$SkipFrontend,
    [switch]$SkipBackend
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

function Invoke-Step {
    param(
        [string]$Message,
        [scriptblock]$Action
    )

    Write-Host "==> $Message"
    & $Action
}

function Assert-Command {
    param([string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "未找到命令: $Name"
    }
}

Push-Location $root

try {
    if (-not $SkipFrontend -and (Test-Path "$root\\frontend\\package.json")) {
        Assert-Command npm
        Invoke-Step "构建前端" {
            Push-Location "$root\\frontend"
            if (Test-Path ".\\package-lock.json") {
                npm ci
            }
            else {
                npm install
            }
            npm run build
            Pop-Location
        }
    }
    elseif (-not $SkipFrontend) {
        Write-Warning "未找到 frontend/package.json，跳过前端构建。"
    }

    if (-not $SkipBackend -and (Test-Path "$root\\backend\\pom.xml")) {
        Assert-Command mvn
        Invoke-Step "构建后端" {
            Push-Location "$root\\backend"
            mvn clean package -DskipTests
            Pop-Location
        }
    }
    elseif (-not $SkipBackend) {
        Write-Warning "未找到 backend/pom.xml，跳过后端构建。"
    }

    if (-not $SkipDockerBuild) {
        Assert-Command docker
        Invoke-Step "构建 Docker 镜像" {
            docker compose -f "$root\\docker\\docker-compose.yml" build
        }
    }
}
finally {
    Pop-Location
}
