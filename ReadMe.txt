/==============================================================================/

 X - a free Java based demux utility
 Copyright (C) 2001-2004 dvb.matt, All Rights Reserved

 It is intended for educational purposes only, as a non-commercial test project.
 It may not be used otherwise. Most parts are only experimental.

/==============================================================================/

 This program is free software; you can redistribute it free of charge
 and/or modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation; either version 2 of the License,
 or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

/==============================================================================/

 This program is completely designed as a test, therefore it mostly
 implement its own code instead of a derivation of an ISO reference
 source or any other code. Considerable effort has been expended to
 ensure an useful implementation, even in cases where the standards
 are ambiguous or misleading.
 Do not expect any useful output from it, even if that may possible.

 For a program compliant to the international standards ISO 11172
 and ISO 13818 it is inevitable to use methods covered by patents
 in various countries. The authors of this program disclaim any
 liability for patent infringement caused by using, modifying or
 redistributing this program.

/==============================================================================/

 This program is provided in sourcecode form only,
 because it is meant for educational purposes.
 Binaries will not be included.

 If you need an executable, you have to compile the package by yourself,
 or you ask someone to do so for you.
 For compilation, you need a Java SDK, which is available for various platforms.

 All classes should work with Sun's JDK/J2RE 1.2.2 and higher.

 Extract all files from the received archive to a separate directory.
 the archive contains:

	ac3.bin   - optional, copy to the same place as your compiled version of X
	COPYING
	readme.txt

	htmls/..  - optional, copy to the same place as your compiled version of X
	index.html

	src/..
	AUDIO.java
	AudioFrameConstants.java
	Bitmap.java
	BMP.java
	BR_MONITOR.java
	Common.java
	CRC.java
	D2V.java
	DVBSubpicture.java
	HEXVIEWER.java
	Html.java
	IDDBufferedOutputStream.java
	Ifo.java
	LOGAREA.java
	MPAC.java
	MPAD.java
	MPVD.java
	RIFFHEADER.java
	SCAN.java
	StartUp.java
	SUBPICTURE.java
	Teletext.java
	TeletextPageMatrix.java
	TS.java
	UISwitchListener.java
	VIDEO.java
	X.java
	MANIFEST.MF	- manifest (refers to the main class and JDK version)

	(A) compile the sources:
		your_extract_path/src> [location_of_javac]javac [-O] *.java

	(B) create an .jar archive:
		your_extract_path/src> [location_of_jar]jar cfvm <new archive_name.jar> MANIFEST.MF *.class

	Note:	the .jar archive shall only contain the .class files without any path
		plus the manifest in  META-INF\MANIFEST.MF

	(C) execute the <new archive_name.jar>

	the compiling process doesn't take more than about 30 seconds.

/==============================================================================/

 CREDITS

    -  thanx to all the people, who gave hints, files and other things
       to the discontinued "father" of X and this new project

/===============================================================================/

