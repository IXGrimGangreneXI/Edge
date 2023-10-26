@echo off
setlocal EnableDelayedExpansion

:MAIN
SET libs=
for %%i in (*.jar) do SET libs=!libs!;%%i
for /r "./libs" %%i in (*.jar) do SET libs=!libs!;%%i
SET libs=%libs:~1%

java -cp "%libs%" org.asf.edge.mmoserver.EdgeMMOServerMain %*
IF %ERRORLEVEL% EQU 237 goto MAIN

if EXIST updater.jar goto UPDATE
exit

:UPDATE
java -cp updater.jar org.asf.edge.mmoserver.EdgeMMOServerUpdater --update
del updater.jar
echo.
goto MAIN
