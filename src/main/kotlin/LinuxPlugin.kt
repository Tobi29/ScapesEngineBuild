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
import java.io.File

open class ScapesEngineApplicationLinux : Plugin<Project> {
    override fun apply(target: Project) {
        val config = target.extensions.getByType(
                ScapesEngineApplicationExtension::class.java)

        // Platform deploy tasks
        val deployLinuxTask32 = target.addDeployLinuxTask("32", Ref {
            target.configurations.getByName("runtime") + target.files(
                    target.tasks.getByName(
                            "jar")) + target.configurations.getByName(
                    "runtimeLinux32")
        }, Ref {
            target.configurations.getByName("nativesLinux32")
        }, config)
        val deployLinuxTask64 = target.addDeployLinuxTask("64", Ref {
            target.configurations.getByName("runtime") + target.files(
                    target.tasks.getByName(
                            "jar")) + target.configurations.getByName(
                    "runtimeLinux64")
        }, Ref {
            target.configurations.getByName("nativesLinux64")
        }, config)

        // Fat jar tasks
        val fatJarLinux32Task = target.addShadowTask("Linux32",
                "fatJarLinux32")
        val fatJarLinux64Task = target.addShadowTask("Linux64",
                "fatJarLinux64")

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

fun Project.addDeployLinuxTask(arch: String,
                               jars: Ref<FileCollection>,
                               natives: Ref<FileCollection>,
                               config: ScapesEngineApplicationExtension): Task? {
    val libPath = if (rootProject.hasProperty("libPath")) {
        File(rootProject.property("libPath").toString())
    } else {
        File("/usr/share/java")
    }
    val binPath = if (rootProject.hasProperty("binPath")) {
        File(rootProject.property("binPath").toString())
    } else {
        File("/usr/bin")
    }
    val lowerName = Ref { config.name.resolveToString().toLowerCase() }
    val libDir = Ref { File(libPath, lowerName()) }

    // Script task
    val scriptTask = linuxScriptTask(libDir, config,
            "scriptLinux$arch")

    // Main task
    val task = linuxTarTask(libDir, Ref { binPath }, "Linux$arch", jars,
            natives,
            Ref { scriptTask.output() }, Ref { config.name.resolveToString() },
            "deployLinux$arch")
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

fun Project.linuxScriptTask(libPath: Ref<File>,
                            config: ScapesEngineApplicationExtension,
                            taskName: String): StartupScriptTask {
    return linuxScriptTask(libPath,
            Ref { config.name.resolveToString().toLowerCase() },
            Ref { config.mainClass.resolveToString() }, taskName)
}

fun Project.linuxScriptTask(libPath: Ref<File>,
                            execName: Ref<String>,
                            mainClass: Ref<String>,
                            taskName: String): StartupScriptTask {
    val task = tasks.create(taskName, StartupScriptTask::class.java)
    task.execName = execName
    task.libPath = libPath
    task.mainClass = mainClass
    task.output = Ref { File(task.temporaryDir, execName()) }
    return task
}

fun Project.linuxTarTask(libPath: Ref<File>,
                         binPath: Ref<File>,
                         distributionName: String,
                         jars: Ref<FileCollection>,
                         natives: Ref<FileCollection>,
                         script: Ref<File>,
                         name: Ref<String>,
                         taskName: String): Tar {
    val task = linuxTarTask(libPath, distributionName, jars, natives, name,
            taskName)
    task.from(script.toClosure()) { it.into(binPath.toClosure()) }
    return task
}

fun Project.linuxTarTask(libPath: Ref<File>,
                         distributionName: String,
                         jars: Ref<FileCollection>,
                         natives: Ref<FileCollection>,
                         name: Ref<String>,
                         taskName: String): Tar {
    val task = tasks.create(taskName, Tar::class.java)
    afterEvaluate {
        task.baseName = "$name-$distributionName"
    }
    task.compression = Compression.GZIP
    task.from(jars.toClosure()) {
        it.into(libPath.toClosure())
    }
    task.from({
        natives().asSequence().map<File, Any> {
            if (it.name.endsWith(".jar")) {
                zipTree(it).files.asSequence().filter {
                    it.isFile && it.name.matches(soRegex)
                }.toList()
            } else {
                it
            }
        }.toList()
    }.toClosure()) {
        it.eachFile { fcp: FileCopyDetails ->
            fcp.relativePath = RelativePath(true, libPath.toString(), fcp.name)
            fcp.mode = 493 // 755
        }
    }
    return task
}

private val soRegex = "(.+)\\.so(.[0-9]+)?".toRegex()
