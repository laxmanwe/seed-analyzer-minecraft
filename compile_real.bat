@echo off
echo Compiling Enhanced Minecraft Seed Analyzer...

cd /d "%~dp0"

rem Clean previous build
if exist "bin" rmdir /s /q "bin"
mkdir bin

rem Compile all Java files
echo Compiling source files...
javac -d bin -cp src src\main\*.java src\ui\*.java src\models\*.java src\analyzer\*.java src\minecraft\*.java

echo.
echo === COMPILATION RESULT ===
if %ERRORLEVEL% EQU 0 (
    echo ✓ Compilation successful!
    echo ✓ Enhanced NBT parser with error handling
    echo ✓ Support for modern and legacy Minecraft formats
    echo ✓ Robust chunk reading with graceful error recovery
    echo.
    echo To run: java -cp bin main.RealMinecraftSeedAnalyzer
    echo Or use: run_real.bat
    echo.
) else (
    echo ✗ Compilation failed with error code: %ERRORLEVEL%
    echo.
    echo Checking for common issues...
    echo.
    
    rem Check if source files exist
    if not exist "src\main\RealMinecraftSeedAnalyzer.java" (
        echo ✗ Main file missing: src\main\RealMinecraftSeedAnalyzer.java
    ) else (
        echo ✓ Main file exists
    )
    
    if not exist "src\minecraft\NBTReader.java" (
        echo ✗ NBT Reader missing: src\minecraft\NBTReader.java
    ) else (
        echo ✓ NBT Reader exists
    )
    
    echo.
    echo Please check the error messages above for details.
)

echo.
echo Press any key to continue...
pause