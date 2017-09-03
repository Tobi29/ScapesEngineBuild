package org.tobi29.scapes.engine.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.setValue
import org.tobi29.scapes.engine.gradle.AppPList
import org.tobi29.scapes.engine.gradle.property
import org.tobi29.scapes.engine.gradle.writeInfoPlist
import org.tobi29.scapes.engine.gradle.writePkgInfo
import java.io.File

open class AppPListTask : DefaultTask() {
    val plistProvider = project.property<AppPList>()

    var plist by plistProvider
        @Input get

    val plistFileProvider = project.property<File>()
            .apply { set(temporaryDir.resolve("Info.plist")) }

    var plistFile by plistFileProvider
        @OutputFile get

    val pkgFileProvider = project.property<File>()
            .apply { set(temporaryDir.resolve("PkgInfo")) }

    var pkgFile by pkgFileProvider
        @OutputFile get

    @TaskAction
    fun run() {
        plist.writeInfoPlist(plistFile)
        plist.writePkgInfo(pkgFile)
    }
}
