rem c-compiler env variables must be set too
set JAVA_HOME=C:\j2sdk1.4.2
set PATH=%JAVA_HOME%\bin;%PATH%

javah -classpath ..\..\ProjectX.jar net.sourceforge.dvb.projectx.video.IDCTSseNative

cl -I%JAVA_HOME%\include -I%JAVA_HOME%\include\win32 -LD net_sourceforge_dvb_projectx_video_IDCTSseNative.c idct_ref.c idct_cli.c -Feidctsse.dll

copy idctsse.dll ..\..\lib

pause