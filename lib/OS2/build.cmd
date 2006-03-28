REM *******************************************
REM Build idctssl.dll with OpenWatcom for OS/2
REM
REM Version 1.0
REM RBRi 2006
REM *******************************************
@echo off
setlocal

REM !!!! Change this....

SET JAVA_HOME=C:\java131
SET WATCOM=D:\progs\watcom

REM !!!!!!!!!!!!!!!!!!!!!!!

rem setup the environment
SET PATH=%WATCOM%\BINP;%WATCOM%\BINW;%PATH%
SET INCLUDE=%WATCOM%\H;%WATCOM%\H\OS2
SET BEGINLIBPATH=%WATCOM%\BINP\DLL

rem create header
%JAVA_HOME%\bin\javah -classpath ..\..\..\src net.sourceforge.dvb.projectx.video.IDCTSseNative

rem compile
wcc386 -bd -6s -oh -i%JAVA_HOME%\include;%JAVA_HOME%\include\os2 net_sourceforge_dvb_projectx_video_IDCTSseNative.c
wcc386 -bd -6s -oh -i%JAVA_HOME%\include;%JAVA_HOME%\include\os2 idct_ref.c
wcc386 -bd -6s -oh -i%JAVA_HOME%\include;%JAVA_HOME%\include\os2 idct_cli.c

rem link
set files=net_sourceforge_dvb_projectx_video_IDCTSseNative,idct_ref,idct_cli
set exports=Java_net_sourceforge_dvb_projectx_video_IDCTSseNative_referenceIDCT
wlink system os2v2 dll initinstance file %files% name idctsse export %exports%

endlocal

