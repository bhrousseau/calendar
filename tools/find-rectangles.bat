@echo off
setlocal

rem Check if an image file is provided as argument
if "%~1"=="" (
    rem No argument provided, use the default test image
    set "IMAGE_PATH=..\assets\images\games\mmd\background\background-mask.png"
) else (
    rem Use the provided image path
    set "IMAGE_PATH=%~1"
)

rem Check if the image file exists
if not exist "%IMAGE_PATH%" (
    echo Error: Image file not found: %IMAGE_PATH%
    exit /b 1
)

rem Run the tool
java -cp bin com.widedot.tools.BlackRectangleFinder "%IMAGE_PATH%"