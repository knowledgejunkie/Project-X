# ProjectX - a free Java based demux utility

## Introduction

This repository was created as a way to track ProjectX development in git,
and to allow simple patching of the code to suit my own preferences. If
these updates also make ProjectX more useful for you, great!

Please see the upstream project's ReadMe.txt file for basic information.


## Repository Layout

This repository's branches are arranged in the following way:

-   master

    pristine clone of upstream's CVS repository kept in sync
    with 'git cvsimport'.

-   dev

    updates to make ProjectX more to my liking - more 'Linuxy', if
    you like (e.g. keybindings, rc file location)

Work is carried out in feature branches and merged into dev. *Nothing*
is merged into master.


## Updates

Nothing yet


## TODO

### Configuration file

The default ProjectX configuration file (X.ini) is created in the current
*working* directory when the jar file is run.

Instead of polluting my home directory (or any others) when
running ProjectX, I will add support to detect when ProjectX is being
run under a Linux environment and instead create the configuration file
in ~/.projectx called .projectxrc.


### Keybindings

Some of the default keybindings in ProjectX do not fit nicely with the
standard keybindings seen in most Linux applications. I therefore want to
remap several keybindings to more 'normal' keybindings found in most other
Linux applications I use: (e.g. Ctrl+Q = Quit, Ctrl+W = Close window)
