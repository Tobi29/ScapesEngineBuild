package org.tobi29.scapes.engine.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.tobi29.scapes.engine.gradle.dsl.ScapesEngineExtensionExtension

open class ScapesEngineExtension : Plugin<Project> {
    override fun apply(target: Project) {
        val config = target.extensions.create(
            "extension",
            ScapesEngineExtensionExtension::class.java, target
        )
        val javaConvention = target.convention.getPlugin(
            JavaPluginConvention::class.java
        )

        // Configurations
        val runtimeConfigurations = target.platformRuntimeConfigurations()
        val nativesConfigurations = target.platformNativesConfigurations()

        // Full deploy task
        val deployTask = target.tasks.create("deploy", Task::class.java)
        deployTask.group = "Deployment"

        // Add runtime configurations to classpath
        target.afterEvaluate {
            javaConvention.sourceSets.findByName("main")?.let {
                it.runtimeClasspath += runtimeConfigurations.platform
            }

            target.extensions.findByType(IdeaModel::class.java)?.apply {
                module.scopes["RUNTIME"]?.get("plus")
                    ?.add(runtimeConfigurations.platform)
                module.excludeDirs = module.excludeDirs + target.file("runtime")
            }
        }
    }
}
