@echo on

rem System dependent paths to the JDK
#set JAVA_HOME=C:\j2sdk1.4.2
#set JAVA_HOME=C:\Programme\Java\jdk1.5.0
set JAVA_HOME=C:\jdk1.2.2
set PATH=%JAVA_HOME%\bin

del ProjectX.jar

mkdir build

javac.exe -O -classpath lib\commons-net-1.2.2.jar -d build @sources.lst

pause

copy resources\*.* build

jar.exe cfvm ProjectX.jar MANIFEST.MF -C build .
