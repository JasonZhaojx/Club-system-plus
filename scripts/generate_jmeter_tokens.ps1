param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [int]$Count = 200,
    [string]$Prefix = "load_user",
    [string]$Password = "Load@123456",
    [string]$Output = "docs/jmeter_tokens.csv",
    [int]$StartIndex = 1
)

$ErrorActionPreference = "Stop"

function Invoke-JsonPost {
    param(
        [string]$Uri,
        [object]$Body
    )

    $json = $Body | ConvertTo-Json -Depth 5
    Invoke-RestMethod -Uri $Uri -Method Post -ContentType "application/json; charset=utf-8" -Body $json
}

function Escape-Csv {
    param([string]$Value)

    if ($null -eq $Value) {
        return ""
    }
    $escaped = $Value.Replace('"', '""')
    if ($escaped.Contains(",") -or $escaped.Contains('"') -or $escaped.Contains("`n") -or $escaped.Contains("`r")) {
        return '"' + $escaped + '"'
    }
    return $escaped
}

$rows = New-Object System.Collections.Generic.List[string]
$rows.Add("username,password,token")

$success = 0
$failed = 0
$endIndex = $StartIndex + $Count - 1

for ($i = $StartIndex; $i -le $endIndex; $i++) {
    $username = "{0}_{1:D4}" -f $Prefix, $i
    $nickname = "Load User {0:D4}" -f $i
    $email = "$username@example.com"

    $registerBody = @{
        username = $username
        password = $Password
        nickname = $nickname
        email = $email
    }

    try {
        Invoke-JsonPost -Uri "$BaseUrl/auth/register" -Body $registerBody | Out-Null
    } catch {
        # Existing users are acceptable; login below is the source of truth.
    }

    $loginBody = @{
        username = $username
        password = $Password
    }

    try {
        $loginResult = Invoke-JsonPost -Uri "$BaseUrl/auth/login" -Body $loginBody
        $token = $loginResult.data.accessToken
        if ([string]::IsNullOrWhiteSpace($token)) {
            throw "Login response did not contain data.accessToken"
        }

        $rows.Add(("{0},{1},{2}" -f (Escape-Csv $username), (Escape-Csv $Password), (Escape-Csv $token)))
        $success++
        Write-Host ("OK {0}" -f $username)
    } catch {
        $failed++
        Write-Warning ("FAILED {0}: {1}" -f $username, $_.Exception.Message)
    }
}

$outputPath = if ([System.IO.Path]::IsPathRooted($Output)) {
    $Output
} else {
    Join-Path (Get-Location) $Output
}

$outputDir = Split-Path $outputPath
if ($outputDir -and -not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

$rows | Set-Content -Path $outputPath -Encoding UTF8

Write-Host ("Written {0}" -f $outputPath)
Write-Host ("Success: {0}, Failed: {1}" -f $success, $failed)
