@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM
@REM Optional ENV vars
@REM   JAVA_HOME - location of a JDK home dir, required when download maven via java source
@REM   MVNW_REPOURL - repo url base for downloading maven distribution
@REM   MVNW_USERNAME/MVNW_PASSWORD - user and password for downloading maven
@REM   MVNW_VERBOSE - true: enable verbose log; others: silence the output
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __MVNW_CMD__=
@SET __MVNW_ERROR__=
@SET __MVNW_PSMODULEP_SAVE=%PSModulePath%
@SET PSModulePath=
@FOR /F "usebackq tokens=1* delims==" %%A IN (`powershell -noprofile "& {$scriptDir='%~dp0'; $script='%__MVNW_ARG0_NAME__%'; icm -ScriptBlock ([Scriptblock]::Create((Get-Content -Raw '%~f0'))) -NoNewScope}"`) DO @(
  IF "%%A"=="__MVNW_CMD__" (set __MVNW_CMD__=%%B)
  IF "%%A"=="__MVNW_ERROR__" (set __MVNW_ERROR__=%%B)
  IF "%%A"=="__MVNW_PSMODULEP_SAVE" (set PSModulePath=%%B)
)
@SET PSModulePath=%__MVNW_PSMODULEP_SAVE%
@SET __MVNW_PSMODULEP_SAVE=
@SET __MVNW_ARG0_NAME__=
@SET MVNW_USERNAME=
@SET MVNW_PASSWORD=
@IF "%__MVNW_ERROR__%"=="1" @(
  @SET __MVNW_ERROR__=
  @ECHO ERROR: __MVNW_CMD__ is not set >&2
  @EXIT /B 1
)
@IF NOT "%__MVNW_CMD__%"=="" @(
  @SET __MVNW_CMD__=
  @GOTO :__mvnw_cmd__
)
@ECHO ERROR: __MVNW_CMD__ is not set >&2
@EXIT /B 1
:__mvnw_cmd__
%__MVNW_CMD__% %*
@IF ERRORLEVEL 1 @GOTO error
@GOTO end
:error
@SET ERROR_CODE=1
:end
@ENDLOCAL & SET ERROR_CODE=%ERROR_CODE%
@IF NOT "%MVNW_LEAVE_RUNNING%"=="true" @PAUSE
@IF "%ERROR_CODE%"=="0" @EXIT /B 0
@EXIT /B %ERROR_CODE%
:__mvnw_psmodulep_init__
@IF NOT "%MVNW_VERBOSE%"=="" @echo MVNW_VERBOSE: "%MVNW_VERBOSE%"
@SET __MVNW_PSMODULEP_SAVE=PSModulePath=%PSModulePath%
@SET PSModulePath=
@IF NOT "%MVNW_PSMODULEP_SAVE%"=="" @echo __MVNW_PSMODULEP_SAVE=%__MVNW_PSMODULEP_SAVE%
@GOTO :EOF
@REM -------------------------- PowerShell Script --------------------------
__MVNW_VERSION__="3.3.2"

# This script must be able to run with PowerShell version 2.0. We cannot use $PSVersionTable.PSVersion because it was introduced in PS 3.0.
# We use [Environment]::Version to get the .NET version, then use reflection to call an internal method to get the PowerShell version.
# PS 2.0 is supported on .NET 2.0 - .NET 3.5
$ps_version = 2

if ($PSVersionTable -and $PSVersionTable.PSVersion) {
    $ps_version = $PSVersionTable.PSVersion.Major
}

if ($ps_version -ge 3) {
    $verbose = $env:MVNW_VERBOSE -eq "true"
} else {
    $verbose = $false
}

function Write-Verbose-Message {
    param ([string]$message)
    if ($verbose) {
        Write-Output $message
    }
}

# workaround for PS2.0: powershell does not have Test-Path -PathType Leaf
function Test-FileExists {
    param ([string]$file)
    if (Test-Path $file) {
        return !(Get-Item $file).PSIsContainer
    }
    return $false
}

$basedir = Split-Path -Path $scriptDir -Parent
Write-Verbose-Message "basedir=$basedir"

$MAVEN_PROJECTBASEDIR = $env:MAVEN_BASEDIR
if (-not $MAVEN_PROJECTBASEDIR) {
    $MAVEN_PROJECTBASEDIR = $basedir
}
Write-Verbose-Message "MAVEN_PROJECTBASEDIR=$MAVEN_PROJECTBASEDIR"

$wrapperJarPath = "$scriptDir\.mvn\wrapper\maven-wrapper.jar"
$wrapperPropertiesPath = "$scriptDir\.mvn\wrapper\maven-wrapper.properties"

function Get-Property {
    param ([string]$key, [string]$file)
    $value = $null
    Get-Content $file | ForEach-Object {
        if ($_ -match "^$key\s*=\s*(.*)$") {
            $value = $matches[1].Trim()
        }
    }
    return $value
}

if (Test-FileExists $wrapperPropertiesPath) {
    $distributionUrl = Get-Property -key "distributionUrl" -file $wrapperPropertiesPath
    $distributionSha256Sum = Get-Property -key "distributionSha256Sum" -file $wrapperPropertiesPath
    $wrapperUrl = Get-Property -key "wrapperUrl" -file $wrapperPropertiesPath
} else {
    Write-Output "ERROR: Maven wrapper properties file not found: $wrapperPropertiesPath"
    "__MVNW_ERROR__=1"
    exit 1
}

