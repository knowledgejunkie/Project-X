@echo off

rem test if JAVA_HOME is already set in the system environment
if exist "%JAVA_HOME%\bin\java.exe" goto JAVA_HOME_SET

rem IMPORTANT! Edit the next line and set JAVA_HOME according to your environment.
set JAVA_HOME=C:\programme\jdk122

rem test if JAVA_HOME is set correctly now
if exist "%JAVA_HOME%\bin\java.exe" goto JAVA_HOME_SET

echo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
echo ! Error: JAVA_HOME not found                                             !
echo ! Please correct the build.bat file and set the JAVA_HOME path variable. !
echo ! If not installed a Java SDK can be downloaded from http://java.sun.com !
echo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
pause
goto END


:JAVA_HOME_SET

echo deleting old ProjectX.jar file
del ProjectX.jar

echo creating build subdirectory
mkdir build

echo compiling ProjectX with JAVA_HOME=%JAVA_HOME%
"%JAVA_HOME%\bin\javac.exe" -O -classpath lib\commons-net-1.3.0.jar -d build @sources.lst
if errorlevel 1 goto ERROR

echo copying resources
copy resources\*.* build
if errorlevel 1 goto ERROR

echo building ProjectX.jar file
"%JAVA_HOME%\bin\jar.exe" cfvm ProjectX.jar MANIFEST.MF -C build .
if errorlevel 1 goto ERROR
goto END

:ERROR
echo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
echo ! Some Errors occured, stopping build !
echo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
pause

:END
