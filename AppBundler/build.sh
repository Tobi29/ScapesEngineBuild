source build.properties
echo "Compiling with: $cc"
echo "Using java: $javaHome"
echo "Using sdk: $osxSDK"

"$cc" \
"-I" "$javaHome/include" \
"-I" "$javaHome/include/darwin" \
"-o" "JavaAppLauncher" \
"-DLIBJLI_DYLIB=\"$javaHome/jre/lib/jli/libjli.dylib\"" \
"-framework" "Cocoa" \
"-F" "$javaHome/../.." \
"-isysroot" "$osxSDK" \
"-mmacosx-version-min=10.7" \
"-fobjc-exceptions" \
"-std=c99" \
"main.m"
