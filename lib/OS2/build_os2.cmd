REM *******************************************
REM Build idctssl.dll with OpenWatcom for OS/2
REM
REM Version 1.1
REM RBRi 2006, 2008
REM
REM HowTo:
REM
REM (1)  Adjust the paths to your JDK 1.3.1
REM      and watcom 1.7 compiler
REM (2)  Build the class files.
REM (3)  Edit file idct_ref.c
REM      at the end of the file there is a line
REM      _mm_empty();
REM      change this to _m_empty();
REM (4)  run this script the new dll is in this
REM      directory
REM *******************************************
@echo off
setlocal

REM !!!! Change this....

SET JAVA_HOME=C:\java131
SET WATCOM=D:\progs\watcom_17

REM !!!!!!!!!!!!!!!!!!!!!!!

rem setup the environment
SET PATH=%WATCOM%\BINP;%WATCOM%\BINW;%PATH%
SET INCLUDE=%WATCOM%\H;%WATCOM%\H\OS2
SET BEGINLIBPATH=%WATCOM%\BINP\DLL

rem create header
%JAVA_HOME%\bin\javah -classpath ..\..\build net.sourceforge.dvb.projectx.video.IDCTSseNative

rem compile
wcc386 -bd -6s -oh -i%JAVA_HOME%\include;%JAVA_HOME%\include\os2 net_sourceforge_dvb_projectx_video_IDCTSseNative.c
wcc386 -bd -6s -oh -i%JAVA_HOME%\include;%JAVA_HOME%\include\os2 idct_ref.c
wcc386 -bd -6s -oh -i%JAVA_HOME%\include;%JAVA_HOME%\include\os2 idct_cli.c

rem link
set files=net_sourceforge_dvb_projectx_video_IDCTSseNative,idct_ref,idct_cli
wlink system os2v2 dll initinstance file %files% name idctsse

endlocal

