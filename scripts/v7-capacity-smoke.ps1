param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [int]$Requests = 200,
    [int]$Concurrency = 16,
    [string]$BearerToken = '',
    [long]$ProjectId = 0,
    [switch]$ExerciseDuplicateRebuild
)

$ErrorActionPreference = 'Stop'
$target = [Uri]$BaseUrl
if ($target.Scheme -notin @('http', 'https') -or $target.Host -notin @('127.0.0.1', 'localhost', '::1')) {
    throw 'Capacity smoke is restricted to localhost. Never target projectmentorai.com or production.'
}
if ($Requests -lt 1 -or $Requests -gt 10000 -or $Concurrency -lt 1 -or $Concurrency -gt 128) {
    throw 'Requests must be 1..10000 and Concurrency must be 1..128.'
}
if ($ExerciseDuplicateRebuild -and ($ProjectId -le 0 -or [string]::IsNullOrWhiteSpace($BearerToken))) {
    throw 'Duplicate rebuild smoke requires a local fixture ProjectId and BearerToken.'
}

$handler = [System.Net.Http.SocketsHttpHandler]::new()
$handler.MaxConnectionsPerServer = $Concurrency
$client = [System.Net.Http.HttpClient]::new($handler)
$client.Timeout = [TimeSpan]::FromSeconds(15)
if (-not [string]::IsNullOrWhiteSpace($BearerToken)) {
    $client.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $BearerToken)
}

try {
    $endpoint = if ($ProjectId -gt 0) { "/api/projects/$ProjectId/vector-index" } else { '/api/health' }
    $latencies = [System.Collections.Concurrent.ConcurrentBag[double]]::new()
    $failures = [System.Collections.Concurrent.ConcurrentBag[string]]::new()
    $gate = [System.Threading.SemaphoreSlim]::new($Concurrency, $Concurrency)
    $tasks = for ($index = 0; $index -lt $Requests; $index++) {
        $null = $gate.Wait()
        [System.Threading.Tasks.Task]::Run([Action]{
            try {
                $watch = [System.Diagnostics.Stopwatch]::StartNew()
                $response = $client.GetAsync("$BaseUrl$endpoint").GetAwaiter().GetResult()
                $watch.Stop()
                $latencies.Add($watch.Elapsed.TotalMilliseconds)
                if (-not $response.IsSuccessStatusCode) { $failures.Add([string][int]$response.StatusCode) }
                $response.Dispose()
            } catch { $failures.Add($_.Exception.GetType().Name) }
            finally { $null = $gate.Release() }
        })
    }
    [System.Threading.Tasks.Task]::WaitAll($tasks)

    $duplicateTaskIds = @()
    if ($ExerciseDuplicateRebuild) {
        $rebuilds = 1..8 | ForEach-Object {
            $content = [System.Net.Http.StringContent]::new('', [Text.Encoding]::UTF8, 'application/json')
            $response = $client.PostAsync("$BaseUrl/api/projects/$ProjectId/vector-index/rebuild", $content).GetAwaiter().GetResult()
            $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            $response.Dispose()
            if ($body -match '"activeTaskId"\s*:\s*(\d+)') { [long]$Matches[1] }
        }
        $duplicateTaskIds = @($rebuilds | Where-Object { $_ } | Sort-Object -Unique)
    }

    $ordered = @($latencies | Sort-Object)
    $percentile = {
        param([double]$ratio)
        if ($ordered.Count -eq 0) { return $null }
        $position = [Math]::Max(0, [Math]::Ceiling($ordered.Count * $ratio) - 1)
        return [Math]::Round($ordered[$position], 2)
    }
    [ordered]@{
        environment = 'pmai-v7-perf'
        target = "$($target.Scheme)://$($target.Authority)$endpoint"
        requests = $Requests
        concurrency = $Concurrency
        successes = $Requests - $failures.Count
        failures = $failures.Count
        latency_ms = [ordered]@{ p50 = & $percentile 0.50; p95 = & $percentile 0.95; p99 = & $percentile 0.99 }
        duplicate_rebuild_unique_task_ids = $duplicateTaskIds.Count
        measured_at_utc = [DateTime]::UtcNow.ToString('o')
    } | ConvertTo-Json -Depth 4
} finally {
    $client.Dispose()
    $handler.Dispose()
}
