@echo off
setlocal EnableDelayedExpansion

:MAIN
SET libs=
for %%i in (*.jar) do SET libs=!libs!;%%i
for /r "./libs" %%i in (*.jar) do SET libs=!libs!;%%i
SET libs=%libs:~1%

java -cp "%libs%" org.asf.edge.mmoserver.EdgeMMOServerMain %*
IF %ERRORLEVEL% EQU 237 goto MAIN

if EXIST mmoserverupdater.jar goto UPDATE
exit

:UPDATE
java -cp mmoserverupdater.jar org.asf.edge.mmoserver.EdgeMMOServerUpdater --update
del mmoserverupdater.jar
echo.
goto MAIN
