@echo off
echo Building Java tools...

rem Create bin directory if it doesn't exist
if not exist bin mkdir bin

rem Compile BlackRectangleFinder
javac -d bin src/main/java/com/widedot/tools/BlackRectangleFinder.java

if %ERRORLEVEL% EQU 0 (
    echo Build successful!
) else (
    echo Build failed!
    exit /b 1
) 