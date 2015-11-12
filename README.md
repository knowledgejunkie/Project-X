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
    you will (e.g. keybindings, configuration file location)

Work is carried out in feature branches and merged into dev. *Nothing*
is merged into master.


## Updates

### Configuration file location

The default ProjectX configuration file (X.ini) is created in the current
*working* directory when the ProjectX.jar file is run. Typically the user
ends up with the configuration file in their home directory.

Instead of polluting the home directory (or any others) when
running ProjectX, I have updated the default configuration file to
~/.projectx/projectx.conf. If an ~/X.ini file is found when running this
updated version of ProjectX it will be migrated automatically to the new
location.

### Keybindings

Many of the default keybindings in ProjectX are not consistent with those seen
in many graphical Linux applications. To benefit my muscle memory I have
updated several keybindings to better fit with those I am already
used to (e.g. Ctrl+Q = Quit, Ctrl+W = Close window, Ctrl+P = Preferences, ...).

### Automatically refresh input file browser

The input file browser list now automatically refreshes whenever it is opened
to reflect changes in the directory contents it is displaying.

### Delete all collections

A new button (and icon) has been added to the main GUI to permit all open
collections to be removed with a single click.


## TODO

- Deprecation warnings during build
- Unicode warnings
- Whitespace cleanup
