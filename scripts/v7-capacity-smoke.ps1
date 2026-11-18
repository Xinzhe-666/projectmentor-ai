param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [int]$Requests = 24,
    [int]$Concurrency = 8,
    [string]$BearerToken = '',
    [long[]]$ProjectIds = @(),
    [switch]$ExerciseDuplicateRebuild,
    [switch]$ExerciseQa,
    [string]$QaQuestion = 'Where is authentication implemented?',
    [string[]]$ExpectedEvidencePrefixes = @(),
    [int]$PollTimeoutSeconds = 180,
    [string]$MetricsUrl = '',
    [string]$RabbitManagementUrl = '',
    [string]$RabbitVhost = '/',
    [string]$RabbitQueue = 'pmai.index.execute.v1',
    [string]$RabbitDlq = 'pmai.index.dlq.v1',
    [string]$RabbitUsername = '',
    [string]$RabbitPassword = ''
)

$ErrorActionPreference = 'Stop'
$target = [Uri]$BaseUrl
if ($target.Scheme -notin @('http', 'https') -or $target.Host -notin @('127.0.0.1', 'localhost', '::1')) {
    throw 'Capacity smoke is restricted to localhost. Never target projectmentorai.com or production.'
}
if ($Requests -lt 1 -or $Requests -gt 10000 -or $Concurrency -lt 1 -or $Concurrency -gt 128) {
    throw 'Requests must be 1..10000 and Concurrency must be 1..128.'
}
if ($ProjectIds.Count -eq 0 -or [string]::IsNullOrWhiteSpace($BearerToken)) {
    throw 'A local fixture BearerToken and at least one ProjectIds value are required; /api/health is not a capacity path.'
}
if (-not $ExerciseDuplicateRebuild -and -not $ExerciseQa) {
    throw 'Select at least one real business scenario: ExerciseDuplicateRebuild or ExerciseQa.'
}

$prefixByProject = @{}
foreach ($entry in $ExpectedEvidencePrefixes) {
    if ($entry -notmatch '^(\d+)=(.+)$') { throw "ExpectedEvidencePrefixes entry must be projectId=prefix: $entry" }
    $prefixByProject[[long]$Matches[1]] = $Matches[2]
}

$work = [System.Collections.Generic.List[object]]::new()
if ($ExerciseDuplicateRebuild) {
    for ($i = 0; $i -lt $Requests; $i++) {
        $projectId = $ProjectIds[$i % $ProjectIds.Count]
        $work.Add([pscustomobject]@{ scenario = 'rebuild'; projectId = $projectId; method = 'POST'
                url = "$BaseUrl/api/projects/$projectId/vector-index/rebuild"; body = '{}' })
    }
}
if ($ExerciseQa) {
    $qaBody = @{ question = $QaQuestion } | ConvertTo-Json -Compress
    for ($i = 0; $i -lt $Requests; $i++) {
        $projectId = $ProjectIds[$i % $ProjectIds.Count]
        $work.Add([pscustomobject]@{ scenario = 'qa'; projectId = $projectId; method = 'POST'
                url = "$BaseUrl/api/projects/$projectId/qa"; body = $qaBody })
    }
}

