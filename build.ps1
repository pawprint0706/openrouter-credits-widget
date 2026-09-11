param(
    [string[]]$Tasks = @("testDebugUnitTest", "assembleDebug")
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$wrapperJar = Join-Path $projectRoot "gradle\wrapper\gradle-wrapper.jar"

$javaCandidates = @(
    "$env:ProgramFiles\Android\Android Studio\jbr\bin\java.exe",
    "$env:LOCALAPPDATA\Programs\Android Studio\jbr\bin\java.exe",
    $(if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\java.exe" }),
    $(if (Get-Command java -ErrorAction SilentlyContinue) { (Get-Command java).Source })
) | Where-Object { $_ -and (Test-Path -LiteralPath $_) } | Select-Object -Unique

$javaExecutable = $null
foreach ($candidate in $javaCandidates) {
    # java -version writes its normal output to stderr. Windows PowerShell 5.1
    # turns that stderr into an error record when ErrorActionPreference is Stop,
    # so capture both streams through Process instead.
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $candidate
    $startInfo.Arguments = "-version"
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    [void]$process.Start()
    $versionText = $process.StandardOutput.ReadToEnd() + $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    $process.Dispose()

    if ($versionText -match 'version "(?:1\.)?(\d+)') {
        $majorVersion = [int]$Matches[1]
        if ($majorVersion -ge 17) {
            $javaExecutable = $candidate
            break
        }
    }
}

if (-not $javaExecutable) {
    throw "JDK 17 이상을 찾을 수 없습니다. JAVA_HOME을 설정하거나 Android Studio JBR을 설치하십시오."
}

Write-Host "Java: $javaExecutable"
Write-Host "Gradle tasks: $($Tasks -join ', ')"

Push-Location $projectRoot
try {
    & $javaExecutable -classpath $wrapperJar org.gradle.wrapper.GradleWrapperMain @Tasks --console=plain
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
