@echo off
echo Minecraft Seed Analyzer baslatiliyor...

cd /d "%~dp0"

if not exist "bin\main\MinecraftSeedAnalyzer.class" (
    echo Derlenmi dosyalar bulunamadi! Once derleniyor...
    call compile.bat
    echo.
)

if exist "bin\main\MinecraftSeedAnalyzer.class" (
    echo Uygulama baslatiliyor...
    java -cp bin main.MinecraftSeedAnalyzer
) else (
    echo Derleme basarisiz!
    pause
)
