@echo off

set JAVA_HOME=C:\Programme\j2sdk1.4.2_02

del ProjectX.jar
javac.exe -O -classpath src -d build src\*.java
jar.exe cfvm ProjectX.jar MANIFEST.MF -C build .

REM cd src
REM javac.exe -O *.java
REM jar.exe cfvm ..\ProjectX.jar MANIFEST.MF *.class
REM cd ..

pause
