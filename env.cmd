@echo off

echo Setting environment

REM Ohne Web Services Development Pack
set JAVA_HOME=C:\Programme\j2sdk1.4.2_02
set ANT_HOME=C:\Entwicklung\lib\ant_1.5
set AXISCLASSPATH=C:\jboss-3.2.2\server\default\deploy\webapps\axis.war\WEB-INF\lib\axis-ant.jar;C:\jboss-3.2.2\server\default\deploy\webapps\axis.war\WEB-INF\lib\axis.jar;C:\jboss-3.2.2\server\default\deploy\webapps\axis.war\WEB-INF\lib\commons-discovery.jar;C:\jboss-3.2.2\server\default\deploy\webapps\axis.war\WEB-INF\lib\commons-logging.jar;C:\jboss-3.2.2\server\default\deploy\webapps\axis.war\WEB-INF\lib\jaxrpc.jar;C:\jboss-3.2.2\server\default\deploy\webapps\axis.war\WEB-INF\lib\log4j-1.2.8.jar;C:\jboss-3.2.2\server\default\deploy\webapps\axis.war\WEB-INF\lib\saaj.jar;C:\jboss-3.2.2\server\default\deploy\webapps\axis.war\WEB-INF\lib\wsdl4j.jar
set PATH=C:\Programme\j2sdk1.4.2_02\bin;C:\Entwicklung\lib\ant_1.5\bin;C:\Programme\GNU\WinCvs_1.2;%PATH%

REM Mit Web Services Development Pack
REM set JAVA_HOME=C:\Programme\j2sdk1.4.2_02
REM set ANT_HOME=
REM set PATH=C:\Programme\j2sdk1.4.2_02\bin;C:\Entwicklung\jwsdp-1.3\bin;C:\Entwicklung\jwsdp-1.3\jwsdp-shared\bin;C:\Entwicklung\jwsdp-1.3\apache-ant\bin;C:\Programme\GNU\WinCvs_1.2;%PATH%
REM set JRE_HOME=C:\Programme\Java\j2re1.4.2_02
