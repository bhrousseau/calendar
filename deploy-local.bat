@echo off
echo ====================================
echo Build et preparation pour GitHub Pages
echo ====================================
echo.

REM Nettoyer les builds précédents
echo [1/4] Nettoyage...
call gradlew.bat :html:clean

REM Compiler la version HTML
echo [2/4] Compilation HTML...
call gradlew.bat :html:dist
if errorlevel 1 (
    echo ERREUR: La compilation a echoue
    exit /b 1
)

REM Créer le répertoire de déploiement
echo [3/4] Preparation des fichiers...
if exist deploy rmdir /s /q deploy
mkdir deploy
xcopy /E /I /Y html\build\dist\* deploy\

REM Créer .nojekyll pour GitHub Pages
echo. > deploy\.nojekyll

echo [4/4] Terminé!
echo.
echo Les fichiers sont prêts dans le dossier 'deploy\'
echo.
echo Pour deployer manuellement sur GitHub Pages:
echo   1. Committez et poussez vos changements sur la branche main
echo   2. L'action GitHub se lancera automatiquement
echo.
echo OU pour tester localement:
echo   cd deploy
echo   python -m http.server 8000
echo   Puis ouvrez http://localhost:8000
echo.
pause

