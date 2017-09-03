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
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.setValue
import org.tobi29.scapes.engine.gradle.invoke
import org.tobi29.scapes.engine.gradle.property
import java.io.File

open class Launch4jTask : DefaultTask() {
    val outputProvider = project.property<File>()

    var output by outputProvider
        @OutputFile get

    val exeMemoryMinProvider = project.property<Int>()

    var exeMemoryMin by exeMemoryMinProvider
        @Input get

    val exeMemoryMaxProvider = project.property<Int>()

    var exeMemoryMax by exeMemoryMaxProvider
        @Input get

    val exeTypeProvider = project.property<String>()

    var exeType by exeTypeProvider
        @Input get

    val fullNameProvider = project.property<String>()

    var fullName by fullNameProvider
        @Input get

    val versionProvider = project.property<String>()

    var version by versionProvider
        @Input get

    val companyProvider = project.property<String>()

    var company by companyProvider
        @Input get

    val copyrightProvider = project.property<String>()

    var copyright by copyrightProvider
        @Input get

    val mainClassProvider = project.property<String>()

    var mainClass by mainClassProvider
        @Input get

    val runInAppDataProvider = project.property<Boolean>()

    var runInAppData by runInAppDataProvider
        @Input get

    val launch4jProvider = project.property<File>()

    var launch4j by launch4jProvider
        @InputFile get

    val iconProvider = project.property<File>()

    var icon by iconProvider
        @InputFile get

    val manifestProvider = project.property<File>()

    var manifest by manifestProvider
        @InputFile get

    @TaskAction
    fun run() {
        val winVersion = "$version.0"
        ant.apply {
            invoke("taskdef") {
                put("name", "launch4j")
                put("classpath", launch4j.absolutePath)
                put("classname", "net.sf.launch4j.ant.Launch4jTask")
            }
            invoke("launch4j", {}) {
                invoke("config", {
                    put("headerType", exeType)
                    put("outfile", output.absolutePath)
                    put("dontWrapJar", "true")
                    put("icon", icon.absolutePath)
                    put("manifest", manifest.absolutePath)
                }) {
                    invoke("classPath") {
                        put("mainClass", mainClass)
                        put("cp", "%EXEDIR%\\lib\\*")
                    }
                    invoke("jre", {
                        put("initialheapsize", exeMemoryMin.toString())
                        put("maxheapsize", exeMemoryMax.toString())
                        put("path", "%EXEDIR%\\jre")
                    }) {
                        if (runInAppData) {
                            invoke("opt", "-Duser.dir=\"%APPDATA%\\$fullName\"")
                        }
                        invoke("opt", "-Djava.library.path=\"%EXEDIR%\"")
                    }
                    invoke("versioninfo") {
                        put("fileversion", winVersion)
                        put("txtfileversion", version)
                        put("filedescription", fullName)
                        put("copyright", copyright)
                        put("productversion", winVersion)
                        put("productname", fullName)
                        put("txtproductversion", version)
                        put("companyname", company)
                        put("internalname", fullName)
                        put("originalfilename", output.name)
                    }
                }
            }
        }
    }
}

