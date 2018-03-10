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

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.setValue
import org.tobi29.io.OutputStreamByteStream
import org.tobi29.io.tag.binary.writeBinary
import org.tobi29.scapes.engine.gradle.property
import org.tobi29.scapes.engine.shader.frontend.clike.CLikeShader
import java.io.File

open class ShaderCompileTask : DefaultTask() {
    val inputProvider = project.property<FileTree>().apply {
        set(project.fileTree("src/main/shaders"))
    }

    var input by inputProvider
        @InputFiles get

    val outputProvider = project.property<File>().apply {
        set(temporaryDir)
    }

    var output by outputProvider
        @OutputDirectory get

    @TaskAction
    fun run() {
        output.deleteRecursively()
        output.mkdirs()
        input.visit { fvd ->
            if (fvd.isDirectory) return@visit
            val source = fvd.open().use {
                it.bufferedReader().lineSequence().joinToString("\n")
            }
            val compiled = CLikeShader.compile(source)
            val destination = fvd.relativePath.parent.getFile(output)
                .resolve(fvd.name.compiledName)
            destination.parentFile?.mkdirs()
            destination.outputStream().use {
                compiled.toTag().writeBinary(OutputStreamByteStream(it), 9)
            }
        }
    }
}

private val String.compiledName
    get() = "${removeSuffix(".program")}.stag"
