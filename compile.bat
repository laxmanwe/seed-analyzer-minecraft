@echo off
echo Minecraft Seed Analyzer derleniyor...

cd /d "%~dp0"

rem Bin klasorunu olustur
if not exist bin mkdir bin

rem Java dosyalarini derle
echo Derleniyor...
javac -encoding UTF-8 -d bin src\main\*.java src\ui\*.java src\models\*.java src\analyzer\*.java src\minecraft\*.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Derleme hatasi!
    echo Lutfen Java JDK 8+ yuklu oldugundan emin olun.
    pause
    exit /b 1
)

echo.
echo === DERLEME BASARILI ===
echo.
echo Calistirmak icin: java -cp bin main.MinecraftSeedAnalyzer
echo Veya: run.bat
echo.
pause
