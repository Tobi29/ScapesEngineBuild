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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.gradle.script.lang.kotlin.task

open class ScapesEngineApplicationMacOSX : Plugin<Project> {
    @Suppress("ReplaceSingleLineLet")
    override fun apply(target: Project) {
        val config = target.extensions.getByType(
                ScapesEngineApplicationExtension::class.java)

        // Platform deploy task
        val deployMacOSXTask = target.addDeployMacOSXTask(Ref {
            target.allJars("MacOSX")
        }, Ref {
            target.configurations.getByName("nativesMacOSX")
        }, config)

        // Fat jar task
        val fatJarMacOSXTask = target.addShadowTask("MacOSX",
                "fatJarMacOSX")

        // Full deploy task
        target.tasks.findByName("deploy")?.let { deployTask ->
            if (deployMacOSXTask != null) {
                deployTask.dependsOn(deployMacOSXTask)
            }
        }
        target.tasks.findByName("fatJar")?.let { fatJarTask ->
            fatJarTask.dependsOn(fatJarMacOSXTask)
        }
    }
}

fun Project.addDeployMacOSXTask(jars: Ref<FileCollection>,
                                natives: Ref<FileCollection>,
                                application: ScapesEngineApplicationExtension): Task? {
    val adoptOpenJDKVersion = Ref {
        application.adoptOpenJDKVersion.resolveTo<String?>()
                ?: throw IllegalStateException("No usable AdoptOpenJDK version")
    }

    // JRE task
    val (jreTask, jre) = adoptOpenJDKMacOSX(adoptOpenJDKVersion)

    // App plist task
    val appPListTask = task<AppPListTask>("appPListMacOSX") {
        plist = Ref { application.generatePList() }
    }

    // JRE plist task
    val jrePListTask = task<JREPListTask>("jrePListMacOSX") {
        plist = Ref { adoptOpenJDKPList(adoptOpenJDKVersion()) }
    }


    // Main task
    val task = task<Tar>("deployMacOSX") {
        compression = Compression.GZIP
        val bundle = Ref { "${application.fullName}.app" }
        val contents = Ref { "$bundle/Contents" }
        val javaDir = Ref { "$contents/Java" }
        val macOSDir = Ref { "$contents/MacOS" }
        val plugInsDir = Ref { "$contents/PlugIns" }
        val resourcesDir = Ref { "$contents/Resources" }

        from(jars.toClosure()) { it.into(javaDir.toClosure()) }
        from(jre.toClosure()) {
            it.into({ "$plugInsDir/JRE.jre/Contents/Home" }.toClosure())
        }
        // There seems to be no way to add symlinks to tars with gradle
        // So we just copy the library (It is only 72 KiB)
        from(jre.toClosure()) {
            it.include("lib/jli/libjli.dylib")
            it.eachFile { fcp: FileCopyDetails ->
                fcp.relativePath = RelativePath(true, plugInsDir(), "JRE.jre",
                        "Contents", "MacOS", "libjli.dylib")
                fcp.mode = 493 // 755
            }
            it.includeEmptyDirs = false
        }
        from(jrePListTask.plistFile()) {
            it.into({ "$plugInsDir/JRE.jre/Contents" }.toClosure())
        }
        from({ fetchNativesMacOSX(natives()) }.toClosure()) {
            it.eachFile { fcp: FileCopyDetails ->
                fcp.relativePath = RelativePath(true, macOSDir(), fcp.name)
                fcp.mode = 493 // 755
            }
            it.includeEmptyDirs = false
        }
        from({
            rootProject.files("buildSrc/resources/AppBundler/JavaAppLauncher")
        }.toClosure()) {
            it.eachFile { fcp: FileCopyDetails ->
                fcp.relativePath = RelativePath(true, macOSDir(),
                        application.name.toString())
                fcp.mode = 493 // 755
            }
            it.includeEmptyDirs = false
        }
        from({
            files("project/Icon.icns") +
                    rootProject.files("buildSrc/resources/AppBundler/Resources")
        }.toClosure()) {
            it.into(resourcesDir.toClosure())
        }
        from({
            files(appPListTask.plistFile(), appPListTask.pkgFile())
        }.toClosure()) {
            it.into(contents.toClosure())
        }
    }
    task.description =
            "Mac OS X Application containing necessary files to run the game"
    task.group = "Deployment"
    task.dependsOn(jreTask)
    task.dependsOn(appPListTask)
    task.dependsOn(jrePListTask)
    task.dependsOn("jar")
    return task
}
