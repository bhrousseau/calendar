@echo off
setlocal

rem Check if an image file is provided as argument
if "%~1"=="" (
    rem No argument provided, use the default test image
    set "IMAGE_PATH=..\assets\images\games\mmd\background\background-mask.png"
    set "MODE=columns"
) else (
    rem Use the provided image path
    set "IMAGE_PATH=%~1"
    if "%~2"=="" (
        set "MODE=columns"
    ) else (
        set "MODE=%~2"
    )
)

rem Check if the image file exists
if not exist "%IMAGE_PATH%" (
    echo Error: Image file not found: %IMAGE_PATH%
    exit /b 1
)

rem Run the tool
echo Running BlackRectangleFinder on %IMAGE_PATH% with mode: %MODE%
java -cp bin com.widedot.tools.BlackRectangleFinder "%IMAGE_PATH%" "%MODE%"