#! /bin/bash
# Build script for ProjectX under Linux

# check prerequisites
type javac >/dev/null 2>&1
if [ $? -ne 0 ] ; then
  echo "javac not found in \$PATH" >&2
  echo "Please install the JAVA compiler before calling this
script!" >&2
  exit 1
fi

if [ -e ./ProjectX.jar ] ; then
  rm -f ./ProjectX.jar
fi

if [ -d ./build ] ; then
  rm -rf ./build
fi

mkdir -p build
javac -encoding "ISO-8859-1" -deprecation -O -g:none -classpath lib/commons-net-1.3.0.jar:lib/jakarta-oro-2.0.8.jar -d build @sources.lst
cp ./resources/* ./build
jar cfvm ProjectX.jar MANIFEST.MF -C build .

######################################################
# Change Log
#
# 5.9.2005 by fredmuc
# - added -deprecation switch for javac
# - renamed bin directory to build 
# - conditional removal of files
#
