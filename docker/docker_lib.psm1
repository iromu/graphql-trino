Set-Location (get-item $PSScriptRoot).parent.FullName

[xml]$pom = Get-Content pom.xml
$version = $pom.project.properties.revision

$PLATFORMS = "linux/amd64,linux/arm64"
$PLATFORM_AMD64 = "linux/amd64"
$PLATFORM_ARM64 = "linux/arm64"

# To activate custom dockerignore
$env:DOCKER_BUILDKIT = 1

$AMD64_BUILDER = "multiarch-builder"
$ARM64_BUILDER = "multiarch-builder"

Function invokeCommand
{
    Param ($command)
    Write-Output $command
    Invoke-Expression $command
}

Function buildx
{
    Param ([string] $name, [string] $path, [string]  $dockerfile, [string] $container, [string] $version, [string] $selectedPlatform)

    $LATEST_IMAGE_NAME = "iromu/" + $container + ":latest"
    $VERSION_IMAGE_NAME = "iromu/" + $container + ":" + $version
    $CACHE_IMAGE_NAME = "iromu/" + $container + ":cache"

    $linuxPath = $path.Replace('\', '/')
    $buildArgs = "--build-arg APP_PATH=${linuxPath} --build-arg NAME=${name}"
    $cacheArgs = "--cache-from ${CACHE_IMAGE_NAME} --cache-to ${CACHE_IMAGE_NAME}"

    $tags = "-t ${VERSION_IMAGE_NAME} -t ${LATEST_IMAGE_NAME}"

    # The path is different for Graal
    if ($dockerfile -eq "./docker/DockerfileGraal")
    {
        $path = '.'
    }

    if ($selectedPlatform)
    {
        If ($selectedPlatform -eq $PLATFORM_AMD64)
        {
            $theBuilder = $AMD64_BUILDER
        }
        Else
        {
            $theBuilder = $ARM64_BUILDER
        }
        invokeCommand "docker buildx build ${buildArgs} ${cacheArgs} ${tags} --push --builder ${theBuilder} --platform=${selectedPlatform} ${path} -f ${dockerfile}"
    }
    elseif($AMD64_BUILDER -eq "multiarch-builder" -And $ARM64_BUILDER -eq "multiarch-builder")
    {
        invokeCommand "docker buildx build ${buildArgs} ${cacheArgs} ${tags} --push --builder ${AMD64_BUILDER} --platform=${PLATFORMS} ${path} -f ${dockerfile}"
    }
    else
    {
        invokeCommand "docker buildx build ${buildArgs} ${cacheArgs} --tag ${CACHE_IMAGE_NAME} --push --builder ${AMD64_BUILDER} --platform=${PLATFORM_AMD64} ${path} -f ${dockerfile} "
        invokeCommand "docker buildx build ${buildArgs} ${cacheArgs} --tag ${CACHE_IMAGE_NAME} --push --builder ${ARM64_BUILDER} --platform=${PLATFORM_ARM64} ${path} -f ${dockerfile}"
        invokeCommand "docker buildx build ${buildArgs} --cache-from ${CACHE_IMAGE_NAME} ${tags} --push --builder ${AMD64_BUILDER} --platform=${PLATFORMS} ${path} -f ${dockerfile}"
    }
    Write-Output "FINISH  $path, $name, $VERSION_IMAGE_NAME, $dockerfile"
    Write-Output ""
    Start-Sleep -Seconds 2.5
}

Function dockerBuild
{
    Param ($path, $name, [string] $selectedPlatform)
    $container = $name
    $dockerfile = "./docker/DockerfileAlpine"
    $platform = $selectedPlatform

    buildx $name $path $dockerfile $container $version $platform
}

Function dockerBuildNative
{
    Param ($path, $name, [string] $selectedPlatform)
    $container = $name
    $dockerfile = "./docker/DockerfileGraal"
    buildx $name $path $dockerfile $container $version $selectedPlatform
}
