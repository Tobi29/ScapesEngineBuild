/*
 * Copyright 2012, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#import <Cocoa/Cocoa.h>
#include <dlfcn.h>
#include <jni.h>
#include <pwd.h>

#define JAVA_LAUNCH_ERROR "JavaLaunchError"


#define CF_BUNDLE_NAME "CFBundleName"
#define JVM_RUNTIME_KEY "JVMRuntime"
#define WORKING_DIRECTORY_IN_LIBRARY "WorkingDirectoryInLibrary"
#define JVM_MAIN_CLASS_NAME_KEY "JVMMainClassName"
#define JVM_OPTIONS_KEY "JVMOptions"
#define JVM_DEFAULT_OPTIONS_KEY "JVMDefaultOptions"
#define JVM_ARGUMENTS_KEY "JVMArguments"

#define UNSPECIFIED_ERROR "An unknown error occurred."

#define APP_ROOT_PREFIX "$APP_ROOT"
#define JVM_RUNTIME "$JVM_RUNTIME"

typedef int (JNICALL *JLI_Launch_t)(int argc, char ** argv,
                                    int jargc, const char** jargv,
                                    int appclassc, const char** appclassv,
                                    const char* fullversion,
                                    const char* dotversion,
                                    const char* pname,
                                    const char* lname,
                                    jboolean javaargs,
                                    jboolean cpwildcard,
                                    jboolean javaw,
                                    jint ergo);

static char** progargv = NULL;
static int progargc = 0;
static int launchCount = 0;

int launch(char *, int, char **);
NSString * addDirectoryToSystemArguments(NSUInteger, NSSearchPathDomainMask, NSString *, NSMutableArray *);
void addModifierFlagToSystemArguments(NSEventModifierFlags, NSString *, NSEventModifierFlags, NSMutableArray *);

int main(int argc, char *argv[]) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    int result;
    @try {
        if ((argc > 1) && (launchCount == 0)) {
            progargc = argc - 1;
            progargv = &argv[1];
        }

        launch(argv[0], progargc, progargv);
        result = 0;
    } @catch (NSException *exception) {
        NSAlert *alert = [[NSAlert alloc] init];
        [alert setAlertStyle:NSCriticalAlertStyle];
        [alert setMessageText:[exception reason]];
        [alert runModal];

        result = 1;
    }

    [pool drain];

    return result;
}

int launch(char *commandName, int progargc, char *progargv[]) {
    // Get the main bundle
    NSBundle *mainBundle = [NSBundle mainBundle];

    // Get the main bundle's info dictionary
    NSDictionary *infoDictionary = [mainBundle infoDictionary];

    // Allocate file manager
    BOOL isDir;
    NSFileManager *fm = [[NSFileManager alloc] init];

    // Set working directory to library path of the app
    bool workingDirectoryInLibrary = [[infoDictionary objectForKey:@WORKING_DIRECTORY_IN_LIBRARY] boolValue];
    if (workingDirectoryInLibrary) {
        NSString* appLibraryPath = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES)[0];
        NSString* bundleName = [infoDictionary objectForKey:@CF_BUNDLE_NAME];
        NSString* workingDir = [appLibraryPath stringByAppendingPathComponent:bundleName];

        if(![fm createDirectoryAtPath:workingDir withIntermediateDirectories:YES attributes:nil error:nil]) {
            NSString *msg = NSLocalizedString(@"LibraryMkdirError", @UNSPECIFIED_ERROR);

            NSLog(@"Error creating library directory: %@", workingDir);

            [[NSException exceptionWithName:@JAVA_LAUNCH_ERROR
                    reason:msg userInfo:nil] raise];
        }

        if (chdir([workingDir UTF8String]) != 0) {
            NSString *msg = NSLocalizedString(@"LibraryChdirError", @UNSPECIFIED_ERROR);

            NSLog(@"Error entering library directory: %@", workingDir);

            [[NSException exceptionWithName:@JAVA_LAUNCH_ERROR
                    reason:msg userInfo:nil] raise];
        }
    }

    // Locate the JLI_Launch() function
    NSString *runtime = [infoDictionary objectForKey:@JVM_RUNTIME_KEY];
    NSString *runtimePath = [[mainBundle builtInPlugInsPath] stringByAppendingPathComponent:runtime];

    NSString *dylibRelPath = @"Contents/Home/lib/jli/libjli.dylib";
    NSString *javaDylib = [runtimePath stringByAppendingPathComponent:dylibRelPath];
    BOOL javaDylibFileExists = [fm fileExistsAtPath:javaDylib isDirectory:&isDir];
    if (!javaDylibFileExists || isDir) {
        javaDylib = NULL;
    }

    const char *libjliPath = NULL;
    if (javaDylib != nil)
    {
        libjliPath = [javaDylib fileSystemRepresentation];
    }

    void *libJLI = dlopen(libjliPath, RTLD_LAZY);

    JLI_Launch_t jli_LaunchFxnPtr = NULL;
    if (libJLI != NULL) {
        jli_LaunchFxnPtr = dlsym(libJLI, "JLI_Launch");
    }

    if (jli_LaunchFxnPtr == NULL) {
        NSString *msg = NSLocalizedString(@"JRELoadError", @UNSPECIFIED_ERROR);

        NSLog(@"Error launching JVM Runtime (%@) Relative Path: '%@' (dylib: %@)\n  error: %@",
              runtime, runtimePath, javaDylib, msg);

        [[NSException exceptionWithName:@JAVA_LAUNCH_ERROR
                reason:msg userInfo:nil] raise];
    }

    // Get the main class name
    NSString *mainClassName = [infoDictionary objectForKey:@JVM_MAIN_CLASS_NAME_KEY];
    if (mainClassName == nil) {
        [[NSException exceptionWithName:@JAVA_LAUNCH_ERROR
            reason:NSLocalizedString(@"MainClassNameRequired", @UNSPECIFIED_ERROR)
            userInfo:nil] raise];
    }

    // Set the class path
    NSString *mainBundlePath = [mainBundle bundlePath];

    // make sure the bundle path does not contain a colon, as that messes up the java.class.path,
    // because colons are used a path separators and cannot be escaped.

    // funny enough, Finder does not let you create folder with colons in their names,
    // but when you create a folder with a slash, e.g. "audio/video", it is accepted
    // and turned into... you guessed it, a colon:
    // "audio:video"
    if ([mainBundlePath rangeOfString:@":"].location != NSNotFound) {
        [[NSException exceptionWithName:@JAVA_LAUNCH_ERROR
            reason:NSLocalizedString(@"BundlePathContainsColon", @UNSPECIFIED_ERROR)
            userInfo:nil] raise];
    }

    NSString *javaPath = [mainBundlePath stringByAppendingString:@"/Contents/Java"];
    NSMutableArray *systemArguments = [[NSMutableArray alloc] init];
    NSMutableString *classPath = [NSMutableString stringWithString:@"-Djava.class.path="];

    // Implicit classpath, so use the contents of the "Java" folder to build an explicit classpath
    [classPath appendFormat:@"%@/Classes", javaPath];
    NSFileManager *defaultFileManager = [NSFileManager defaultManager];
    NSArray *javaDirectoryContents = [defaultFileManager contentsOfDirectoryAtPath:javaPath error:nil];
    if (javaDirectoryContents == nil) {
        [[NSException exceptionWithName:@JAVA_LAUNCH_ERROR
            reason:NSLocalizedString(@"JavaDirectoryNotFound", @UNSPECIFIED_ERROR)
            userInfo:nil] raise];
    }

    for (NSString *file in javaDirectoryContents) {
        if ([file hasSuffix:@".jar"]) {
            [classPath appendFormat:@":%@/%@", javaPath, file];
        }
    }

    [systemArguments addObject:classPath];

    // Set the library path
    NSString *libraryPath = [NSString stringWithFormat:@"-Djava.library.path=%@/Contents/MacOS", mainBundlePath];
    [systemArguments addObject:libraryPath];

    // Get the VM options
    NSArray *options = [infoDictionary objectForKey:@JVM_OPTIONS_KEY];
    if (options == nil) {
        options = [NSArray array];
    }

    // Get the VM default options
    NSArray *defaultOptions = [NSArray array];
    NSDictionary *defaultOptionsDict = [infoDictionary objectForKey:@JVM_DEFAULT_OPTIONS_KEY];
    if (defaultOptionsDict != nil) {
        NSMutableDictionary *defaults = [NSMutableDictionary dictionaryWithDictionary: defaultOptionsDict];
        // Replace default options with user specific options, if available
        NSUserDefaults *userDefaults = [NSUserDefaults standardUserDefaults];
        // Create special key that should be used by Java's java.util.Preferences impl
        // Requires us to use "/" + bundleIdentifier.replace('.', '/') + "/JVMOptions/" as node on the Java side
        // Beware: bundleIdentifiers shorter than 3 segments are placed in a different file!
        // See java/util/prefs/MacOSXPreferences.java of OpenJDK for details
        NSString *bundleDictionaryKey = [mainBundle bundleIdentifier];
        bundleDictionaryKey = [bundleDictionaryKey stringByReplacingOccurrencesOfString:@"." withString:@"/"];
        bundleDictionaryKey = [NSString stringWithFormat: @"/%@/", bundleDictionaryKey];

        NSDictionary *bundleDictionary = [userDefaults dictionaryForKey: bundleDictionaryKey];
        if (bundleDictionary != nil) {
            NSDictionary *jvmOptionsDictionary = [bundleDictionary objectForKey: @"JVMOptions/"];
            for (NSString *key in jvmOptionsDictionary) {
                NSString *value = [jvmOptionsDictionary objectForKey:key];
                [defaults setObject: value forKey: key];
            }
        }
        defaultOptions = [defaults allValues];
    }

    // Get the application arguments
    NSArray *arguments = [infoDictionary objectForKey:@JVM_ARGUMENTS_KEY];
    if (arguments == nil) {
        arguments = [NSArray array];
    }

    // Set OSX special folders
    NSString * libraryDirectory = addDirectoryToSystemArguments(NSLibraryDirectory, NSUserDomainMask, @"LibraryDirectory", systemArguments);
    addDirectoryToSystemArguments(NSDocumentDirectory, NSUserDomainMask, @"DocumentsDirectory", systemArguments);
    addDirectoryToSystemArguments(NSApplicationSupportDirectory, NSUserDomainMask, @"ApplicationSupportDirectory", systemArguments);
    addDirectoryToSystemArguments(NSCachesDirectory, NSUserDomainMask, @"CachesDirectory", systemArguments);
    addDirectoryToSystemArguments(NSApplicationDirectory, NSUserDomainMask, @"ApplicationDirectory", systemArguments);
    addDirectoryToSystemArguments(NSAutosavedInformationDirectory, NSUserDomainMask, @"AutosavedInformationDirectory", systemArguments);
    addDirectoryToSystemArguments(NSDesktopDirectory, NSUserDomainMask, @"DesktopDirectory", systemArguments);
    addDirectoryToSystemArguments(NSDownloadsDirectory, NSUserDomainMask, @"DownloadsDirectory", systemArguments);
    addDirectoryToSystemArguments(NSMoviesDirectory, NSUserDomainMask, @"MoviesDirectory", systemArguments);
    addDirectoryToSystemArguments(NSMusicDirectory, NSUserDomainMask, @"MusicDirectory", systemArguments);
    addDirectoryToSystemArguments(NSPicturesDirectory, NSUserDomainMask, @"PicturesDirectory", systemArguments);
    addDirectoryToSystemArguments(NSSharedPublicDirectory, NSUserDomainMask, @"SharedPublicDirectory", systemArguments);

    addDirectoryToSystemArguments(NSLibraryDirectory, NSLocalDomainMask, @"SystemLibraryDirectory", systemArguments);
    addDirectoryToSystemArguments(NSApplicationSupportDirectory, NSLocalDomainMask, @"SystemApplicationSupportDirectory", systemArguments);
    addDirectoryToSystemArguments(NSCachesDirectory, NSLocalDomainMask, @"SystemCachesDirectory", systemArguments);
    addDirectoryToSystemArguments(NSApplicationDirectory, NSLocalDomainMask, @"SystemApplicationDirectory", systemArguments);
    addDirectoryToSystemArguments(NSUserDirectory, NSLocalDomainMask, @"SystemUserDirectory", systemArguments);

    // get the user's home directory, independent of the sandbox container
    int bufsize;
    if ((bufsize = sysconf(_SC_GETPW_R_SIZE_MAX)) != -1) {
        char buffer[bufsize];
        struct passwd pwd, *result = NULL;
        if (getpwuid_r(getuid(), &pwd, buffer, bufsize, &result) == 0 && result) {
            [systemArguments addObject:[NSString stringWithFormat:@"-DUserHome=%s", pwd.pw_dir]];
        }
    }

    //Sandbox
    NSString *containersDirectory = [libraryDirectory stringByAppendingPathComponent:@"Containers"];
    NSString *sandboxEnabled = @"false";
    BOOL containersDirExists = [fm fileExistsAtPath:containersDirectory isDirectory:&isDir];
    if (containersDirExists && isDir) {
        sandboxEnabled = @"true";
    }
    NSString *sandboxEnabledVar = [NSString stringWithFormat:@"-DSandboxEnabled=%@", sandboxEnabled];
    [systemArguments addObject:sandboxEnabledVar];


    // Check for modifier keys on app launch

    // Since [NSEvent modifierFlags] is only available since OS X 10.6., only add properties if supported.
    if ([NSEvent respondsToSelector:@selector(modifierFlags)]) {
        NSEventModifierFlags launchModifierFlags = [NSEvent modifierFlags] & NSEventModifierFlagDeviceIndependentFlagsMask;

        [systemArguments addObject:[NSString stringWithFormat:@"-DLaunchModifierFlags=%lu", (unsigned long)launchModifierFlags]];

        addModifierFlagToSystemArguments(NSEventModifierFlagCapsLock, @"LaunchModifierFlagCapsLock", launchModifierFlags, systemArguments);
        addModifierFlagToSystemArguments(NSEventModifierFlagShift, @"LaunchModifierFlagShift", launchModifierFlags, systemArguments);
        addModifierFlagToSystemArguments(NSEventModifierFlagControl, @"LaunchModifierFlagControl", launchModifierFlags, systemArguments);
        addModifierFlagToSystemArguments(NSEventModifierFlagOption, @"LaunchModifierFlagOption", launchModifierFlags, systemArguments);
        addModifierFlagToSystemArguments(NSEventModifierFlagCommand, @"LaunchModifierFlagCommand", launchModifierFlags, systemArguments);
        addModifierFlagToSystemArguments(NSEventModifierFlagNumericPad, @"LaunchModifierFlagNumericPad", launchModifierFlags, systemArguments);
        addModifierFlagToSystemArguments(NSEventModifierFlagHelp, @"LaunchModifierFlagHelp", launchModifierFlags, systemArguments);
        addModifierFlagToSystemArguments(NSEventModifierFlagFunction, @"LaunchModifierFlagFunction", launchModifierFlags, systemArguments);
    }


    // replace $APP_ROOT in environment variables
    NSDictionary* environment = [[NSProcessInfo processInfo] environment];
    for (NSString* key in environment) {
        NSString* value = [environment objectForKey:key];
        NSString* newValue = [value stringByReplacingOccurrencesOfString:@APP_ROOT_PREFIX withString:[mainBundle bundlePath]];
        if (! [newValue isEqualToString:value]) {
            setenv([key UTF8String], [newValue UTF8String], 1);
        }
    }

    // Initialize the arguments to JLI_Launch()
    // +5 due to the special directories and the sandbox enabled property
    int argc = 1 + [systemArguments count] + [options count] + [defaultOptions count] + 1 + [arguments count] + progargc;
    char *argv[argc];

    int i = 0;
    argv[i++] = commandName;
    for (NSString *systemArgument in systemArguments) {
        argv[i++] = strdup([systemArgument UTF8String]);
    }

    for (NSString *option in options) {
        option = [option stringByReplacingOccurrencesOfString:@APP_ROOT_PREFIX withString:[mainBundle bundlePath]];
        option = [option stringByReplacingOccurrencesOfString:@JVM_RUNTIME withString:runtimePath];
        argv[i++] = strdup([option UTF8String]);
    }

    for (NSString *defaultOption in defaultOptions) {
        defaultOption = [defaultOption stringByReplacingOccurrencesOfString:@APP_ROOT_PREFIX withString:[mainBundle bundlePath]];
        argv[i++] = strdup([defaultOption UTF8String]);
    }

    argv[i++] = strdup([mainClassName UTF8String]);

    for (NSString *argument in arguments) {
        argument = [argument stringByReplacingOccurrencesOfString:@APP_ROOT_PREFIX withString:[mainBundle bundlePath]];
        argv[i++] = strdup([argument UTF8String]);
    }

    int ctr = 0;
    for (ctr = 0; ctr < progargc; ctr++) {
        argv[i++] = progargv[ctr];
    }

    launchCount++;

    // Invoke JLI_Launch()
    return jli_LaunchFxnPtr(argc, argv,
                            0, NULL,
                            0, NULL,
                            "",
                            "",
                            "java",
                            "java",
                            FALSE,
                            FALSE,
                            FALSE,
                            0);
}

NSString * addDirectoryToSystemArguments(NSUInteger searchPath, NSSearchPathDomainMask domainMask, 
        NSString *systemProperty, NSMutableArray *systemArguments) {
    NSArray *paths = NSSearchPathForDirectoriesInDomains(searchPath,domainMask, YES);
    if ([paths count] > 0) {
        NSString *basePath = [paths objectAtIndex:0];
        NSString *directory = [NSString stringWithFormat:@"-D%@=%@", systemProperty, basePath];
        [systemArguments addObject:directory];
        return basePath;
    }
    return nil;
}

void addModifierFlagToSystemArguments(NSEventModifierFlags mask, NSString *systemProperty, NSEventModifierFlags modifierFlags, NSMutableArray *systemArguments) {
    NSString *modifierFlagValue = (modifierFlags & mask) ? @"true" : @"false";
    NSString *modifierFlagVar = [NSString stringWithFormat:@"-D%@=%@", systemProperty, modifierFlagValue];
    [systemArguments addObject:modifierFlagVar];
}
