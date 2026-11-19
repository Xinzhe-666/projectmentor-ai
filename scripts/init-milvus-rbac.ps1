param()

$ErrorActionPreference = 'Stop'
$required = @(
    'MILVUS_ROOT_CURRENT_PASSWORD', 'MILVUS_ROOT_NEW_PASSWORD',
    'MILVUS_APP_USERNAME', 'MILVUS_APP_PASSWORD'
)
foreach ($name in $required) {
    $value = [Environment]::GetEnvironmentVariable($name)
    if ([string]::IsNullOrWhiteSpace($value) -or $value.StartsWith('replace-with')) {
        throw "$name must be set to a non-placeholder value in the current shell."
    }
}
if ([string]::IsNullOrWhiteSpace($env:MILVUS_ADMIN_URI)) {
    $env:MILVUS_ADMIN_URI = 'http://127.0.0.1:19530'
}

Push-Location (Join-Path $PSScriptRoot '..\backend\projectmentor-server')
try {
    .\mvnw.cmd -q -DskipTests compile exec:java "-Dexec.mainClass=com.xinzhe.projectmentor.retrieval.milvus.MilvusRbacInitializer"
    if ($LASTEXITCODE -ne 0) { throw 'Milvus RBAC initializer failed.' }
} finally {
    Pop-Location
}
