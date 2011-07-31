/==============================================================================/

 ProjectX - a free Java based demux utility
 Copyright (C) 2001-2011 dvb.matt, All Rights Reserved

 By the authors, ProjectX is intended for educational purposes only, 
 as a non-commercial test project.

/==============================================================================/

 This program is free software; you can redistribute it and/or modify 
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

/==============================================================================/

 This program is completely designed as a test, therefore it mostly
 implements its own code instead of a derivation of an ISO reference
 source or any other code. Considerable effort has been expended to
 ensure an useful implementation, even in cases where the standards
 are ambiguous or misleading.
 Do not expect any useful output, even if that may be possible.

 For a program compliant to the international standards ISO 11172
 and ISO 13818 it is inevitable to use methods covered by patents
 in various countries. The authors of this program disclaim any
 liability for patent infringement caused by using, modifying or
 redistributing this program.

/==============================================================================/

 This program includes software developed by the
 Apache Software Foundation (http://www.apache.org/).

 This software contains fast high-quality IDCT decoder by Miha Peternel.

/==============================================================================/

 This program is provided in sourcecode form only,
 because it is meant for educational purposes.
 Binaries of this project itself will not be included (may not apply to external libraries)

 If you need an executable, you have to compile the package by yourself,
 or you ask someone to do so for you.

/==============================================================================/

 For a compilation, you need a Java SDK, which is available for various platforms.

 All classes should work at least with Sun's JDK/J2RE 1.2.2 (from year 2000) and higher.
 Note: under special circumstances, some graphic drivers/locales cause 
       big troubles with some versions of JRE's on different OS's.


 Extract all files from the received archive to a separate directory.
 Note:  dependent on the used JDK/JRE, ensure that the directory does not start with a '!'-sign or similars,
        otherwise you'll get an error like 'main class not found'

   /**
    * sample of a compilation (may differ on your system) :
    */
   (A) open "build.bat" with an editor and check/correct the entry/path of JAVA_HOME 
       of your installed JDK 

   (B) execute the "build.bat" on its place.
       that will compile all sources, 
       build the .jar file and
       copies the resource files from the resource folder into the new .jar,

       Notes:
       [i]   now, we made a package for the sources,
             the file sources.lst points to every required sourcefile and its location
       [ii]  the following libraries are required on this place from the V 0.82.0,
             related to the executed .jar:
             - lib/commons-net-1.3.0.jar  (compiled with JDK 1.2.2) *)
             - lib/jakarta-oro-2.0.8.jar  (compiled with JDK 1.2.2) *)
       [iii] the following libraries are optional from the V 0.82.0 :
             - lib/idct*.dll  (win32 / os/2 optimized lib's for a faster preview)
             copy one of them to the systems folder or where the compiled .jar is located
       [iv]  dependent on the used JDK, you'll encounter some 'warnings' about 'using a deprecated API'
             that's not critical, as long as an actual JDK still supports these older methods

       *)
       further informations and newer versions (mostly compiled with JDK 1.4.2) you'll find at:
       'jakarta.apache.org/site/binindex.cgi', look for 'Commons Net' and  'ORO'
       - using newer lib's possibly requires an update of the 'build.bat' and 'MANIFEST.MF' !


   (C) additional received resource files (e.g. other language files) can be added 
       to the root directory inside the .jar file, later, 
       or you put them into the same folder where the compiled .jar is executed.


   (D) new with version 0.90 and later:
       it is possible to compile and run this program without the gui (means all source files in /gui/..).
       so there is no requirement of running a X server or similars anymore, calling it from the commandline.

       if you can't start the non-gui version without a X server, try to call pjx with the 'headless' option
       java -Djava.awt.headless=true -jar ProjectX.jar [options] <file>
       
   (E) if you encounter frequently "OutOfMemory" Errors, define more RAM for the Java instance:
       java -Xms128m -Xmx128m -jar ProjectX.jar [options] <files>
       that reserves 128MB, instead of 64MB as default value.

   the compiling process doesn't take more than about 30 seconds.

/==============================================================================/

 For lazy beginners, a pre-compiled version (ProjectX.jar) suitable to JRE 1.2.2
 and higher is included. 
 Possibly, this won't give the best performance with all OS.

/==============================================================================/

  the 2 official ProjectX sites:

  Project + D/L, only @ sourceforge.net/projects/project-x/

  Support for the official version, only @ forum.dvbtechnics.info

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
	Kano / RoEn - Unix buils script
	Eric Albert - BrowserLauncher
	catapult,Bonni - Topfield 5x00 export
	MartinR - Gui-BaseOutputFileName
	...and all other supporters...

/===============================================================================/

