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
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import java.io.File

open class ScapesEngineApplicationMacOSX : Plugin<Project> {
    override fun apply(target: Project) {
        val config = target.extensions.getByType(
                ScapesEngineApplicationExtension::class.java)

        // Platform deploy task
        val deployMacOSXTask = target.addDeployMacOSXTask(Ref {
            target.allJars("MacOSX")
        }, Ref {
            target.configurations.getByName("nativesMacOSX")
        }, config)

        // Full deploy task
        val deployTask = target.tasks.getByName("deploy")
        if (deployMacOSXTask != null) {
            deployTask.dependsOn(deployMacOSXTask)
        }
    }
}

fun Project.addDeployMacOSXTask(jars: Ref<FileCollection>,
                                natives: Ref<FileCollection>,
                                application: ScapesEngineApplicationExtension): Task? {
    // JRE Task
    val jreTask = jreTask("jreMacOSX", "MacOSX")
    if (jreTask == null) {
        logger.warn("No JRE for Mac OS X found!")
        return null
    }
    pruneJREMacOSX(jreTask, "*/Contents/Home")

    // Bundle task
    val bundleTask = macOSXBundleTask(jars, Ref { jreTask.temporaryDir },
            application, "bundleMacOSX")
    bundleTask.dependsOn(jreTask)
    bundleTask.dependsOn("jar")

    // Natives task
    val nativesTask = macOSXNativesTask(natives, Ref { bundleTask.output() },
            "nativesMacOSX")
    nativesTask.dependsOn(bundleTask)

    // Main task
    val task = macOSXTarTask("MacOSX", Ref { bundleTask.output().parentFile },
            application, "deployMacOSX")
    task.description =
            "Mac OS X Application containing necessary files to run the game"
    task.group = "Deployment"
    task.dependsOn(nativesTask)
    return task
}

fun Project.macOSXBundleTask(jars: Ref<FileCollection>,
                             jre: Ref<File>,
                             config: ScapesEngineApplicationExtension,
                             taskName: String): AppBundlerTask {
    val task = tasks.create(taskName, AppBundlerTask::class.java)
    task.fullName = Ref { config.fullName.resolveToString() }
    task.version = Ref { config.version.resolveToString() }
    task.copyright = Ref { config.copyright.resolveToString() }
    task.mainClass = Ref { config.mainClass.resolveToString() }
    task.appbundler = Ref {
        rootProject.file("ScapesEngine/resources/appbundler-1.0ea.jar")
    }
    task.jre = jre
    task.icon = Ref { file("project/Icon.icns") }
    task.classpath = jars
    task.output = Ref { File(task.temporaryDir, "${config.fullName}.app") }
    return task
}

fun Project.macOSXNativesTask(natives: Ref<FileCollection>,
                              bundle: Ref<File>,
                              taskName: String): Copy {
    val task = tasks.create(taskName, Copy::class.java)
    task.from({
        natives().asSequence().map<File, Any> {
            if (it.name.endsWith(".jar")) {
                zipTree(it).files.asSequence().filter {
                    it.isFile && it.name.matches(libRegex)
                }.toList()
            } else {
                it
            }
        }.toList()
    }.toClosure()) {
        it.eachFile { fcp: FileCopyDetails ->
            fcp.relativePath = RelativePath(true, fcp.name)
            fcp.mode = 493 // 755
        }
    }
    task.into({ File(bundle(), "Contents/MacOS") }.toClosure())
    return task
}

fun Project.macOSXTarTask(distributionName: String,
                          dir: Ref<File>,
                          config: ScapesEngineApplicationExtension,
                          taskName: String): Tar {
    val task = tasks.create(taskName, Tar::class.java)
    afterEvaluate {
        task.baseName = "${config.name}-$distributionName"
    }
    task.compression = Compression.GZIP
    task.from(dir.toClosure())
    return task
}

private val libRegex = "(.+)\\.(jnilib|dylib)".toRegex()