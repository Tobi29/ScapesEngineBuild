package org.tobi29.scapes.engine.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.setValue
import org.tobi29.scapes.engine.gradle.JREPList
import org.tobi29.scapes.engine.gradle.property
import org.tobi29.scapes.engine.gradle.writeInfoPlist
import java.io.File

open class JREPListTask : DefaultTask() {
    val plistProvider = project.property<JREPList>()

    var plist by plistProvider
        @Input get

    val plistFileProvider = project.property<File>()
        .apply { set(temporaryDir.resolve("Info.plist")) }

    var plistFile by plistFileProvider
        @OutputFile get

    @TaskAction
    fun run() {
        plist.writeInfoPlist(plistFile)
    }
}
