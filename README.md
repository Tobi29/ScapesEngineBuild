# Scapes Engine Build
Build module for the [ScapesEngine](https://github.com/Tobi29/ScapesEngine)

Includes support for deployment:
* To Linux with simple tars to be processed by the package manager
* To MacOSX using an app bundle distributed in a tar
* To Windows using an Inno Setup based installer

This repo is meant to be added as a submodule into `buildSrc` to make its
classes available to the gradle scripts

## Deploy
You can check the available deployment targets using the `task` target (in
Deployment group).

Other than that, running the `deploy` target will run all available deploy
tasks.

Note: Windows deployment can take a long time due to compression, edit
`Setup.iss` to disable compression for testing.

### Linux
  * Unix environment highly recommended
  * `deployLinux32` and `deployLinux64` should be available out-of-the-box

### MacOSX
  * Unix environment highly recommended
  * `deployMacOSX` should be available out-of-the-box

### Windows
  * Download Launch4j for your platform
  * Place the extracted archives into `buildSrc/resources/Launch4j`
    (Make sure the jar is in `buildSrc/resources/Launch4j/launch4j.jar`!)
  * Download Inno Setup (Unicode version recommended)
  * Windows only:
    * Run the installer and install everything into
      `buildSrc/resources/Inno Setup 5` (Make sure the compiler is in
      `buildSrc/resources/Inno Setup 5/ISCC.exe`!)
  * Non-Windows only:
    * Install Wine for your system (Running `wine --version` in the terminal has
      to work, as depends on that command to be set up)
    * Run the Inno Setup installer
    * Copy the `Inno Setup 5` directory to `buildSrc/resources/Inno Setup 5`
      (Make sure the compiler is in
      `buildSrc/resources/Inno Setup 5/ISCC.exe`!)
    * Make sure to have a working Wine prefix when building
  * Run `tasks` target to check if `deployWindows` is available
