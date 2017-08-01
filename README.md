# Scapes Engine Build
Build module for [ScapesEngine](https://github.com/Tobi29/ScapesEngine) based
applications

## Features
### Deployment
* To Linux with simple tars to be processed by the package manager
* To MacOSX using an app bundle distributed in a tar
* To Windows using an Inno Setup based installer or extractable zips

### Asset bundling
`AssetBundler` task can bundle a directory into a binary tag structure
to easy distribution (e.g. for assets stored on a web server)

### Base64 embedding
`Base64Embedder` turns a file into Kotlin source code, relying
on some decoding utilities to allow bundling assets directly with code,
specifically useful when compiling with Kotlin/JS.

### NPM configuration
`NpmConfigTask` creates an npm package configuration for exporting
Kotlin/JS projects into a npm environment (e.g. for webpack).

### Webpack configuration
`WebpackConfigTask` creates a webpack configuration for deploying
Kotlin/JS project to the browser.

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

## JavaScript

The npm and webpack tasks rely on working `npm` and `node` commands on the path.
