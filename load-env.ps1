Get-Content .env | ForEach-Object {
    $key, $value = $_.Split('=')
    [System.Environment]::SetEnvironmentVariable($key, $value, [System.EnvironmentVariableTarget]::Process)
}