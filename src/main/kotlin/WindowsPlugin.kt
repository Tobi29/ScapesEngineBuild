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
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Zip
import java.io.File
import java.util.*

open class ScapesEngineApplicationWindows : Plugin<Project> {
    override fun apply(target: Project) {
        val config = target.extensions.getByType(
                ScapesEngineApplicationExtension::class.java)

        // Platform deploy task
        val deployWindowsTasks = target.addDeployWindowsTasks(Ref {
            target.allCommonJars()
        }, Ref {
            target.configurations.getByName("runtimeWindows32")
        }, Ref {
            target.configurations.getByName("runtimeWindows64")
        }, Ref {
            target.configurations.getByName("nativesWindows32")
        }, Ref {
            target.configurations.getByName("nativesWindows64")
        }, config)

        // Fat jar tasks
        val fatJarWindows32Task = target.addShadowTask("Windows32",
                "fatJarWindows32")
        val fatJarWindows64Task = target.addShadowTask("Windows64",
                "fatJarWindows64")

        // Full deploy task
        target.tasks.findByName("deploy")?.let { deployTask ->
            deployWindowsTasks.forEach { deployTask.dependsOn(it) }
        }
        target.tasks.findByName("fatJar")?.let { fatJarTask ->
            fatJarTask.dependsOn(fatJarWindows32Task)
            fatJarTask.dependsOn(fatJarWindows64Task)
        }
    }
}

fun Project.addDeployWindowsTasks(jars: Ref<FileCollection>,
                                  jars32: Ref<FileCollection>,
                                  jars64: Ref<FileCollection>,
                                  natives32: Ref<FileCollection>,
                                  natives64: Ref<FileCollection>,
                                  config: ScapesEngineApplicationExtension): List<Task> {
    val deployTasks = ArrayList<Task>()

    // JRE Task 64-Bit
    val jreTask32 = jreTask("jreWindows32", "Windows/32")
    if (jreTask32 == null) {
        logger.warn("No 32-Bit JRE for Windows found!")
    } else {
        pruneJREWindows(jreTask32, "*")
    }

    // JRE Task 64-Bit
    val jreTask64 = jreTask("jreWindows64", "Windows/64")
    if (jreTask64 == null) {
        logger.warn("No 64-Bit JRE for Windows found!")
    } else {
        pruneJREWindows(jreTask64, "*")
    }

    // Program task
    val programTask = windowsProgramTask(false, Ref { "${config.name}.exe" },
            config, "programWindows")

    // Command task
    val programCmdTask = windowsProgramTask(true,
            Ref { "${config.name}Cmd.exe" }, config, "programWindowsCmd")

    // Zip tasks
    if (jreTask32 != null) {
        val deployWindowsZip32 = windowsZipTask("Windows32",
                Ref { jars() + jars32() },
                natives32,
                Ref { files(programTask.output(), programCmdTask.output()) },
                Ref { jreTask32.temporaryDir }, "deployWindowsZip32")
        deployWindowsZip32.dependsOn(jreTask32)
        deployWindowsZip32.dependsOn("jar")
        deployWindowsZip32.dependsOn(programTask)
        deployWindowsZip32.dependsOn(programCmdTask)
        deployWindowsZip32.group = "Deployment"
        deployWindowsZip32.description = "Zip for Windows with bundled JRE"
        afterEvaluate {
            deployWindowsZip32.baseName = "${config.name}-Windows32"
        }
        deployTasks.add(deployWindowsZip32)
    }
    if (jreTask64 != null) {
        val deployWindowsZip64 = windowsZipTask("Windows64",
                Ref { jars() + jars64() },
                natives64,
                Ref { files(programTask.output(), programCmdTask.output()) },
                Ref { jreTask64.temporaryDir }, "deployWindowsZip64")
        deployWindowsZip64.dependsOn(jreTask64)
        deployWindowsZip64.dependsOn("jar")
        deployWindowsZip64.dependsOn(programTask)
        deployWindowsZip64.dependsOn(programCmdTask)
        deployWindowsZip64.group = "Deployment"
        deployWindowsZip64.description = "Zip for Windows with bundled JRE"
        afterEvaluate {
            deployWindowsZip64.baseName = "${config.name}-Windows64"
        }
        deployTasks.add(deployWindowsZip64)
    }

    if (jreTask32 != null && jreTask64 != null) {
        // Prepare task
        val prepareTask = windowsPrepareTask(jars, jars32, jars64, natives32,
                natives64, Ref { jreTask32.temporaryDir },
                Ref { jreTask64.temporaryDir }, "prepareWindows")
        prepareTask.dependsOn(jreTask32)
        prepareTask.dependsOn(jreTask64)
        prepareTask.dependsOn("jar")
        prepareTask.dependsOn(programTask)
        prepareTask.dependsOn(programCmdTask)
        prepareTask.from({ programTask.output() }.toClosure()) {
            it.into("install/common")
        }
        prepareTask.from({ programCmdTask.output() }.toClosure()) {
            it.into("install/common")
        }

        val innoEXE = rootProject.file(
                "buildSrc/resources/Inno Setup 5/ISCC.exe")
        if (!innoEXE.exists()) {
            logger.warn("No Inno Setup for Windows found!")
            return deployTasks
        }

        // Pack task
        val packTask = windowsPackTask(Ref { prepareTask.temporaryDir },
                innoEXE, config, "packWindows")
        packTask.dependsOn(prepareTask)

        // Main task
        val task = tasks.create("deployWindows", Copy::class.java)
        task.dependsOn(packTask)
        task.group = "Deployment"
        task.description = "Windows Installer with bundled JRE"
        task.from(File(prepareTask.temporaryDir, "output/setup.exe"))
        task.rename({ str: String ->
            "${config.name}-Setup-${project.version}.exe"
        }.toClosure())
        task.into(File(buildDir, "distributions"))
        deployTasks.add(task)
    }
    return deployTasks
}

