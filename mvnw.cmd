@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@echo off
@setlocal EnableExtensions EnableDelayedExpansion

if "%MAVEN_BATCH_ECHO%"=="on" echo %MAVEN_BATCH_ECHO%

@set ERROR_CODE=0
@setlocal

if not "%JAVA_HOME%"=="" goto OkJHome
for %%i in (java.exe) do set "JAVACMD=%%~$PATH:i"
if not "%JAVACMD%"=="" goto checkJavaCmd

echo Error: JAVA_HOME not found in your environment. >&2
goto error

:OkJHome
set "JAVACMD=%JAVA_HOME%\bin\java.exe"

:checkJavaCmd
if exist "%JAVACMD%" goto chkMHome
echo Error: JAVA_HOME is set to an invalid directory: "%JAVA_HOME%" >&2
goto error

:chkMHome
set "DIRNAME=%~dp0"
if "%DIRNAME%"=="" set "DIRNAME=."
set "MAVEN_PROJECTBASEDIR=%DIRNAME%"

set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties"

if exist "%WRAPPER_JAR%" goto run

powershell -NoProfile -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $webClient = New-Object System.Net.WebClient; if (Test-Path '%WRAPPER_PROPERTIES%') { $url = (Get-Content '%WRAPPER_PROPERTIES%' | Select-String '^wrapperUrl=').Line.Substring(11) } else { $url = 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar' }; [System.IO.Directory]::CreateDirectory('%MAVEN_PROJECTBASEDIR%\.mvn\wrapper'); $webClient.DownloadFile($url, '%WRAPPER_JAR%')}" 2>nul

if exist "%WRAPPER_JAR%" goto run

for /f "delims=" %%F in ('dir /b /s "%USERPROFILE%\.m2\wrapper\dists\mvn.cmd" 2^>nul') do (
    set "FALLBACK_MVN=%%F"
    goto fallbackRun
)

:run
"%JAVACMD%" %MAVEN_OPTS% -classpath "%WRAPPER_JAR%" "-Dmaven.home=%MAVEN_PROJECTBASEDIR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
if ERRORLEVEL 1 goto error
goto end

:fallbackRun
call "%FALLBACK_MVN%" %*
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%
exit /B %ERROR_CODE%
