@echo off
echo Starting Minecraft Seed Analyzer with Real World Support...

cd /d "%~dp0"

if not exist "bin\main\RealMinecraftSeedAnalyzer.class" (
    echo Class files not found! Running compilation first...
    call compile_real.bat
    echo.
)

if exist "bin\main\RealMinecraftSeedAnalyzer.class" (
    echo Launching application...
    java -cp bin main.RealMinecraftSeedAnalyzer
) else (
    echo Failed to find compiled classes!
    pause
)