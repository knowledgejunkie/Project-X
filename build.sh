#!/bin/sh
#build.sh
rm -f ProjectX.jar
mkdir -p build
javac -O -classpath lib/commons-net-1.3.0.jar -d build @sources.lst
cp resources/*.* build
jar cfvm projectx.jar MANIFEST.MF -C build .
#start von projectx
java -jar projectx.jar
