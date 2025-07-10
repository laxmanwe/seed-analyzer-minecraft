@echo off
echo Minecraft Seed Analyzer baslatiliyor...

REM Bin klasoru kontrolu
if not exist bin (
    echo Lutfen once compile.bat dosyasini calistirin!
    pause
    exit /b 1
)

REM Programi calistir
java -cp bin main.MinecraftSeedAnalyzer

pause