if (-not $distributionUrl) {
    Write-Output "ERROR: distributionUrl is not set in $wrapperPropertiesPath"
    "__MVNW_ERROR__=1"
    exit 1
}

# Determine Maven home directory based on distribution URL
function Get-HashCode {
    param ([string]$str)
    $h = 0
    $str.ToCharArray() | ForEach-Object {
        $h = (($h * 31) + [int]$_) % 4294967296
    }
    return "{0:x}" -f $h
}

$distributionUrlName = [System.IO.Path]::GetFileName($distributionUrl)
$distributionUrlNameMain = [System.IO.Path]::GetFileNameWithoutExtension($distributionUrlName)
if ($distributionUrlNameMain -match "-bin$") {
    $distributionUrlNameMain = $distributionUrlNameMain -replace "-bin$", ""
}

$MAVEN_USER_HOME = if ($env:MAVEN_USER_HOME) { $env:MAVEN_USER_HOME } else { "$env:USERPROFILE\.m2" }
$MAVEN_HOME = "$MAVEN_USER_HOME\wrapper\dists\$distributionUrlNameMain\$(Get-HashCode $distributionUrl)"
Write-Verbose-Message "MAVEN_HOME=$MAVEN_HOME"

# Check if Maven is already installed
if (Test-Path "$MAVEN_HOME\bin\mvn.cmd") {
    Write-Verbose-Message "Found Maven in MAVEN_HOME"
    "__MVNW_CMD__=""$MAVEN_HOME\bin\mvn.cmd"" %*"
    exit 0
}

# Download and install Maven
Write-Verbose-Message "Couldn't find Maven home, downloading and installing it..."
Write-Verbose-Message "Downloading from: $distributionUrl"

$TMP_DOWNLOAD_DIR = New-Item -ItemType Directory -Path "$env:TEMP\mvnw-$(New-Guid)"
$distributionFile = "$TMP_DOWNLOAD_DIR\$distributionUrlName"

try {
    Write-Verbose-Message "Downloading to: $distributionFile"

    # Download using .NET WebClient
    $webclient = New-Object System.Net.WebClient
    if ($env:MVNW_USERNAME -and $env:MVNW_PASSWORD) {
        $webclient.Credentials = New-Object System.Net.NetworkCredential($env:MVNW_USERNAME, $env:MVNW_PASSWORD)
    }
    if ($env:MVNW_REPOURL) {
        $distributionUrl = "$env:MVNW_REPOURL/org/apache/maven/$($distributionUrl.Substring($distributionUrl.LastIndexOf('/org/apache/maven/') + 1))"
        Write-Verbose-Message "Using MVNW_REPOURL: $distributionUrl"
    }
    $webclient.DownloadFile($distributionUrl, $distributionFile)

    # Validate SHA-256 if specified
    if ($distributionSha256Sum) {
        Write-Verbose-Message "Validating SHA-256 checksum"
        $stream = [System.IO.File]::OpenRead($distributionFile)
        $sha256 = [System.Security.Cryptography.SHA256]::Create()
        $hash = $sha256.ComputeHash($stream)
        $stream.Close()
        $hashString = [System.BitConverter]::ToString($hash).Replace("-", "").ToLower()

        if ($hashString -ne $distributionSha256Sum) {
            Write-Output "ERROR: Failed to validate Maven distribution SHA-256, your Maven distribution might be compromised."
            Write-Output "If you updated your Maven version, you need to update the specified distributionSha256Sum property."
            Remove-Item -Recurse -Force $TMP_DOWNLOAD_DIR
            "__MVNW_ERROR__=1"
            exit 1
        }
    }

    # Extract the distribution
    Write-Verbose-Message "Extracting Maven distribution to $TMP_DOWNLOAD_DIR"
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::ExtractToDirectory($distributionFile, $TMP_DOWNLOAD_DIR)

    # Move to final location
    $extractedDir = Get-ChildItem -Path $TMP_DOWNLOAD_DIR -Directory | Where-Object { $_.Name -match "^apache-maven-" -or $_.Name -match "^maven-mvnd-" } | Select-Object -First 1
    if ($extractedDir) {
        Write-Verbose-Message "Moving Maven to $MAVEN_HOME"
        New-Item -ItemType Directory -Path (Split-Path $MAVEN_HOME -Parent) -Force | Out-Null
        Move-Item -Path $extractedDir.FullName -Destination $MAVEN_HOME -Force
        "$distributionUrl" | Out-File -FilePath "$MAVEN_HOME\mvnw.url" -Encoding ASCII
    } else {
        Write-Output "ERROR: Could not find extracted Maven directory"
        Remove-Item -Recurse -Force $TMP_DOWNLOAD_DIR
        "__MVNW_ERROR__=1"
        exit 1
    }

    Remove-Item -Recurse -Force $TMP_DOWNLOAD_DIR
    Write-Verbose-Message "Maven installed successfully"
    "__MVNW_CMD__=""$MAVEN_HOME\bin\mvn.cmd"" %*"
    exit 0

} catch {
    Write-Output "ERROR: Failed to download or install Maven"
    Write-Output $_.Exception.Message
    if (Test-Path $TMP_DOWNLOAD_DIR) {
        Remove-Item -Recurse -Force $TMP_DOWNLOAD_DIR
    }
    "__MVNW_ERROR__=1"
    exit 1
}
