$ErrorActionPreference = 'Stop'
$javaBin = Join-Path $env:JAVA_HOME 'bin'
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root
$cp = (Get-Content '.tools/classpath.txt' -Raw).Trim() + ';' + (Join-Path $root 'target/EndUpdate-1.0.3.jar')
New-Item -ItemType Directory -Force '.tools/probe' | Out-Null
& "$javaBin/javac.exe" --release 25 -cp $cp -d '.tools/probe' 'tools/integration/VerificationPlugin.java'
if ($LASTEXITCODE -ne 0) { throw 'Probe compilation failed' }
Copy-Item 'tools/integration/plugin.yml' '.tools/probe/plugin.yml'
New-Item -ItemType Directory -Force '.tools/tmp' | Out-Null
& "$javaBin/jar.exe" "-J-Djava.io.tmpdir=$root/.tools/tmp" --create --file '.test-server/plugins/EndUpdateVerification.jar' -C '.tools/probe' .
if ($LASTEXITCODE -ne 0) { throw 'Probe packaging failed' }
