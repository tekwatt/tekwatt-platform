[CmdletBinding()]
param(
    [string]$ApiGatewayUrl = 'https://api-gateway.lemonmushroom-1166ae48.eastasia.azurecontainerapps.io',
    [ValidateRange(1, 30)][int]$Attempts = 12,
    [ValidateRange(0, 30)][int]$RetrySeconds = 5
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.AllowAutoRedirect = $false
$client = [System.Net.Http.HttpClient]::new($handler)
$client.Timeout = [TimeSpan]::FromSeconds(20)
$lastResult = 'No response'
try {
    for ($attempt = 1; $attempt -le $Attempts; $attempt++) {
        $request = [System.Net.Http.HttpRequestMessage]::new(
            [System.Net.Http.HttpMethod]::Get, ($ApiGatewayUrl.TrimEnd('/') + '/ocpp/TEKWATT-AUTH-PROBE'))
        $response = $null
        try {
            foreach ($entry in @{
                'Connection' = 'Upgrade'
                'Upgrade' = 'websocket'
                'Origin' = 'https://evcharger-simulator.com'
                'Sec-WebSocket-Version' = '13'
                'Sec-WebSocket-Key' = 'dGhlIHNhbXBsZSBub25jZQ=='
                'Sec-WebSocket-Protocol' = 'ocpp2.0'
            }.GetEnumerator()) {
                [void]$request.Headers.TryAddWithoutValidation($entry.Key, $entry.Value)
            }
            $response = $client.SendAsync($request,
                [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead).GetAwaiter().GetResult()
            $challenge = (@($response.Headers.WwwAuthenticate | ForEach-Object { $_.ToString() }) -join ', ')
            $lastResult = 'HTTP {0}; WWW-Authenticate: {1}' -f [int]$response.StatusCode, $challenge
            Write-Host ('OCPP check {0}/{1}: {2}' -f $attempt, $Attempts, $lastResult)
            if ([int]$response.StatusCode -eq 401 -and $challenge -match 'Basic\s+realm="TekWatt OCPP"') {
                Write-Host 'PASS: public OCPP authentication challenge is live. No password was sent.' -ForegroundColor Green
                Write-Host 'This checks the password challenge, not an authenticated charging session.'
                return
            }
        }
        catch {
            $lastResult = $_.Exception.Message
            Write-Host ('OCPP check {0}/{1}: {2}' -f $attempt, $Attempts, $lastResult)
        }
        finally {
            if ($null -ne $response) { $response.Dispose() }
            $request.Dispose()
        }
        if ($attempt -lt $Attempts) { Start-Sleep -Seconds $RetrySeconds }
    }
    throw ('OCPP challenge not confirmed after {0} attempts. Last result: {1}. Check the active API gateway image, traffic allocation, and OCPP_REQUIRE_CREDENTIALS=true.' -f $Attempts, $lastResult)
}
finally {
    $client.Dispose()
}
