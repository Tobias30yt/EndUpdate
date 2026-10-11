param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (!$JavaHome) {
    $candidates = Get-ChildItem -LiteralPath "$env:ProgramFiles\Java" -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '^jdk-(\d+)' -and [int]$Matches[1] -ge 25 } |
        Sort-Object { [int]($_.Name -replace '^jdk-(\d+).*','$1') } -Descending
    $JavaHome = $candidates | Select-Object -First 1 -ExpandProperty FullName
}
if (!$JavaHome -or !(Test-Path -LiteralPath "$JavaHome\bin\javac.exe")) { throw 'JDK 25+ erforderlich: JAVA_HOME setzen oder -JavaHome angeben.' }
$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome\bin;$env:PATH"
mvn -B -ntp '-Dmaven.repo.local=.tools/m2' package
if ($LASTEXITCODE -ne 0) { throw 'Build fehlgeschlagen.' }
Copy-Item -LiteralPath 'target/EndUpdate-1.0.1.jar' -Destination 'EndUpdate-1.0.1.jar'
Write-Output 'Fertig: EndUpdate-1.0.1.jar'
