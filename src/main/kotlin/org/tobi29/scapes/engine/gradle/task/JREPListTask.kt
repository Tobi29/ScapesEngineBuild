package org.tobi29.scapes.engine.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.tobi29.scapes.engine.gradle.*
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