fun Project.windowsProgramTask(cmd: Boolean,
                               exeName: Ref<String>,
                               config: ScapesEngineApplicationExtension,
                               taskName: String): Launch4jTask {
    val task = tasks.create(taskName, Launch4jTask::class.java)
    task.fullName = Ref { "${config.fullName}" }
    task.version = Ref { "${config.version}" }
    task.company = Ref { "${config.company}" }
    task.copyright = Ref { "${config.copyright}" }
    task.mainClass = Ref { "${config.mainClass}" }
    task.launch4j = Ref {
        file("$rootDir/buildSrc/resources/Launch4j/launch4j.jar")
    }
    task.icon = Ref { file("project/Icon.ico") }
    task.exeMemoryMin = Ref { 64 }
    task.exeMemoryMax = Ref { 2048 }
    task.exeType = Ref { if (cmd) "console" else "gui" }
    task.runInAppData = Ref { !cmd }
    task.manifest = Ref {
        file("$rootDir/buildSrc/resources/Program.manifest")
    }
    task.output = Ref { File(task.temporaryDir, exeName()) }
    return task
}

fun Project.windowsPrepareTask(jars: Ref<FileCollection>,
                               jars32: Ref<FileCollection>,
                               jars64: Ref<FileCollection>,
                               natives32: Ref<FileCollection>,
                               natives64: Ref<FileCollection>,
                               jre32: Ref<File>,
                               jre64: Ref<File>,
                               taskName: String): Copy {
    val task = tasks.create(taskName, Copy::class.java)
    task.from(rootProject.file("buildSrc/resources/Setup.iss"))
    task.from(file("project/installer"))
    task.from(jars.toClosure()) {
        it.into("install/common/lib")
    }
    task.from(jars32.toClosure()) {
        it.into("install/32/lib")
    }
    task.from(jars64.toClosure()) {
        it.into("install/64/lib")
    }
    task.from({
        natives32().asSequence().map<File, Any> {
            if (it.name.endsWith(".jar")) {
                zipTree(it).files.asSequence().filter {
                    it.isFile && it.name.matches(dllRegex)
                }.toList()
            } else {
                it
            }
        }.toList()
    }.toClosure()) {
        it.eachFile { fcp: FileCopyDetails ->
            fcp.relativePath = RelativePath(true, "install", "32", fcp.name)
            fcp.mode = 493 // 755
        }
    }
    task.from({
        natives64().asSequence().map<File, Any> {
            if (it.name.endsWith(".jar")) {
                zipTree(it).files.asSequence().filter {
                    it.isFile && it.name.matches(dllRegex)
                }.toList()
            } else {
                it
            }
        }.toList()
    }.toClosure()) {
        it.eachFile { fcp: FileCopyDetails ->
            fcp.relativePath = RelativePath(true, "install", "64", fcp.name)
            fcp.mode = 493 // 755
        }
    }
    task.from(jre32.toClosure()) {
        it.into("install/32/jre")
    }
    task.from(jre64.toClosure()) {
        it.into("install/64/jre")
    }
    task.from(rootProject.file(
            "buildSrc/resources/Install/Windows")) { it.into("install") }
    task.into(task.temporaryDir)
    return task
}

fun Project.windowsZipTask(distributionName: String,
                           jars: Ref<FileCollection>,
                           natives: Ref<FileCollection>,
                           exes: Ref<FileCollection>,
                           jre: Ref<File>,
                           taskName: String): Zip {
    val task = tasks.create(taskName, Zip::class.java)
    task.from(jars.toClosure()) { it.into("lib") }
    task.from({
        natives().asSequence().map<File, Any> {
            if (it.name.endsWith(".jar")) {
                zipTree(it).files.asSequence().filter {
                    it.isFile && it.name.matches(dllRegex)
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
    task.from(exes.toClosure())
    task.from(jre.toClosure()) { it.into("jre") }
    return task
}

fun Project.windowsPackTask(dir: Ref<File>,
                            innoEXE: File,
                            config: ScapesEngineApplicationExtension,
                            taskName: String): Exec {

    val task = tasks.create(taskName, Exec::class.java)
    val innoArgs = arrayOf(Ref {
        "/DApplicationFullName=${config.fullName.resolveToString()}"
    }, Ref {
        "/DApplicationVersion=${config.version.resolveToString()}"
    }, Ref {
        "/DApplicationCompany=${config.company.resolveToString()}"
    }, Ref {
        "/DApplicationCopyright=${config.copyright.resolveToString()}"
    }, Ref {
        "/DApplicationURL=${config.url.resolveToString()}"
    }, Ref {
        "/DApplicationUUID=${config.uuid.resolveToString()}"
    }, Ref {
        "/DApplicationName=${config.name.resolveToString()}"
    })
    val innoISS = Ref { File(dir(), "Setup.iss").absolutePath }
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
        task.commandLine(innoEXE.absolutePath, *innoArgs, innoISS)
    } else {
        task.commandLine("wine", innoEXE.absolutePath, *innoArgs,
                Ref { "Z:$innoISS" })
    }
    return task
}

private val dllRegex = "(.+)\\.dll".toRegex()
