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

import ApplicationType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.gradle.kotlin.dsl.task
import org.tobi29.scapes.engine.gradle.dsl.ScapesEngineApplicationExtension
import org.tobi29.scapes.engine.gradle.task.AppPListTask
import org.tobi29.scapes.engine.gradle.task.ClasspathExtractTask
import org.tobi29.scapes.engine.gradle.task.JREPListTask

open class ScapesEngineApplicationMacOSX : Plugin<Project> {
    @Suppress("ReplaceSingleLineLet")
    override fun apply(target: Project) {
        val config = target.extensions.getByType(
                ScapesEngineApplicationExtension::class.java)

        // Platform deploy task
        val deployMacOSXTask = target.addDeployMacOSXTask(
                target.providers.provider {
                    target.allJars("MacOSX")
                }, target.providers.provider<FileCollection> {
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

fun Project.addDeployMacOSXTask(jars: Provider<FileCollection>,
                                natives: Provider<FileCollection>,
                                config: ScapesEngineApplicationExtension): Task? {
    // JRE task
    val (jreTask, jre) = adoptOpenJDKMacOSX(
            config.adoptOpenJDKVersionProvider)

    // Launcher extract task
    val launcherExtractTask = task<ClasspathExtractTask>(
            "launcherExtractMacOSX") {
        resourcePath = "AppBundler/JavaAppLauncher"
        outputProvider.set(config.nameProvider.map { temporaryDir.resolve(it) })
    }

    // Localizable extract task
    val localizableExtractTask = task<ClasspathExtractTask>(
            "localizableExtractMacOSX") {
        resourcePath = "AppBundler/Localizable.strings"
        output = temporaryDir.resolve("Localizable.strings")
    }

    // App plist task
    val appPListTask = task<AppPListTask>("appPListMacOSX") {
        plistProvider.set(provider { config.generatePList() })
    }

    // JRE plist task
    val jrePListTask = task<JREPListTask>("jrePListMacOSX") {
        plistProvider.set(config.adoptOpenJDKVersionProvider.map {
            adoptOpenJDKPList(it)
        })
    }

    // Main task
    val task = task<Tar>("deployMacOSX") {
        compression = Compression.GZIP
        val bundle = config.fullNameProvider.map { "$it.app" }
        val contents = bundle.map { "$it/Contents" }
        val javaDir = contents.map { "$it/Java" }
        val macOSDir = contents.map { "$it/MacOS" }
        val plugInsDir = contents.map { "$it/PlugIns" }
        val resourcesDir = contents.map { "$it/Resources" }

        from(jars.toClosure()) { it.into(javaDir.toClosure()) }
        from(jre.toClosure()) {
            it.into(plugInsDir.map { "$it/JRE.jre/Contents/Home" }.toClosure())
        }
        // There seems to be no way to add symlinks to tars with gradle
        // So we just copy the library (It is only 72 KiB)
        from(jre.toClosure()) {
            it.include("lib/jli/libjli.dylib")
            it.eachFile { fcp: FileCopyDetails ->
                fcp.relativePath = RelativePath(true, plugInsDir.get(),
                        "JRE.jre", "Contents", "MacOS", "libjli.dylib")
                fcp.mode = 493 // 755
            }
            it.includeEmptyDirs = false
        }
        from(jrePListTask.plistFileProvider.toClosure()) {
            it.into(plugInsDir.map { "$it/JRE.jre/Contents" }.toClosure())
        }
        from({ launcherExtractTask.output }.toClosure()) {
            it.eachFile { fcp: FileCopyDetails ->
                fcp.mode = 493 // 755
            }
            it.into(macOSDir.toClosure())
        }
        from({ localizableExtractTask.output }.toClosure()) {
            it.into(resourcesDir.map { "$it/en.lproj" }.toClosure())
        }
        from(natives.map { fetchNativesMacOSX(it) }.toClosure()) {
            it.eachFile { fcp: FileCopyDetails ->
                fcp.relativePath = RelativePath(true, macOSDir.get(), fcp.name)
                fcp.mode = 493 // 755
            }
            it.includeEmptyDirs = false
        }
        from({
            files("project/Icon.icns")
        }.toClosure()) {
            it.into(resourcesDir.toClosure())
        }
        from(map(appPListTask.plistFileProvider,
                appPListTask.pkgFileProvider) { a, b ->
            files(a, b)
        }.toClosure()) {
            it.into(contents.toClosure())
        }
    }
    task.description =
            "Mac OS X Application containing necessary files to run the game"
    task.group = "Deployment"
    task.dependsOn(jreTask)
    task.dependsOn(launcherExtractTask)
    task.dependsOn(localizableExtractTask)
    task.dependsOn(appPListTask)
    task.dependsOn(jrePListTask)
    task.dependsOn("jar")
    afterEvaluate {
        task.baseName = "${config.name}-MacOSX"
    }
    return task
}

fun ScapesEngineApplicationExtension.generatePList() = AppPList(
        name = name,
        displayName = fullName,
        executableName = name,
        identifier = mainClass,
        shortVersion = version,
        mainClassName = mainClass,
        copyright = copyright,
        icon = "Icon.icns",
        runtime = "JRE.jre",
        workingDirectoryInLibrary = workingDirectoryInLibrary,
        applicationCategory = when (category) {
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
