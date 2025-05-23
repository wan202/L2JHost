@echo off
title L2JAngel Game Console
:start
REM -------------------------------------
REM Default parameters for a basic server.

SET PATH="C:\Program Files\Java\jdk-17\bin"
java -Xmx1G -cp ../libs/*; net.sf.l2j.gameserver.GameServer
REM -------------------------------------
if ERRORLEVEL 2 goto restart
if ERRORLEVEL 1 goto error
goto end
:restart
echo.
echo Admin have restarted, please wait.
echo.
goto start
:error
echo.
echo Server have terminated abnormaly.
echo.
:end
echo.
echo Server terminated.
echo.
pause
