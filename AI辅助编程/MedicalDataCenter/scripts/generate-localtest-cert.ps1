param(
    [string]$PrimaryDnsName = "mdc.localtest.me",
    [string[]]$AdditionalDnsNames = @("localhost"),
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "..\docker\certs"),
    [int]$ValidDays = 825,
    [switch]$Trust
)

$ErrorActionPreference = "Stop"

function Convert-ToPem {
    param(
        [byte[]]$Bytes,
        [string]$Label
    )

    $base64 = [System.Convert]::ToBase64String($Bytes, [System.Base64FormattingOptions]::InsertLineBreaks)
    return "-----BEGIN $Label-----`n$base64`n-----END $Label-----`n"
}

$dnsNames = @($PrimaryDnsName) + $AdditionalDnsNames | Where-Object { $_ -and $_.Trim() } | Select-Object -Unique
if (-not $dnsNames -or $dnsNames.Count -eq 0) {
    throw "至少需要一个 DNS 名称。"
}

$resolvedOutputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Force -Path $resolvedOutputDirectory | Out-Null

$cert = New-SelfSignedCertificate `
    -Subject "CN=$PrimaryDnsName" `
    -DnsName $dnsNames `
    -FriendlyName "MedicalDataCenter Local TLS" `
    -CertStoreLocation "Cert:\CurrentUser\My" `
    -HashAlgorithm "SHA256" `
    -KeyAlgorithm "RSA" `
    -KeyLength 2048 `
    -KeyExportPolicy Exportable `
    -NotAfter (Get-Date).AddDays($ValidDays) `
    -TextExtension @(
        "2.5.29.19={text}CA=false",
        "2.5.29.37={text}1.3.6.1.5.5.7.3.1"
    )

$rsa = [System.Security.Cryptography.X509Certificates.RSACertificateExtensions]::GetRSAPrivateKey($cert)
if ($null -eq $rsa) {
    throw "未能从证书中读取 RSA 私钥。"
}

$certPem = Convert-ToPem -Bytes $cert.RawData -Label "CERTIFICATE"
if ($rsa.GetType().GetMethod("ExportPkcs8PrivateKey", [System.Type[]]@())) {
    $privateKeyBytes = $rsa.ExportPkcs8PrivateKey()
    $privateKeyLabel = "PRIVATE KEY"
} elseif ($rsa.GetType().GetMethod("ExportRSAPrivateKey", [System.Type[]]@())) {
    $privateKeyBytes = $rsa.ExportRSAPrivateKey()
    $privateKeyLabel = "RSA PRIVATE KEY"
} else {
    throw "当前 PowerShell/.NET 环境不支持导出 RSA 私钥。"
}
$keyPem = Convert-ToPem -Bytes $privateKeyBytes -Label $privateKeyLabel

$certPath = Join-Path $resolvedOutputDirectory "tls.crt"
$keyPath = Join-Path $resolvedOutputDirectory "tls.key"

Set-Content -Path $certPath -Value $certPem -Encoding ascii -NoNewline
Set-Content -Path $keyPath -Value $keyPem -Encoding ascii -NoNewline

if ($Trust.IsPresent) {
    $rootStore = [System.Security.Cryptography.X509Certificates.X509Store]::new("Root", "CurrentUser")
    $rootStore.Open([System.Security.Cryptography.X509Certificates.OpenFlags]::ReadWrite)
    try {
        $rootStore.Add($cert)
    } finally {
        $rootStore.Close()
    }
}

Write-Host "自签名证书已生成：" -ForegroundColor Green
Write-Host "  证书: $certPath"
Write-Host "  私钥: $keyPath"
Write-Host "  DNS : $($dnsNames -join ', ')"
Write-Host ""
Write-Host "如需启动生产版 Compose，可执行：" -ForegroundColor Cyan
Write-Host "  docker compose -f .\docker\docker-compose.prod.yml --env-file .\docker\.env.prod up -d --build"
