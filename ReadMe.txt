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
 Binaries of this project itself will not be included.

 If you need an executable, you have to compile the package by yourself,
 or you ask someone to do so for you.

 For a compilation, you need a Java SDK, which is available for various platforms.

 All classes should work at least with Sun's JDK/J2RE 1.2.2 and higher.
 Note: under special circumstances, some graphic drivers/locales cause 
       big troubles with some versions of JRE's on different OS's.


 Extract all files from the received archive to a separate directory.

   sample of a compilation (may differ on your system) :

   (A) open "build.bat" with an editor an check/correct the entry/path of JAVA_HOME 
       of your installed JDK 

   (B) execute the "build.bat" on its place.
       that will compile all sources, 
       build the .jar file and
       copies the resource files from the resource folder into the new .jar,

       Note: now, we made a package for the sources
             the file source.lst points to any required sourcefile and its location

   (C) additional received resource files (e.g. other language files) can be added 
       to the .jar file, later, 
       or you put them into the same folder where the compiled .jar is executed.


   the compiling process doesn't take more than about 30 seconds.


 ProjectX - CVS at sourceforge.net/projects/project-x/

/==============================================================================/

 CREDITS

	-  thanx to all the people, who gave hints, files and other things to this project:

	dvb.matt - father of Project-X
	Lucike - forum hoster, documentation
	TheHorse - keyboardcontrol of preview
	java.lang - conditional patch of H-resolution
	R-One - DTS support
	ghost - dreambox file segment completion
	roehrist - CVS, X-input
	pstorch - i18n support
	chrisg - Topfield disk access (AddOn)
	jazzydane - danish translation
	Eric Albert - BrowserLauncher
	...and all other supporters...

/===============================================================================/

