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

import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.Sync
import java.io.File
import java.io.FileFilter

fun Project.jreTask(name: String,
                    platform: String): Sync? {
    val jre = getJRE(platform) ?: return null
    val task = tasks.create<Sync>(name, Sync::class.java)
    task.from({ tarTree(jre) }.toClosure())
    task.into({ "${task.temporaryDir}" }.toClosure())
    task.includeEmptyDirs = false
    task.eachFile { fcp: FileCopyDetails ->
        val segments = fcp.relativePath.segments
        val newSegments = if (segments.size <= 1) {
            emptyArray<String>()
        } else {
            segments.sliceArray(1..segments.lastIndex)
        }
        fcp.relativePath = RelativePath(true, *newSegments)
    }
    return task
}

fun Project.getJRE(platform: String): File? {
    val jres = rootProject.file(
            "buildSrc/resources/JRE/$platform").listFiles(
            FileFilter { !it.isDirectory && !it.isHidden })
    if (jres == null || jres.isEmpty()) {
        return null
    }
    return jres[0]
}

fun pruneJREMacOSX(copy: CopySpec,
                   dir: String) {
    pruneJRE(copy, dir)

    copy.exclude("$dir/bin/javaws")
    copy.exclude("$dir/bin/orbd")
    copy.exclude("$dir/bin/policytool")
    copy.exclude("$dir/bin/keytool")
    copy.exclude("$dir/bin/rmid")
    copy.exclude("$dir/bin/rmiregistry")
    copy.exclude("$dir/bin/servertool")
    copy.exclude("$dir/bin/tnameserv")

    copy.exclude("$dir/lib/libdecora_sse.dylib")
    copy.exclude("$dir/lib/libfxplugins.dylib")
    copy.exclude("$dir/lib/libglass.dylib")
    copy.exclude("$dir/lib/libglib-lite.dylib")
    copy.exclude("$dir/lib/libgstreamer-lite.dylib")
    copy.exclude("$dir/lib/libjavafx_font.dylib")
    copy.exclude("$dir/lib/libjavafx_font_t2k.dylib")
    copy.exclude("$dir/lib/libjavafx_iio.dylib")
    copy.exclude("$dir/lib/libjfxmedia.dylib")
    copy.exclude("$dir/lib/libjfxwebkit.dylib")
    copy.exclude("$dir/lib/libprism_common.dylib")
    copy.exclude("$dir/lib/libprism_es2.dylib")
    copy.exclude("$dir/lib/libprism_sw.dylib")
}

fun pruneJREWindows(copy: CopySpec,
                    dir: String) {
    pruneJRE(copy, dir)

    copy.exclude("$dir/bin/dtplugin/**")
    copy.exclude("$dir/bin/plugin2/**")
    copy.exclude("$dir/bin/jabswitch.exe")
    copy.exclude("$dir/bin/javacpl.cpl")
    copy.exclude("$dir/bin/javacpl.exe")
    copy.exclude("$dir/bin/javaws.exe")
    copy.exclude("$dir/bin/jucheck.exe")
    copy.exclude("$dir/bin/kinit.exe")
    copy.exclude("$dir/bin/klist.exe")
    copy.exclude("$dir/bin/ktab.exe")
    copy.exclude("$dir/bin/orbd.exe")
    copy.exclude("$dir/bin/policytool.exe")
    copy.exclude("$dir/bin/keytool.exe")
    copy.exclude("$dir/bin/rmid.exe")
    copy.exclude("$dir/bin/rmiregistry.exe")
    copy.exclude("$dir/bin/servertool.exe")
    copy.exclude("$dir/bin/tnameserv.exe")

    copy.exclude("$dir/lib/deploy/**")
    copy.exclude("$dir/lib/deploy.jar")
    copy.exclude("$dir/lib/plugin.jar")

    copy.exclude("$dir/bin/java_crw_demo.dll")
    copy.exclude("$dir/bin/JavaAccessBridge-32.dll")
    copy.exclude("$dir/bin/JavaAccessBridge.dll")
    copy.exclude("$dir/bin/JAWTAccessBridge-32.dll")
    copy.exclude("$dir/bin/JAWTAccessBridge.dll")
    copy.exclude("$dir/bin/WindowsAccessBridge-32.dll")
    copy.exclude("$dir/bin/WindowsAccessBridge.dll")
    copy.exclude("$dir/bin/wsdetect.dll")
    copy.exclude("$dir/bin/deploy.dll")
    copy.exclude("$dir/bin/jfr.dll")
    copy.exclude("$dir/bin/decora_sse.dll")
    copy.exclude("$dir/bin/fxplugins.dll")
    copy.exclude("$dir/bin/glass.dll")
    copy.exclude("$dir/bin/glib-lite.dll")
    copy.exclude("$dir/bin/gstreamer-lite.dll")
    copy.exclude("$dir/bin/javafx_font.dll")
    copy.exclude("$dir/bin/javafx_font_t2k.dll")
    copy.exclude("$dir/bin/javafx_iio.dll")
    copy.exclude("$dir/bin/jfxmedia.dll")
    copy.exclude("$dir/bin/jfxwebkit.dll")
    copy.exclude("$dir/bin/prism_common.dll")
    copy.exclude("$dir/bin/prism_d3d.dll")
    copy.exclude("$dir/bin/prism_es2.dll")
    copy.exclude("$dir/bin/prism_sw.dll")
}

fun pruneJRE(copy: CopySpec,
             dir: String) {
    copy.exclude("$dir/THIRDPARTYLICENSEREADME-JAVAFX.txt")

    copy.exclude("$dir/plugin/**")

    copy.exclude("$dir/lib/ext/access-bridge.jar")
    copy.exclude("$dir/lib/ext/access-bridge-32.jar")
    copy.exclude("$dir/lib/ext/access-bridge-64.jar")
    copy.exclude("$dir/lib/ext/cldrdata.jar")
    copy.exclude("$dir/lib/ext/jfxrt.jar")
    copy.exclude("$dir/lib/ext/localedata.jar")
    copy.exclude("$dir/lib/ext/nashorn.jar")
    copy.exclude("$dir/lib/ext/sunmscapi.jar")

    copy.exclude("$dir/lib/desktop/**")
    copy.exclude("$dir/lib/jfr/**")
    copy.exclude("$dir/lib/oblique-fonts/**")
    copy.exclude("$dir/lib/ant-javafx.jar")
    copy.exclude("$dir/lib/javafx.properties")
    copy.exclude("$dir/lib/javaws.jar")
    copy.exclude("$dir/lib/jfr.jar")
    copy.exclude("$dir/lib/jfxswt.jar")
}
