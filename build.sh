#!/bin/bash
#build.sh
rm -f ProjectX.jar
mkdir -p build
javac -encoding "ISO-8859-1" -O -g:none -classpath lib/commons-net-1.3.0.jar:lib/jakarta-oro-2.0.8.jar -d build @sources.lst
cp resources/* build
jar cfvm ProjectX.jar MANIFEST.MF -C build .
