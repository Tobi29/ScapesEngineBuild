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

package org.tobi29.scapes.engine.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.setValue
import org.tobi29.scapes.engine.gradle.property
import java.io.File
import java.io.PrintWriter

open class StartupScriptTask : DefaultTask() {
    val outputProvider = project.property<File>()

    var output by outputProvider
        @OutputFile get

    val execNameProvider = project.property<String>()

    var execName by execNameProvider
        @Input get

    val libPathProvider = project.property<String>()

    var libPath by libPathProvider

    val mainClassProvider = project.property<String>()

    var mainClass by mainClassProvider
        @Input get

    val workingDirInLibraryProvider = project.property<Boolean>()
            .apply { set(false) }

    var workingDirInLibrary by workingDirInLibraryProvider
        @Input get

    @TaskAction
    fun run() {
        output.printWriter().use { writer: PrintWriter ->
            writer.println("#!/bin/bash")
            if (workingDirInLibrary) {
                writer.println("runtime=~/.$execName")
                writer.println("mkdir -p \"\$runtime\"")
                writer.println("cd \"\$runtime\"")
            }
            writer.println("export CLASSPATH=\"$libPath/*\"")
            writer.println(
                    "exec -a $execName java -Djava.library.path=$libPath $mainClass \$@")
        }
    }
}
