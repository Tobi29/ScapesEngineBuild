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
import java.io.File

open class ScapesEngineApplicationWindows : Plugin<Project> {
    override fun apply(target: Project) {
        val config = target.extensions.getByType(
                ScapesEngineApplicationExtension::class.java)

        // Platform deploy task
        val deployWindowsTask = target.addDeployWindowsTask(Ref {
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

        // Full deploy task
        val deployTask = target.tasks.getByName("deploy")
        if (deployWindowsTask != null) {
            deployTask.dependsOn(deployWindowsTask)
        }
    }
}

fun Project.addDeployWindowsTask(jars: Ref<FileCollection>,
                                 jars32: Ref<FileCollection>,
                                 jars64: Ref<FileCollection>,
                                 natives32: Ref<FileCollection>,
                                 natives64: Ref<FileCollection>,
                                 config: ScapesEngineApplicationExtension): Task? {
    // JRE Task 64-Bit
    val jreTask32 = jreTask("jreWindows32", "Windows/32")
    if (jreTask32 == null) {
        logger.warn("No 32-Bit JRE for Windows found!")
        return null
    }
    pruneJREWindows(jreTask32, "*")

    // JRE Task 64-Bit
    val jreTask64 = jreTask("jreWindows64", "Windows/64")
    if (jreTask64 == null) {
        logger.warn("No 64-Bit JRE for Windows found!")
        return null
    }
    pruneJREWindows(jreTask64, "*")

    // Program task
    val programTask = windowsProgramTask(false, Ref { "${config.name}.exe" },
            config, "programWindows")

    // Command task
    val programCmdTask = windowsProgramTask(true,
            Ref { "${config.name}Cmd.exe" }, config, "programWindowsCmd")

    // Prepare Task
    val prepareTask = windowsPrepareTask(jars, jars32, jars64, natives32,
            natives64,
            Ref { jreTask32.temporaryDir },
            Ref { jreTask64.temporaryDir },
            "prepareWindows")
    prepareTask.dependsOn(jreTask32)
    prepareTask.dependsOn(jreTask64)
    prepareTask.dependsOn("jar")
    prepareTask.dependsOn(programTask)
    prepareTask.dependsOn(programCmdTask)
    prepareTask.from(
            { programTask.output() }.toClosure()) { it.into("install/common") }
    prepareTask.from({ programCmdTask.output() }.toClosure()) {
        it.into("install/common")
    }

    // Pack task
    val packTask = windowsPackTask(Ref { prepareTask.temporaryDir }, config,
            "packWindows")
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
    return task
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
        file("$rootDir/ScapesEngine/resources/Launch4j/launch4j.jar")
    }
    task.icon = Ref { file("project/Icon.ico") }
    task.exeMemoryMin = Ref { 64 }
    task.exeMemoryMax = Ref { 2048 }
    task.exeType = Ref { if (cmd) "console" else "gui" }
    task.runInAppData = Ref { !cmd }
    task.manifest = Ref {
        file("$rootDir/ScapesEngine/resources/Program.manifest")
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
    task.from(rootProject.file("ScapesEngine/resources/Setup.iss"))
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
            "ScapesEngine/resources/Install/Windows")) { it.into("install") }
    task.into(task.temporaryDir)
    return task
}

fun Project.windowsPackTask(dir: Ref<File>,
                            config: ScapesEngineApplicationExtension,
                            taskName: String): Exec {

    val task = tasks.create(taskName, Exec::class.java)
    val innoEXE = rootProject.
            file("ScapesEngine/resources/Inno Setup 5/ISCC.exe").absolutePath
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
        task.commandLine(innoEXE, *innoArgs, innoISS)
    } else {
        task.commandLine("wine", innoEXE, *innoArgs, Ref { "Z:$innoISS" })
    }
    return task
}

private val dllRegex = "(.+)\\.dll".toRegex()
