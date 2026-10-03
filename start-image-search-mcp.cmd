@echo off
setlocal enabledelayedexpansion

REM Start image-search MCP server only (SSE mode, port 8127)
REM Prereq: Pexels API key filled in .env at project root

set "ROOT=%~dp0"

REM Detect JDK 21 (IDEA managed JDK)
set "JAVA_HOME="
if exist "%USERPROFILE%\.jdks\ms-21.0.12.1" set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.12.1"
if "!JAVA_HOME!"=="" (
    echo [WARN] JDK 21 not found, falling back to system java.
) else (
    echo [INFO] JAVA_HOME=!JAVA_HOME!
)

REM Load PEXELS_API_KEY from .env
set "PEXELS_API_KEY="
for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%ROOT%.env") do (
    set "key=%%a"
    set "val=%%b"
    if "!key!"=="PEXELS_API_KEY" set "PEXELS_API_KEY=!val!"
)

if "!PEXELS_API_KEY!"=="" (
    echo [WARN] PEXELS_API_KEY is empty, image search will fail.
    echo [INFO] Configure PEXELS_API_KEY in .env (see .env.example).
) else (
    echo [INFO] PEXELS_API_KEY loaded: !PEXELS_API_KEY:~0,8!...
)

echo [INFO] Starting image-search-mcp-server (port 8127, SSE mode)...
cd /d "%ROOT%image-search-mcp-server"

REM Explicitly set SERVER_PORT to avoid SERVER__PORT env pollution
set "SERVER_PORT=8127"
call mvnw.cmd spring-boot:run

endlocal
