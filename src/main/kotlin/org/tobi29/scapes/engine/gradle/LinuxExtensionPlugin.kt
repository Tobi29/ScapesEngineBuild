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
import org.gradle.api.provider.Provider
import org.tobi29.scapes.engine.gradle.dsl.ScapesEngineApplicationExtension
import org.tobi29.scapes.engine.gradle.dsl.ScapesEngineExtensionExtension

open class ScapesEngineExtensionLinux : Plugin<Project> {
    override fun apply(target: Project) {
        val config = target.extensions.getByType(
                ScapesEngineExtensionExtension::class.java)

        // Platform deploy tasks
        val deployLinuxTask32 = target.addDeployLinuxExtensionTask("32",
                provider {
                    target.allJars("Linux32") - config.parent.allJars("Linux32")
                }, provider {
            target.configurations.getByName("nativesLinux32")
        }, getName(config.parentProvider), config)
        val deployLinuxTask64 = target.addDeployLinuxExtensionTask("64",
                provider {
                    target.allJars("Linux64") - config.parent.allJars("Linux64")
                }, provider {
            target.configurations.getByName("nativesLinux64")
        }, getName(config.parentProvider), config)

        // Full deploy task
        val deployTask = target.tasks.getByName("deploy")
        if (deployLinuxTask32 != null) {
            deployTask.dependsOn(deployLinuxTask32)
        }
        if (deployLinuxTask64 != null) {
            deployTask.dependsOn(deployLinuxTask64)
        }
    }
}

private fun getName(
        project: Provider<Project>,
        default: Provider<String> = provider("INVALID")
) = project.map { project ->
    project.extensions?.findByType(
            ScapesEngineExtensionExtension::class.java)?.let { config ->
        return@map config.name
    }
    project.extensions?.findByType(
            ScapesEngineApplicationExtension::class.java)?.let { config ->
        return@map config.name
    }
    return@map default.get()
}

fun Project.addDeployLinuxExtensionTask(arch: String,
                                        jars: Provider<FileCollection>,
                                        natives: Provider<FileCollection>,
                                        parentName: Provider<String>,
                                        config: ScapesEngineExtensionExtension): Task? {
    val libPath = if (rootProject.hasProperty("libPath")) {
        rootProject.property("libPath").toString()
    } else {
        "/usr/share/java"
    }
    val libDir = parentName.map { "$libPath/${it.toLowerCase()}" }

    // Main task
    val task = linuxTarTask(libDir, "Linux$arch", jars,
            natives, config.nameProvider, "deployLinux$arch")
    task.description =
            "Contains tarball that can be extracted into root for easier package creation"
    task.group = "Deployment"
    task.dependsOn("jar")
    afterEvaluate {
        task.baseName = "${config.name}-Linux$arch"
    }
    return task
}
