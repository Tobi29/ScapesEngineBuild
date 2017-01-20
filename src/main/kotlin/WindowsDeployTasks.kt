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
import java.io.File

open class Launch4jTask : DefaultTask() {
    @Input
    var output: Ref<File>? = null
    @Input
    var exeMemoryMin: Ref<Int>? = null
    @Input
    var exeMemoryMax: Ref<Int>? = null
    @Input
    var exeType: Ref<String>? = null
    @Input
    var fullName: Ref<String>? = null
    @Input
    var version: Ref<String>? = null
    @Input
    var company: Ref<String>? = null
    @Input
    var copyright: Ref<String>? = null
    @Input
    var mainClass: Ref<String>? = null
    @Input
    var runInAppData: Ref<Boolean>? = null
    @Input
    var launch4j: Ref<File>? = null
    @Input
    var icon: Ref<File>? = null
    @Input
    var manifest: Ref<File>? = null

    @InputFile
    fun launch4j() = launch4j.invoke() ?: throw IllegalStateException(
            "No launch4j given")

    @InputFile
    fun icon() = icon.invoke() ?: throw IllegalStateException(
            "No icon given")

    @InputFile
    fun manifest() = manifest.invoke() ?: throw IllegalStateException(
            "No manifest given")

    @OutputFile
    fun output() = output.invoke() ?: throw IllegalStateException(
            "No output given")

    @TaskAction
    fun run() {
        val launch4j = launch4j()
        val icon = icon()
        val manifest = manifest()
        val output = output()
        val exeMemoryMin = exeMemoryMin() ?: throw IllegalStateException(
                "No exeMemoryMin given")
        val exeMemoryMax = exeMemoryMax() ?: throw IllegalStateException(
                "No exeMemoryMax given")
        val exeType = exeType() ?: throw IllegalStateException(
                "No exeType given")
        val fullName = fullName() ?: throw IllegalStateException(
                "No fullName given")
        val version = version() ?: throw IllegalStateException(
                "No version given")
        val company = company() ?: throw IllegalStateException(
                "No company given")
        val copyright = copyright() ?: throw IllegalStateException(
                "No copyright given")
        val mainClass = mainClass() ?: throw IllegalStateException(
                "No mainClass given")
        val runInAppData = runInAppData() ?: throw IllegalStateException(
                "No runInAppData given")
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
                            invoke("opt", "-Djava.library.path=\"%EXEDIR%\"")
                        }
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

