$pythonDir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\DISRUPTIVE-ARCHITECTURES-IOT-IOB-GENERATIVE-IA\py'))
$pythonExe = Join-Path $pythonDir '.venv\Scripts\python.exe'
if (-not (Test-Path -LiteralPath $pythonExe)) {
    Write-Error 'Ambiente Python nao encontrado em py\.venv\Scripts\python.exe.'
    exit 2
}
& $pythonExe (Join-Path $pythonDir 'scripts\iniciar_java.py') @args
exit $LASTEXITCODE
