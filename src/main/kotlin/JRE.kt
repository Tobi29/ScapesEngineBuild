/*
 * Copyright 2012-2017 Tobi29
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Copy
import org.gradle.script.lang.kotlin.task
import java.io.File
import java.net.URL

fun Project.adoptOpenJDKMacOSX(version: Ref<String>) =
        adoptOpenJDKMacOSX(version,
                Ref {
                    adoptOpenJDKRelease("OpenJDK8", version(), "Mac", "x64")
                })

fun Project.adoptOpenJDKMacOSX(version: Ref<String>,
                               release: Ref<String>) =
        jdk("MacOSX", release, Ref { "./j2sdk-image" }, "./j2sdk-image",
                Ref { adoptOpenJDKURL(version(), release()) }, "tar.gz",
                { tarTree(it) }, { pruneJREMacOSX(it) })

fun adoptOpenJDKURL(version: String,
                    release: String) =
        URL("https://github.com/AdoptOpenJDK/openjdk-releases/releases/download/$version/$release.tar.gz")

fun adoptOpenJDKRelease(jdk: String,
                        version: String,
                        platform: String,
                        arch: String) =
        "${jdk}_${arch}_${platform}_$version"

fun adoptOpenJDKPList(version: String) = JREPList(
        name = "OpenJDK JRE 8",
        identifier = "com.github.AdoptOpenJDK.$version.jre",
        executableName = "libjli.dylib",
        shortVersion = "1.8.0",
        version = version,
        jvmMinimumFrameworkVersion = "13.2.9",
        jvmMinimumSystemVersion = "10.6.0",
        jvmPlatformVersion = "1.8",
        jvmVendor = "OpenJDK",
        jvmVersion = version)

fun Project.ojdkBuildWindows(version: Ref<Pair<String, String>>,
                             arch: String) =
        ojdkBuildWindows(Ref { version().first }, Ref {
            ojdkBuildRelease("java-1.8.0-openjdk", version().second, "windows",
                    when (arch) {
                        "32" -> "x86"
                        "64" -> "x86_64"
                        else -> throw IllegalArgumentException(
                                "Invalid architecture: $arch")
                    })
        }, arch)

fun Project.ojdkBuildWindows(version: Ref<String>,
                             release: Ref<String>,
                             arch: String) =
        jdk("Windows$arch", release, release, "*",
                Ref { ojdkBuildURL(version(), release()) }, "zip",
                { zipTree(it) }, { pruneJREWindows(it) })

fun ojdkBuildURL(version: String,
                 release: String) =
        URL("https://github.com/ojdkbuild/ojdkbuild/releases/download/$version/$release.zip")

fun ojdkBuildRelease(jdk: String,
                     version: String,
                     platform: String,
                     arch: String) =
        "$jdk-$version.ojdkbuild.$platform.$arch"

fun Project.jdk(platform: String,
                release: Ref<String>,
                root: Ref<String>,
                matchRoot: String,
                url: Ref<URL>,
                extension: String,
                unpackTree: Project.(File) -> FileTree,
                postProcess: CopySpec.(String) -> Unit): Pair<Task, Ref<File>> {
    val cacheDir = Ref { rootProject.file(".jreCache/$release") }
    val jdkFile = Ref { cacheDir().resolve("jdk.$extension") }
    val unpackDir = Ref { cacheDir().resolve("unpack") }
    val downloadTask = task<Download>("downloadJDK$platform")
    afterEvaluate {
        downloadTask.src(url.toClosure())
        downloadTask.dest(jdkFile.toClosure())
    }
    downloadTask.onlyIf { !jdkFile().exists() }
    val unpackTask = jdkUnpack("unpackJDK$platform",
            Ref { unpackTree(this@jdk, jdkFile()) }, matchRoot, unpackDir)
    unpackTask.postProcess("$matchRoot/jre")
    unpackTask.dependsOn(downloadTask)
    unpackTask.onlyIf { !unpackDir().exists() }
    return Pair(unpackTask, Ref { unpackDir().resolve("$root/jre") })
}

fun Project.jdkUnpack(name: String,
                      jreTar: Ref<FileTree>,
                      root: String,
                      unpackDir: Ref<File>) = task<Copy>(name) {
    from({ jreTar() }.toClosure())
    include("$root/jre/**")
    into({ unpackDir() }.toClosure())
}

fun CopySpec.pruneJREMacOSX(dir: String) {
    pruneJRE(dir)

    exclude("$dir/bin/**")

    exclude("$dir/lib/libdecora_sse.dylib")
    exclude("$dir/lib/libfxplugins.dylib")
    exclude("$dir/lib/libglass.dylib")
    exclude("$dir/lib/libglib-lite.dylib")
    exclude("$dir/lib/libgstreamer-lite.dylib")
    exclude("$dir/lib/libjavafx_font.dylib")
    exclude("$dir/lib/libjavafx_font_t2k.dylib")
    exclude("$dir/lib/libjavafx_iio.dylib")
    exclude("$dir/lib/libjfxmedia.dylib")
    exclude("$dir/lib/libjfxwebkit.dylib")
    exclude("$dir/lib/libprism_common.dylib")
    exclude("$dir/lib/libprism_es2.dylib")
    exclude("$dir/lib/libprism_sw.dylib")
}

fun CopySpec.pruneJREWindows(dir: String) {
    pruneJRE(dir)

    exclude("$dir/bin/dtplugin/**")
    exclude("$dir/bin/plugin2/**")
    exclude("$dir/bin/jabswitch.exe")
    exclude("$dir/bin/javacpl.cpl")
    exclude("$dir/bin/javacpl.exe")
    exclude("$dir/bin/javaws.exe")
    exclude("$dir/bin/jucheck.exe")
    exclude("$dir/bin/kinit.exe")
    exclude("$dir/bin/klist.exe")
    exclude("$dir/bin/ktab.exe")
    exclude("$dir/bin/orbd.exe")
    exclude("$dir/bin/policytool.exe")
    exclude("$dir/bin/keytool.exe")
    exclude("$dir/bin/rmid.exe")
    exclude("$dir/bin/rmiregistry.exe")
    exclude("$dir/bin/servertool.exe")
    exclude("$dir/bin/tnameserv.exe")

    exclude("$dir/lib/deploy/**")
    exclude("$dir/lib/deploy.jar")
    exclude("$dir/lib/plugin.jar")

    exclude("$dir/bin/java_crw_demo.dll")
    exclude("$dir/bin/JavaAccessBridge-32.dll")
    exclude("$dir/bin/JavaAccessBridge.dll")
    exclude("$dir/bin/JAWTAccessBridge-32.dll")
    exclude("$dir/bin/JAWTAccessBridge.dll")
    exclude("$dir/bin/WindowsAccessBridge-32.dll")
    exclude("$dir/bin/WindowsAccessBridge.dll")
    exclude("$dir/bin/wsdetect.dll")
    exclude("$dir/bin/deploy.dll")
    exclude("$dir/bin/jfr.dll")
    exclude("$dir/bin/decora_sse.dll")
    exclude("$dir/bin/fxplugins.dll")
    exclude("$dir/bin/glass.dll")
    exclude("$dir/bin/glib-lite.dll")
    exclude("$dir/bin/gstreamer-lite.dll")
    exclude("$dir/bin/javafx_font.dll")
    exclude("$dir/bin/javafx_font_t2k.dll")
    exclude("$dir/bin/javafx_iio.dll")
    exclude("$dir/bin/jfxmedia.dll")
    exclude("$dir/bin/jfxwebkit.dll")
    exclude("$dir/bin/prism_common.dll")
    exclude("$dir/bin/prism_d3d.dll")
    exclude("$dir/bin/prism_es2.dll")
    exclude("$dir/bin/prism_sw.dll")
}

fun CopySpec.pruneJRE(dir: String) {
    exclude("$dir/THIRDPARTYLICENSEREADME-JAVAFX.txt")

    exclude("$dir/plugin/**")

    exclude("$dir/lib/ext/access-bridge.jar")
    exclude("$dir/lib/ext/access-bridge-32.jar")
    exclude("$dir/lib/ext/access-bridge-64.jar")
    exclude("$dir/lib/ext/cldrdata.jar")
    exclude("$dir/lib/ext/jfxrt.jar")
    exclude("$dir/lib/ext/localedata.jar")
    exclude("$dir/lib/ext/nashorn.jar")
    exclude("$dir/lib/ext/sunmscapi.jar")

    exclude("$dir/lib/desktop/**")
    exclude("$dir/lib/jfr/**")
    exclude("$dir/lib/oblique-fonts/**")
    exclude("$dir/lib/ant-javafx.jar")
    exclude("$dir/lib/javafx.properties")
    exclude("$dir/lib/javaws.jar")
    exclude("$dir/lib/jfr.jar")
    exclude("$dir/lib/jfxswt.jar")
}
