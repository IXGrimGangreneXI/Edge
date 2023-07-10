@echo off
setlocal EnableDelayedExpansion

:MAIN
SET libs=
for %%i in (*.jar) do SET libs=!libs!;%%i
for /r "./libs" %%i in (*.jar) do SET libs=!libs!;%%i
SET libs=%libs:~1%

java -cp "%libs%" org.asf.edge.gameplayapi.EdgeGameplayApiServerMain %*
IF %ERRORLEVEL% EQU 237 goto MAIN

if EXIST gameplayapiupdater.jar goto UPDATE
exit

:UPDATE
java -cp gameplayapiupdater.jar org.asf.edge.gameplayapi.EdgeGameplayApiServerUpdater --update
del gameplayapiupdater.jar
echo.
goto MAIN
