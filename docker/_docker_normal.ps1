$sw = [Diagnostics.Stopwatch]::StartNew()

# To activate custom dockerignore
$env:DOCKER_BUILDKIT = 1

Set-Location (get-item $PSScriptRoot).parent.FullName
Import-Module ./docker/docker_lib.psm1

dockerBuild '.' 'graphql-trino'

$sw.Stop()
$sw.Elapsed
