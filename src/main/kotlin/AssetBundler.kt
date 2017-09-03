import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.setValue
import org.tobi29.scapes.engine.gradle.property
import org.tobi29.scapes.engine.utils.io.BufferedWriteChannelStream
import org.tobi29.scapes.engine.utils.io.tag.binary.writeBinary
import org.tobi29.scapes.engine.utils.tag.MutableTagMap
import org.tobi29.scapes.engine.utils.tag.TagMap
import org.tobi29.scapes.engine.utils.tag.mapMut
import org.tobi29.scapes.engine.utils.tag.toTag
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

open class AssetBundler : DefaultTask() {
    val inputsProvider = project.property<FileTree>()

    var inputs by inputsProvider
        @InputFiles get

    val outputProvider = project.property<File>()

    var output by outputProvider
        @OutputFile get

    @TaskAction
    fun run() {
        val bundleMut = MutableTagMap()
        inputs.visit(object : FileVisitor {
            override fun visitFile(f: FileVisitDetails) {
                var dir = bundleMut
                val segments = f.relativePath.segments
                for (i in 0..segments.size - 2) {
                    val element = dir.mapMut(segments[i])
                    dir["Type"]?.toString()?.let {
                        if (it != "Directory") {
                            throw IllegalArgumentException(
                                    "Conflicting entries")
                        }
                    }
                    dir = element.mapMut("Contents")
                }
                if (dir.contains(segments.last())) {
                    throw IllegalArgumentException("Conflicting entries")
                }
                val element = dir.mapMut(segments.last())
                element["Type"] = "File".toTag()
                element["Contents"] = FileInputStream(f.file)
                        .use { it.readBytes() }.toTag()
            }

            override fun visitDir(d: FileVisitDetails) {
                var dir = bundleMut
                val segments = d.relativePath.segments
                for (i in 0..segments.size - 2) {
                    val element = dir.mapMut(segments[i])
                    dir["Type"]?.toString()?.let {
                        if (it != "Directory") {
                            throw IllegalArgumentException(
                                    "Conflicting entries")
                        }
                    }
                    dir = element.mapMut("Contents")
                }
                if (dir.contains(segments.last())) {
                    throw IllegalArgumentException("Conflicting entries")
                }
                val element = dir.mapMut(segments.last())
                element["Type"] = "Directory".toTag()
                element["Contents"] = TagMap()
            }
        })
        val bundle = bundleMut.toTag()
        FileChannel.open(output.also {
            it.parentFile.mkdirs()
        }.toPath(), StandardOpenOption.WRITE,
                StandardOpenOption.CREATE).use { channel ->
            val stream = BufferedWriteChannelStream(channel)
            bundle.writeBinary(stream, 9)
            stream.flush()
        }
    }
}