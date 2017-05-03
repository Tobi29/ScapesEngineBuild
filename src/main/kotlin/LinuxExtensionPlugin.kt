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
import java.io.File

open class ScapesEngineExtensionLinux : Plugin<Project> {
    override fun apply(target: Project) {
        val config = target.extensions.getByType(
                ScapesEngineExtensionExtension::class.java)

        // Platform deploy tasks
        val deployLinuxTask32 = target.addDeployLinuxExtensionTask("32", Ref {
            target.allJars("Linux32") - config.parent?.allJars("Linux32")
        }, Ref {
            target.configurations.getByName("nativesLinux32")
        }, Ref { config.parent?.let(::getName) ?: "" }, config)
        val deployLinuxTask64 = target.addDeployLinuxExtensionTask("64", Ref {
            target.allJars("Linux64") - config.parent?.allJars("Linux64")
        }, Ref {
            target.configurations.getByName("nativesLinux64")
        }, Ref { config.parent?.let(::getName) ?: "INVALID" }, config)

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

private fun getName(project: Project): String? {
    project.extensions.findByType(
            ScapesEngineExtensionExtension::class.java)?.let { extension ->
        return extension.name.resolveToString()
    }
    project.extensions.findByType(
            ScapesEngineApplicationExtension::class.java)?.let { application ->
        return application.name.resolveToString()
    }
    return null
}

fun Project.addDeployLinuxExtensionTask(arch: String,
                                        jars: Ref<FileCollection>,
                                        natives: Ref<FileCollection>,
                                        parentName: Ref<String>,
                                        config: ScapesEngineExtensionExtension): Task? {
    val libPath = if (rootProject.hasProperty("libPath")) {
        File(rootProject.property("libPath").toString())
    } else {
        File("/usr/share/java")
    }
    val lowerName = Ref { parentName.resolveToString().toLowerCase() }
    val libDir = Ref { File(libPath, lowerName()) }

    // Main task
    val task = linuxTarTask(libDir, "Linux$arch", jars,
            natives, Ref { config.name.resolveToString() },
            "deployLinux$arch")
    task.description =
            "Contains tarball that can be extracted into root for easier package creation"
    task.group = "Deployment"
    task.dependsOn("jar")
    afterEvaluate {
        task.baseName = "${config.name}-Linux$arch"
    }
    return task
}
