package org.tobi29.scapes.engine.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.setValue
import org.tobi29.scapes.engine.gradle.property
import org.tobi29.scapes.engine.utils.io.BufferedWriteChannelStream
import org.tobi29.scapes.engine.utils.io.asArray
import org.tobi29.scapes.engine.utils.io.classpath.ClasspathPath
import org.tobi29.scapes.engine.utils.io.process
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

open class ClasspathExtractTask : DefaultTask() {
    val outputProvider = project.property<File>()

    var output by outputProvider
        @OutputFile get

    val resourcePathProvider = project.property<String>()

    var resourcePath by resourcePathProvider
        @Input get

    val data by lazy { read() }
        @Input get

    @TaskAction
    fun run() {
        FileChannel.open(output.toPath(), StandardOpenOption.WRITE,
                StandardOpenOption.CREATE).use {
            val stream = BufferedWriteChannelStream(it)
            stream.put(data)
            stream.flush()
        }
    }

    private fun read() =
            ClasspathPath(this::class.java.classLoader, resourcePath).read {
                process(it, asArray())
            }
}
