package org.tobi29.scapes.engine.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RelativePath
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.tobi29.scapes.engine.gradle.dsl.ScapesEngineApplicationExtension

open class ScapesEngineApplication : Plugin<Project> {
    override fun apply(target: Project) {
        val config = target.extensions.create("application",
                ScapesEngineApplicationExtension::class.java, target)
        val javaConvention = target.convention.getPlugin(
                JavaPluginConvention::class.java)

        // Configurations
        val runtimeConfigurations = target.platformRuntimeConfigurations()
        val nativesConfigurations = target.platformNativesConfigurations()

        // Natives task
        val nativesTask = target.tasks.create("natives", Copy::class.java)
        nativesTask.description = "Extract natives for runtime"
        nativesTask.group = "run"
        val natives = target.file("${target.buildDir}/natives")
        nativesTask.from({ ->
            nativesConfigurations.platform.files.flatMap {
                if (it.name.endsWith(".jar")) {
                    target.zipTree(it).files.filter {
                        it.isFile && it.name.matches(
                                "(.+)\\.(dll|so(.[0-9]+)?|jnilib|dylib)".toRegex())
                    }
                } else {
                    listOf(it)
                }
            }
        }.toClosure()) {
            it.eachFile { fcp: FileCopyDetails ->
                fcp.relativePath = RelativePath(true, fcp.name)
                fcp.mode = 493 // 755
            }
        }
        nativesTask.into(natives)

        // Run task
        val runTask = target.tasks.create("run", JavaExec::class.java)
        runTask.description = "Runs this project as a JVM application"
        runTask.group = "Run"
        target.afterEvaluate {
            runTask.main = config.mainClass
            runTask.classpath = javaConvention.sourceSets
                    .getByName("main").runtimeClasspath
        }
        runTask.jvmArgs("-Xms64M", "-Xmx2048M", "-XX:+UseG1GC",
                "-XX:MaxGCPauseMillis=1")
        if (target.rootProject.hasProperty("runArgs")) {
            target.rootProject.property("runArgs")
            runTask.args(target.rootProject.property("runArgs")
                    .toString().splitArgumentList())
        }
        if (target.rootProject.hasProperty("jvmArgs")) {
            runTask.jvmArgs(target.rootProject.property("jvmArgs")
                    .toString().splitArgumentList())
        }
        if (target.rootProject.hasProperty("runEnv")) {
            runTask.environment.putAll(target.rootProject.property("runEnv")
                    .toString().splitArgumentMap())
        }
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            runTask.jvmArgs("-XstartOnFirstThread")
        }
        if (target.rootProject.hasProperty("runtime")) {
            runTask.jvmArgs(
                    "-Duser.dir=${target.rootProject.property("runtime")}")
        } else {
            runTask.jvmArgs("-Duser.dir=${target.file("runtime")}")
        }
        runTask.jvmArgs("-Djava.library.path=${natives.absolutePath}")
        runTask.standardInput = System.`in`
        runTask.dependsOn(target.tasks.getByName("classes"))
        runTask.dependsOn(nativesTask)

        // Full deploy task
        val deployTask = target.tasks.create("deploy", Task::class.java)
        deployTask.group = "Deployment"

        // Full fatJar task
        val fatJarTask = target.tasks.create("fatJar", Task::class.java)
        fatJarTask.group = "Deployment"

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
