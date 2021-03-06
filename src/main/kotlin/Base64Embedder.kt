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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.setValue
import org.tobi29.base64.toBase64
import org.tobi29.scapes.engine.gradle.property
import java.io.File

open class Base64Embedder : DefaultTask() {
    val inputProvider = project.property<File>()

    var input by inputProvider
        @InputFile get

    val outputProvider = project.property<File>()

    var output by outputProvider
        @OutputFile get

    val codePackageProvider = project.property<String>()

    var codePackage by codePackageProvider
        @Input get

    val codeNameProvider = project.property<String>()

    var codeName by codeNameProvider
        @Input get

    @TaskAction
    fun run() {
        val base64 = input.readBytes().toBase64()
        output.also {
            it.parentFile.mkdirs()
        }.printWriter().use { writer ->
            writer.print(
                """package $codePackage

import org.tobi29.base64.fromBase64
import org.tobi29.io.HeapResource
import org.tobi29.io.ro
import org.tobi29.io.view

val $codeName by lazy {
    HeapResource(
            ("$base64")
            .fromBase64().view.ro)
}
"""
            )
        }
    }
}