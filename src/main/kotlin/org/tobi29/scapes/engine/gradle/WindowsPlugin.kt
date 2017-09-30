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
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.task
import org.tobi29.scapes.engine.gradle.dsl.ScapesEngineApplicationExtension
import org.tobi29.scapes.engine.gradle.task.ClasspathExtractTask
import org.tobi29.scapes.engine.gradle.task.Launch4jTask
import java.io.File

open class ScapesEngineApplicationWindows : Plugin<Project> {
    override fun apply(target: Project) {
        val config = target.extensions.getByType(
                ScapesEngineApplicationExtension::class.java)

        // Platform deploy task
        val deployWindowsTasks = target.addDeployWindowsTasks(
                target.providers.provider {
                    target.allCommonJars()
                }, target.providers.provider {
            target.configurations.getByName("runtimeWindows32")
        }, target.providers.provider {
            target.configurations.getByName("runtimeWindows64")
        }, target.providers.provider {
            target.configurations.getByName("nativesWindows32")
        }, target.providers.provider {
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

fun Project.addDeployWindowsTasks(jars: Provider<FileCollection>,
                                  jars32: Provider<FileCollection>,
                                  jars64: Provider<FileCollection>,
                                  natives32: Provider<FileCollection>,
                                  natives64: Provider<FileCollection>,
                                  config: ScapesEngineApplicationExtension): List<Task> {
    val deployTasks = ArrayList<Task>()

    // JRE Task 32-Bit
    val (jreTask32, jre32) = ojdkBuildWindows(
            config.ojdkBuildVersionProvider, "32")

    // JRE Task 64-Bit
    // TODO: AdoptOpenJDK does not include a proper cacerts file on windows
    // val (jreTask64, jre64) = adoptOpenJDKWindows(
    //         config.adoptOpenJDKVersionProvider, "64")
    val (jreTask64, jre64) = ojdkBuildWindows(
            config.ojdkBuildVersionProvider, "64")

    // Program manifest extract task
    val programManifestExtractTask = task<ClasspathExtractTask>(
            "programManifestExtractWindows") {
        resourcePath = "Launch4j/Program.manifest"
        output = temporaryDir.resolve("Program.manifest")
    }

    // Inno setup script extract task
    val innoSetupScriptExtractTask = task<ClasspathExtractTask>(
            "innoSetupScriptExtractWindows") {
        resourcePath = "InnoSetup/Setup.iss"
        output = temporaryDir.resolve("Setup.iss")
    }

    // Program task
    val programTask = windowsProgramTask(false,
            config.nameProvider.map { "$it.exe" }, config,
            config.workingDirectoryInLibraryProvider,
            programManifestExtractTask.outputProvider,
            "programWindows")
    programTask.dependsOn(programManifestExtractTask)

    // Command task
    val programCmdTask = windowsProgramTask(true,
            config.nameProvider.map { "${it}Cmd.exe" }, config,
            config.workingDirectoryInLibraryProvider,
            programManifestExtractTask.outputProvider,
            "programCmdWindows")
    programCmdTask.dependsOn(programManifestExtractTask)

    // Zip tasks
    val deployWindowsZip32 = windowsZipTask(
            map(jars, jars32) { a, b -> a + b }, natives32,
            map(programTask.outputProvider,
                    programCmdTask.outputProvider) { a, b -> files(a, b) },
            jre32, "deployWindowsZip32")
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

    val deployWindowsZip64 = windowsZipTask(
            map(jars, jars64) { a, b -> a + b }, natives64,
            map(programTask.outputProvider,
                    programCmdTask.outputProvider) { a, b -> files(a, b) },
            jre64, "deployWindowsZip64")
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

    // Prepare task
    val prepareTask = windowsPrepareTask(jars, jars32, jars64, natives32,
            natives64, jre32, jre64, innoSetupScriptExtractTask.outputProvider,
            "prepareWindows")
    prepareTask.dependsOn(jreTask32)
    prepareTask.dependsOn(jreTask64)
    prepareTask.dependsOn(innoSetupScriptExtractTask)
    prepareTask.dependsOn("jar")
    prepareTask.dependsOn(programTask)
    prepareTask.dependsOn(programCmdTask)
    prepareTask.from(programTask.outputProvider.toClosure()) {
        it.into("install/common")
    }
    prepareTask.from(programCmdTask.outputProvider.toClosure()) {
        it.into("install/common")
    }

    val innoEXE = rootProject.file("resources/Inno Setup 5/ISCC.exe")
    if (!innoEXE.exists()) {
        logger.warn("No Inno Setup for Windows found!")
        return deployTasks
    }

    // Pack task
    val packTask = windowsPackTask(
            providers.provider(prepareTask.temporaryDir),
            innoEXE, config, "packWindows")
    packTask.dependsOn(prepareTask)

    // Main task
    val task = tasks.create("deployWindows", Copy::class.java)
    task.dependsOn(packTask)
    task.group = "Deployment"
    task.description = "Windows Installer with bundled JRE"
    task.from(File(prepareTask.temporaryDir, "output/output.exe"))
    task.rename({ str: String ->
        "${config.name}-Setup-${project.version}.exe"
    }.toClosure())
    task.into(File(buildDir, "distributions"))
    deployTasks.add(task)
    return deployTasks
}

fun Project.windowsProgramTask(cmd: Boolean,
                               exeName: Provider<String>,
                               config: ScapesEngineApplicationExtension,
                               workingDirInLibrary: Provider<Boolean>,
                               manifest: Provider<File>,
                               taskName: String): Launch4jTask {
    val task = tasks.create(taskName, Launch4jTask::class.java)
    task.fullNameProvider.set(config.fullNameProvider)
    task.versionProvider.set(config.versionProvider)
    task.companyProvider.set(config.companyProvider)
    task.copyrightProvider.set(config.copyrightProvider)
    task.mainClassProvider.set(config.mainClassProvider)
    task.launch4j = rootProject.file("resources/Launch4j/launch4j.jar")
    task.icon = file("project/Icon.ico")
    task.exeMemoryMin = 64
    task.exeMemoryMax = 2048
    task.exeType = if (cmd) "console" else "gui"
    task.runInAppDataProvider.set(workingDirInLibrary)
    task.manifestProvider.set(manifest)
    task.outputProvider.set(exeName.map { File(task.temporaryDir, it) })
    return task
}

fun Project.windowsPrepareTask(jars: Provider<FileCollection>,
                               jars32: Provider<FileCollection>,
                               jars64: Provider<FileCollection>,
                               natives32: Provider<FileCollection>,
                               natives64: Provider<FileCollection>,
                               jre32: Provider<File>,
                               jre64: Provider<File>,
                               iss: Provider<File>,
                               taskName: String): Copy {
    val task = tasks.create(taskName, Copy::class.java)
    task.from(iss.toClosure())
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
    task.from(natives32.map { fetchNativesWindows(it) }.toClosure()) {
        it.eachFile { fcp: FileCopyDetails ->
            fcp.relativePath = RelativePath(true, "install", "32", fcp.name)
            fcp.mode = 493 // 755
        }
    }
    task.from(natives64.map { fetchNativesWindows(it) }.toClosure()) {
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
    task.from(rootProject.file("resources/Install/Windows")) {
        it.into("install")
    }
    task.into(task.temporaryDir)
    return task
}

fun Project.windowsZipTask(jars: Provider<FileCollection>,
                           natives: Provider<FileCollection>,
                           exes: Provider<FileCollection>,
                           jre: Provider<File>,
                           taskName: String): Zip {
    val task = tasks.create(taskName, Zip::class.java)
    task.from(jars.toClosure()) { it.into("lib") }
    task.from(natives.map { fetchNativesWindows(it) }.toClosure()) {
        it.eachFile { fcp: FileCopyDetails ->
            fcp.relativePath = RelativePath(true, fcp.name)
            fcp.mode = 493 // 755
        }
    }
    task.from(exes.toClosure())
    task.from(jre.toClosure()) { it.into("jre") }
    return task
}

fun Project.windowsPackTask(dir: Provider<File>,
                            innoEXE: File,
                            config: ScapesEngineApplicationExtension,
                            taskName: String): Exec {
    val task = tasks.create(taskName, Exec::class.java)
    val innoArgs = arrayOf(
            config.fullNameProvider.map { "/DApplicationFullName=$it" }.lazyString(),
            config.versionProvider.map { "/DApplicationVersion=$it" }.lazyString(),
            config.companyProvider.map { "/DApplicationCompany=$it" }.lazyString(),
            config.copyrightProvider.map { "/DApplicationCopyright=$it" }.lazyString(),
            config.urlProvider.map { "/DApplicationURL=$it" }.lazyString(),
            config.uuidProvider.map { "/DApplicationUUID=$it" }.lazyString(),
            config.nameProvider.map { "/DApplicationName=$it" }.lazyString())
    val commandLine = arrayOf(innoEXE.absolutePath, *innoArgs, "Setup.iss")
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
        task.commandLine(*commandLine)
    } else {
        task.commandLine("wine", *commandLine)
    }
    afterEvaluate {
        task.workingDir = dir.get()
    }
    return task
}
