@echo off
setlocal EnableDelayedExpansion

:MAIN
SET libs=
for %%i in (*.jar) do SET libs=!libs!;%%i
for /r "./libs" %%i in (*.jar) do SET libs=!libs!;%%i
SET libs=%libs:~1%

java -cp "%libs%" org.asf.edge.commonapi.EdgeCommonApiServerMain %*
IF %ERRORLEVEL% EQU 237 goto MAIN

if EXIST commonapiupdater.jar goto UPDATE
exit

:UPDATE
java -cp commonapiupdater.jar org.asf.edge.commonapi.EdgeCommonApiServerUpdater --update
del commonapiupdater.jar
echo.
goto MAIN
