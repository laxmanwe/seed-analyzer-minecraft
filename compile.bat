@echo off
echo Minecraft Seed Analyzer derlemesi baslatiliyor...

REM Bin klasorunu olustur
if not exist bin mkdir bin

REM Java dosyalarini derle
echo Derleniyor...
javac -encoding UTF-8 -d bin src\main\*.java src\ui\*.java src\models\*.java src\analyzer\*.java

if %ERRORLEVEL% NEQ 0 (
    echo Derleme hatasi!
    pause
    exit /b 1
)

echo Derleme basarili!
echo.
echo Programi calistirmak icin: java -cp bin main.MinecraftSeedAnalyzer
echo.
pause
