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

package org.tobi29.scapes.engine.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.tobi29.scapes.engine.gradle.dsl.ScapesEngineApplicationExtension
import org.tobi29.scapes.engine.gradle.task.StartupScriptTask
import java.io.File

open class ScapesEngineApplicationLinux : Plugin<Project> {
    override fun apply(target: Project) {
        val config = target.extensions.getByType(
            ScapesEngineApplicationExtension::class.java
        )

        // Platform deploy tasks
        val deployLinuxTask32 = target.addDeployLinuxTask("32",
            target.providers.provider {
                target.configurations.getByName("runtime") + target.files(
                    target.tasks.getByName(
                        "jar"
                    )
                ) + target.configurations.getByName(
                    "runtimeLinux32"
                )
            }, target.providers.provider {
                target.configurations.getByName("nativesLinux32")
            }, config
        )
        val deployLinuxTask64 = target.addDeployLinuxTask("64",
            target.providers.provider {
                target.configurations.getByName("runtime") + target.files(
                    target.tasks.getByName(
                        "jar"
                    )
                ) + target.configurations.getByName(
                    "runtimeLinux64"
                )
            }, target.providers.provider {
                target.configurations.getByName("nativesLinux64")
            }, config
        )

        // Fat jar tasks
        val fatJarLinux32Task = target.addShadowTask(
            "Linux32",
            "fatJarLinux32"
        )
        val fatJarLinux64Task = target.addShadowTask(
            "Linux64",
            "fatJarLinux64"
        )

        // Full deploy task
        target.tasks.findByName("deploy")?.let { deployTask ->
            if (deployLinuxTask32 != null) {
                deployTask.dependsOn(deployLinuxTask32)
            }
            if (deployLinuxTask64 != null) {
                deployTask.dependsOn(deployLinuxTask64)
            }
        }
        target.tasks.findByName("fatJar")?.let { fatJarTask ->
            fatJarTask.dependsOn(fatJarLinux32Task)
            fatJarTask.dependsOn(fatJarLinux64Task)
        }
    }
}

fun Project.addDeployLinuxTask(
    arch: String,
    jars: Provider<FileCollection>,
    natives: Provider<FileCollection>,
    config: ScapesEngineApplicationExtension
): Task? {
    val libPath = if (rootProject.hasProperty("libPath")) {
        rootProject.property("libPath").toString()
    } else {
        "/usr/share/java"
    }
    val binPath = if (rootProject.hasProperty("binPath")) {
        rootProject.property("binPath").toString()
    } else {
        "/usr/bin"
    }
    val libDir = config.nameProvider.map { "$libPath/${it.toLowerCase()}" }

    // Script task
    val scriptTask = linuxScriptTask(
        libDir, config,
        "scriptLinux$arch"
    )

    // Main task
    val task = linuxTarTask(
        libDir,
        providers.provider(binPath), "Linux$arch", jars,
        natives,
        scriptTask.outputProvider,
        config.nameProvider,
        "deployLinux$arch"
    )
    task.description =
            "Contains tarball that can be extracted into root for easier package creation"
    task.group = "Deployment"
    task.dependsOn(scriptTask)
    task.dependsOn("jar")
    afterEvaluate {
        task.baseName = "${config.name}-Linux$arch"
    }
    return task
}

fun Project.linuxScriptTask(
    libPath: Provider<String>,
    config: ScapesEngineApplicationExtension,
    taskName: String
): StartupScriptTask {
    return linuxScriptTask(
        libPath,
        config.nameProvider.map { it.toLowerCase() },
        config.mainClassProvider, taskName,
        config.workingDirectoryInLibraryProvider
    )
}

fun Project.linuxScriptTask(
    libPath: Provider<String>,
    execName: Provider<String>,
    mainClass: Provider<String>,
    taskName: String,
    workingDirInLibrary: Provider<Boolean>
): StartupScriptTask {
    val task = tasks.create(taskName, StartupScriptTask::class.java)
    task.execNameProvider.set(execName)
    task.libPathProvider.set(libPath)
    task.mainClassProvider.set(mainClass)
    task.workingDirInLibraryProvider.set(workingDirInLibrary)
    task.outputProvider.set(execName.map { File(task.temporaryDir, it) })
    return task
}

fun Project.linuxTarTask(
    libPath: Provider<String>,
    binPath: Provider<String>,
    distributionName: String,
    jars: Provider<FileCollection>,
    natives: Provider<FileCollection>,
    script: Provider<File>,
    name: Provider<String>,
    taskName: String
): Tar {
    val task = linuxTarTask(
        libPath, distributionName, jars, natives, name,
        taskName
    )
    task.from(script.toClosure()) { it.into(binPath.toClosure()) }
    return task
}

fun Project.linuxTarTask(
    libPath: Provider<String>,
    distributionName: String,
    jars: Provider<FileCollection>,
    natives: Provider<FileCollection>,
    name: Provider<String>,
    taskName: String
): Tar {
    val task = tasks.create(taskName, Tar::class.java)
    task.compression = Compression.GZIP
    task.from(jars.toClosure()) {
        it.into(libPath.toClosure())
    }
    task.from(natives.map { fetchNativesLinux(it) }.toClosure()) {
        it.eachFile { fcp: FileCopyDetails ->
            fcp.relativePath = RelativePath(
                true,
                libPath.get().toString(), fcp.name
            )
            fcp.mode = 493 // 755
        }
    }
    return task
}
