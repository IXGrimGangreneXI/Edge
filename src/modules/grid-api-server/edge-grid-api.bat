@echo off
setlocal EnableDelayedExpansion

:MAIN
SET libs=
for %%i in (*.jar) do SET libs=!libs!;%%i
for /r "./libs" %%i in (*.jar) do SET libs=!libs!;%%i
SET libs=%libs:~1%

java -cp "%libs%" org.asf.edge.org.asf.edge.modules.gridapi.EdgeGridApiServerMain.EdgeGlobalServerMain %*
IF %ERRORLEVEL% EQU 237 goto MAIN
exit
