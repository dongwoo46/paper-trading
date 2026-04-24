$wav = @(
    "$env:SystemRoot\Media\Windows Notify System Generic.wav",
    "$env:SystemRoot\Media\Windows Notify.wav",
    "$env:SystemRoot\Media\chimes.wav",
    "$env:SystemRoot\Media\ding.wav"
) | Where-Object { Test-Path $_ } | Select-Object -First 1

if ($wav) {
    # 숨겨진 PowerShell 프로세스에서 SoundPlayer 실행 — 미디어 플레이어 창 없음
    $cmd = "(New-Object System.Media.SoundPlayer '$wav').PlaySync()"
    Start-Process powershell.exe -WindowStyle Hidden -ArgumentList "-NoProfile -Command $cmd" -Wait
}