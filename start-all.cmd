@echo off
setlocal enabledelayedexpansion

REM ============================================================
REM  One-click start: backend (8123) + image-search MCP (8127)
REM  Prereqs:
REM    1. Keys filled in .env at project root (see .env.example)
REM    2. Ollama running locally (needed for RAG embedding only)
REM    3. JDK 21 available
REM ============================================================

set "ROOT=%~dp0"

REM ---------- Detect JDK 21 (IDEA managed JDK under %USERPROFILE%\.jdks) ----------
set "JAVA_HOME="
if exist "%USERPROFILE%\.jdks\ms-21.0.12.1" set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.12.1"
if "!JAVA_HOME!"=="" (
    echo [WARN] JDK 21 not found under %USERPROFILE%\.jdks\ms-21.0.12.1
    echo [INFO] Falling back to system java (JAVA_HOME / PATH)
) else (
    echo [INFO] JAVA_HOME=!JAVA_HOME!
)

REM ---------- Load keys from .env ----------
if not exist "%ROOT%.env" (
    echo [WARN] .env not found. Keys will be empty, chat/image may fail.
    echo [INFO] Copy .env.example to .env and fill in real keys.
)

set "SPRING_AI_OPENAI_API_KEY="
set "SEARCH_API_API_KEY="
set "PEXELS_API_KEY="
for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%ROOT%.env") do (
    set "key=%%a"
    set "val=%%b"
    if "!key!"=="SPRING_AI_OPENAI_API_KEY" set "SPRING_AI_OPENAI_API_KEY=!val!"
    if "!key!"=="SEARCH_API_API_KEY" set "SEARCH_API_API_KEY=!val!"
    if "!key!"=="PEXELS_API_KEY" set "PEXELS_API_KEY=!val!"
)

echo ============================================================
echo  [1/2] Starting image-search MCP server (port 8127)
echo ============================================================
start "image-search-mcp-server" cmd /k "set JAVA_HOME=!JAVA_HOME! && cd /d "%ROOT%image-search-mcp-server" && set PEXELS_API_KEY=!PEXELS_API_KEY! && set SERVER_PORT=8127 && mvnw.cmd spring-boot:run"

echo ============================================================
echo  [2/2] Starting backend (port 8123)
echo ============================================================
start "travel-ai-agent-backend" cmd /k "set JAVA_HOME=!JAVA_HOME! && cd /d "%ROOT%" && set SPRING_AI_OPENAI_API_KEY=!SPRING_AI_OPENAI_API_KEY! && set SEARCH_API_API_KEY=!SEARCH_API_API_KEY! && set SERVER_PORT=8123 && mvnw.cmd spring-boot:run"

echo.
echo ============================================================
echo  Two services started in separate windows:
echo    - image-search-mcp-server : http://localhost:8127
echo    - backend                 : http://localhost:8123/api
echo  Health check: http://localhost:8123/api/health  (ok = ready)
echo  Stop: close the corresponding window
echo ============================================================

endlocal
