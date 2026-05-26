param(
    [int]$MaxAttempts = 30,
    [int]$RetryIntervalSeconds = 10,
    [string]$BackendHealthUrl = "http://127.0.0.1:18080/actuator/health",
    [string]$FrontendUrl = "http://127.0.0.1:18081/",
    [string]$GatewayLoginUrl = "http://127.0.0.1:18081/api/auth/login",
    [string]$GatewayCurrentUserUrl = "http://127.0.0.1:18081/api/auth/me",
    [string]$GatewayOAuthTokenUrl = "http://127.0.0.1:18081/oauth2/token",
    [string]$OpenApiPatientsUrl = "http://127.0.0.1:18081/open-api/v1/patients?patientCode=P2026001&gender=男&nameKeyword=张&startDate=1980-01-01&endDate=1990-12-31",
    [string]$OpenApiMedicationsUrl = "http://127.0.0.1:18081/open-api/v1/medications?patientCode=P2026001&drugName=阿司匹林&startDate=2026-05-01&endDate=2026-05-31",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "Admin@123",
    [string]$DemoClientId = "demo-client",
    [string]$DemoClientSecret = "demo-secret"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path (Join-Path $root "docker") "docker-compose.yml"
$artifactsDir = Join-Path (Join-Path $root ".artifacts") "compose-smoke"

function Assert-Command {
    param([string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "未找到命令: $Name"
    }
}

function Invoke-Compose {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Args)

    & docker compose -f $composeFile @Args
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose $($Args -join ' ') 执行失败"
    }
}

function Write-Diagnostics {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        return
    }

    New-Item -ItemType Directory -Force -Path $artifactsDir | Out-Null

    try {
        $psOutput = & docker compose -f $composeFile ps --all 2>&1
        $psOutput | Out-File -FilePath (Join-Path $artifactsDir "compose-ps.log") -Encoding utf8

        $logOutput = & docker compose -f $composeFile logs --no-color 2>&1
        $logOutput | Out-File -FilePath (Join-Path $artifactsDir "compose-logs.log") -Encoding utf8
    }
    catch {
        Write-Warning "写入 compose 诊断信息失败: $($_.Exception.Message)"
    }
}

function Wait-Until {
    param(
        [string]$Name,
        [scriptblock]$Probe
    )

    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        try {
            if (& $Probe) {
                Write-Host "==> $Name 已通过"
                return
            }
        }
        catch {
            Write-Host "[$attempt/$MaxAttempts] $Name 未就绪: $($_.Exception.Message)"
        }

        if ($attempt -lt $MaxAttempts) {
            Start-Sleep -Seconds $RetryIntervalSeconds
        }
    }

    throw "$Name 在 ${MaxAttempts} 次重试后仍未通过"
}

function Test-BackendHealth {
    $response = Invoke-RestMethod -Uri $BackendHealthUrl -Method Get -TimeoutSec 10
    return $response.status -eq "UP"
}

function Test-FrontendHome {
    $response = Invoke-WebRequest -Uri $FrontendUrl -Method Get -TimeoutSec 10
    return $response.StatusCode -ge 200 -and $response.StatusCode -lt 400
}

function Test-GatewayLogin {
    $body = @{
        username = $AdminUsername
        password = $AdminPassword
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri $GatewayLoginUrl -Method Post -ContentType "application/json" -Body $body -TimeoutSec 10
    return -not [string]::IsNullOrWhiteSpace($response.data.accessToken)
}

function Assert-ApiSuccess {
    param(
        [object]$Response,
        [string]$Name
    )

    if ($null -eq $Response) {
        throw "$Name 返回为空"
    }

    if ($null -eq $Response.code -or $Response.code -ne 0) {
        throw "$Name 未返回成功响应"
    }
}

function Get-AdminAccessToken {
    $body = @{
        username = $AdminUsername
        password = $AdminPassword
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri $GatewayLoginUrl -Method Post -ContentType "application/json" -Body $body -TimeoutSec 10
    Assert-ApiSuccess -Response $response -Name "网关登录"
    if ([string]::IsNullOrWhiteSpace($response.data.accessToken)) {
        throw "网关登录未返回 accessToken"
    }
    return $response.data.accessToken
}

function Test-AuthMe {
    param([string]$AccessToken)

    $headers = @{
        Authorization = "Bearer $AccessToken"
    }
    $response = Invoke-RestMethod -Uri $GatewayCurrentUserUrl -Method Get -Headers $headers -TimeoutSec 10
    Assert-ApiSuccess -Response $response -Name "当前用户接口"
    return $response.data.username -eq $AdminUsername
}

function Get-OpenApiAccessToken {
    $body = @{
        clientId = $DemoClientId
        clientSecret = $DemoClientSecret
        scope = "patients.read medications.read"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri $GatewayOAuthTokenUrl -Method Post -ContentType "application/json" -Body $body -TimeoutSec 10
    Assert-ApiSuccess -Response $response -Name "OAuth2 令牌接口"
    if ([string]::IsNullOrWhiteSpace($response.data.accessToken)) {
        throw "OAuth2 令牌接口未返回 accessToken"
    }
    return $response.data.accessToken
}

function Test-OpenApiRows {
    param(
        [string]$Name,
        [string]$Url,
        [string]$AccessToken
    )

    $headers = @{
        Authorization = "Bearer $AccessToken"
    }
    $response = Invoke-RestMethod -Uri $Url -Method Get -Headers $headers -TimeoutSec 10
    Assert-ApiSuccess -Response $response -Name $Name
    if ($null -eq $response.data -or $response.data.Count -lt 1) {
        throw "$Name 未返回有效数据行"
    }
    return $true
}

Push-Location $root

try {
    Assert-Command docker
    New-Item -ItemType Directory -Force -Path $artifactsDir | Out-Null

    Invoke-Compose down -v --remove-orphans
    Invoke-Compose up -d --build

    Wait-Until "后端健康检查" { Test-BackendHealth }
    Wait-Until "前端首页访问" { Test-FrontendHome }
    Wait-Until "网关登录链路" { Test-GatewayLogin }
    $adminAccessToken = Get-AdminAccessToken
    Wait-Until "管理端当前用户接口" { Test-AuthMe -AccessToken $adminAccessToken }
    $openApiAccessToken = Get-OpenApiAccessToken
    Wait-Until "开放 API 患者主链路" {
        Test-OpenApiRows -Name "开放 API 患者接口" -Url $OpenApiPatientsUrl -AccessToken $openApiAccessToken
    }
    Wait-Until "开放 API 用药主链路" {
        Test-OpenApiRows -Name "开放 API 用药接口" -Url $OpenApiMedicationsUrl -AccessToken $openApiAccessToken
    }

    Write-Diagnostics
}
catch {
    Write-Diagnostics
    throw
}
finally {
    try {
        Invoke-Compose down -v --remove-orphans
    }
    catch {
        Write-Warning "清理容器失败: $($_.Exception.Message)"
    }

    Pop-Location
}