$results = @($work | ForEach-Object -Parallel {
    $item = $_
    $headers = @{ Authorization = "Bearer $($using:BearerToken)" }
    $watch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $response = Invoke-WebRequest -Uri $item.url -Method $item.method -Headers $headers `
            -ContentType 'application/json' -Body $item.body -SkipHttpErrorCheck -TimeoutSec 60
        $watch.Stop()
        [pscustomobject]@{ scenario = $item.scenario; projectId = $item.projectId
            success = [int]$response.StatusCode -ge 200 -and [int]$response.StatusCode -lt 300
            statusCode = [int]$response.StatusCode; latencyMs = $watch.Elapsed.TotalMilliseconds
            body = [string]$response.Content; error = $null }
    } catch {
        $watch.Stop()
        [pscustomobject]@{ scenario = $item.scenario; projectId = $item.projectId; success = $false
            statusCode = 0; latencyMs = $watch.Elapsed.TotalMilliseconds; body = ''
            error = $_.Exception.GetType().Name }
    }
} -ThrottleLimit $Concurrency)

function Get-DataPayload([string]$Body) {
    if ([string]::IsNullOrWhiteSpace($Body)) { return $null }
    $payload = $Body | ConvertFrom-Json
    if ($null -ne $payload.data) { return $payload.data }
    return $payload
}

function Get-Percentile([double[]]$Values, [double]$Ratio) {
    if ($Values.Count -eq 0) { return $null }
    $ordered = @($Values | Sort-Object)
    $position = [Math]::Max(0, [Math]::Ceiling($ordered.Count * $Ratio) - 1)
    return [Math]::Round($ordered[$position], 2)
}

$duplicateViolations = [System.Collections.Generic.List[object]]::new()
foreach ($projectId in $ProjectIds) {
    $taskIds = @($results | Where-Object { $_.scenario -eq 'rebuild' -and $_.projectId -eq $projectId -and $_.success } |
        ForEach-Object { (Get-DataPayload $_.body).activeTaskId } | Where-Object { $null -ne $_ } | Sort-Object -Unique)
    if ($taskIds.Count -gt 1) {
        $duplicateViolations.Add([pscustomobject]@{ projectId = $projectId; observedActiveTaskIds = $taskIds })
    }
}

$crossProjectViolations = [System.Collections.Generic.List[object]]::new()
foreach ($result in $results | Where-Object { $_.scenario -eq 'qa' -and $_.success }) {
    if (-not $prefixByProject.ContainsKey([long]$result.projectId)) { continue }
    $expected = [string]$prefixByProject[[long]$result.projectId]
    $data = Get-DataPayload $result.body
    foreach ($evidence in @($data.evidences)) {
        if ($evidence.filePath -and -not ([string]$evidence.filePath).StartsWith($expected, [StringComparison]::Ordinal)) {
            $crossProjectViolations.Add([pscustomobject]@{ projectId = $result.projectId
                    expectedPrefix = $expected; actualPath = $evidence.filePath })
        }
    }
}

$statuses = @{}
$indexDurations = @{}
if ($ExerciseDuplicateRebuild) {
    $pollStarted = [DateTime]::UtcNow
    $remaining = [System.Collections.Generic.HashSet[long]]::new()
    foreach ($projectId in $ProjectIds) { $null = $remaining.Add($projectId) }
    while ($remaining.Count -gt 0 -and ([DateTime]::UtcNow - $pollStarted).TotalSeconds -lt $PollTimeoutSeconds) {
        foreach ($projectId in @($remaining)) {
            $response = Invoke-WebRequest -Uri "$BaseUrl/api/projects/$projectId/vector-index" `
                -Headers @{ Authorization = "Bearer $BearerToken" } -SkipHttpErrorCheck -TimeoutSec 30
            if ([int]$response.StatusCode -ge 200 -and [int]$response.StatusCode -lt 300) {
                $status = Get-DataPayload ([string]$response.Content)
                $statuses[[string]$projectId] = $status
                if ($status.status -notin @('PENDING', 'RUNNING')) {
                    $indexDurations[[string]$projectId] = [Math]::Round(([DateTime]::UtcNow - $pollStarted).TotalMilliseconds, 2)
                    $null = $remaining.Remove($projectId)
                }
            }
        }
        if ($remaining.Count -gt 0) { Start-Sleep -Milliseconds 500 }
    }
}

$pointerViolations = [System.Collections.Generic.List[object]]::new()
foreach ($projectId in $statuses.Keys) {
    $status = $statuses[$projectId]
    $activeMismatch = ($null -eq $status.activeGeneration) -xor [string]::IsNullOrWhiteSpace([string]$status.activeBuildId)
    $pendingMismatch = ($null -eq $status.pendingGeneration) -xor [string]::IsNullOrWhiteSpace([string]$status.pendingBuildId)
    if ($activeMismatch -or $pendingMismatch) {
        $pointerViolations.Add([pscustomobject]@{ projectId = $projectId; status = $status.status
                activeGeneration = $status.activeGeneration; activeBuildId = $status.activeBuildId
                pendingGeneration = $status.pendingGeneration; pendingBuildId = $status.pendingBuildId })
    }
}

