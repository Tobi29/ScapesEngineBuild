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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

fun ScapesEngineApplicationExtension.generatePList() = AppPList(
        name = name.toString(),
        displayName = fullName.toString(),
        executableName = name.toString(),
        identifier = mainClass.toString(),
        shortVersion = version.toString(),
        mainClassName = mainClass.toString(),
        copyright = copyright.toString(),
        icon = "Icon.icns",
        runtime = "JRE.jre",
        workingDirectoryInLibrary = workingDirectoryInLibrary.resolveTo<Boolean?>() ?: false,
        applicationCategory = when (category.resolveTo<ApplicationType?>() ?: ApplicationType.UTILITY) {
            ApplicationType.DEVELOPMENT -> "public.app-category.developer-tools"
            ApplicationType.GAME -> "public.app-category.games"
            ApplicationType.GRAPHICS -> "public.app-category.graphics-design"
            ApplicationType.INTERNET -> "public.app-category.social-networking"
            ApplicationType.MULTIMEDIA -> "public.app-category.entertainment"
            ApplicationType.OFFICE -> "public.app-category.productivity"
            ApplicationType.UTILITY -> "public.app-category.utilities"
        },
        options = listOf(
                Option(value = "-XstartOnFirstThread"),
                Option(value = "-Xms64M"),
                Option(value = "-Xmx2048M"),
                Option(value = "-XX:+UseG1GC"),
                Option(value = "-XX:MaxGCPauseMillis=1"),
                Option(value = "-Xdock:icon=Contents/resources/Icon.icns"))
)

open class AppPListTask : DefaultTask() {
    @Input
    var plist: Ref<AppPList>? = null
    @Input
    var plistFile: Ref<File> = Ref { temporaryDir.resolve("Info.plist") }
    @Input
    var pkgFile: Ref<File> = Ref { temporaryDir.resolve("PkgInfo") }

    @OutputFile
    fun plistFile() = plistFile.invoke()

    @OutputFile
    fun pkgFile() = pkgFile.invoke()

    @TaskAction
    @Suppress("unused")
    fun run() {
        val plist = plist() ?: throw IllegalStateException("No plist given")
        val plistFile = plistFile()
        val pkgFile = pkgFile()
        plist.writeInfoPlist(plistFile)
        plist.writePkgInfo(pkgFile)
    }
}

open class JREPListTask : DefaultTask() {
    @Input
    var plist: Ref<JREPList>? = null
    @Input
    var plistFile: Ref<File> = Ref { temporaryDir.resolve("Info.plist") }

    @OutputFile
    fun plistFile() = plistFile.invoke()

    @TaskAction
    @Suppress("unused")
    fun run() {
        val plist = plist() ?: throw IllegalStateException("No plist given")
        val plistFile = plistFile()
        plist.writeInfoPlist(plistFile)
    }
}