$metricSnapshot = @{}
if (-not [string]::IsNullOrWhiteSpace($MetricsUrl)) {
    $metricsResponse = Invoke-WebRequest -Uri $MetricsUrl -SkipHttpErrorCheck -TimeoutSec 15
    foreach ($line in ([string]$metricsResponse.Content -split "`n")) {
        if ($line -match '^(pmai_(?:index_outbox|index_active_tasks|index_duplicate_active_projects|milvus_error)[^{ ]*)(?:\{[^}]*\})?\s+(.+)$') {
            $metricSnapshot[$Matches[1]] = $Matches[2].Trim()
        }
    }
}

$rabbit = $null
if (-not [string]::IsNullOrWhiteSpace($RabbitManagementUrl)) {
    $pair = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${RabbitUsername}:${RabbitPassword}"))
    $rabbitHeaders = @{ Authorization = "Basic $pair" }
    $vhost = [Uri]::EscapeDataString($RabbitVhost)
    function Read-RabbitQueue([string]$Name) {
        $queue = [Uri]::EscapeDataString($Name)
        $response = Invoke-WebRequest -Uri "$RabbitManagementUrl/api/queues/$vhost/$queue" `
            -Headers $rabbitHeaders -SkipHttpErrorCheck -TimeoutSec 15
        if ([int]$response.StatusCode -eq 404) { return $null }
        return ([string]$response.Content | ConvertFrom-Json)
    }
    $mainQueue = Read-RabbitQueue $RabbitQueue
    $deadQueue = Read-RabbitQueue $RabbitDlq
    $rabbit = [ordered]@{ ready = $mainQueue.messages_ready; unacked = $mainQueue.messages_unacknowledged
        total = $mainQueue.messages; dlq = $deadQueue.messages }
}

$latencies = @($results | ForEach-Object { [double]$_.latencyMs })
$failed = @($results | Where-Object { -not $_.success })
$invariants = [ordered]@{
    no_permanent_pending_or_running = @($statuses.Values | Where-Object { $_.status -in @('PENDING', 'RUNNING') }).Count -eq 0
    one_active_task_per_project = $duplicateViolations.Count -eq 0
    generation_build_pointers_consistent = $pointerViolations.Count -eq 0
    no_cross_project_evidence = $crossProjectViolations.Count -eq 0
    outbox_drained = if ($metricSnapshot.ContainsKey('pmai_index_outbox_backlog')) {
        [double]$metricSnapshot['pmai_index_outbox_backlog'] -eq 0
    } else { $null }
}

[ordered]@{
    environment = 'pmai-v7-perf'
    target = "$($target.Scheme)://$($target.Authority)"
    scenarios = @($work.scenario | Sort-Object -Unique)
    projects = $ProjectIds
    requests = $results.Count
    concurrency = $Concurrency
    successes = $results.Count - $failed.Count
    failures = $failed.Count
    failure_details = @($failed | Group-Object statusCode, error | ForEach-Object { @{ key = $_.Name; count = $_.Count } })
    latency_ms = [ordered]@{ p50 = Get-Percentile $latencies 0.50; p95 = Get-Percentile $latencies 0.95
        p99 = Get-Percentile $latencies 0.99 }
    index_completion_ms_by_project = $indexDurations
    final_index_status_by_project = $statuses
    rabbit = $rabbit
    operational_metrics = $metricSnapshot
    invariant_checks = $invariants
    invariant_details = [ordered]@{ duplicate_active_tasks = $duplicateViolations
        pointer_violations = $pointerViolations; cross_project_evidence = $crossProjectViolations }
    measured_at_utc = [DateTime]::UtcNow.ToString('o')
    formal_capacity_baseline = $false
} | ConvertTo-Json -Depth 9

if ($failed.Count -gt 0 -or $invariants.Values -contains $false) { exit 2 }
