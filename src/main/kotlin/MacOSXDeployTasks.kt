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
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import java.io.File

open class AppBundlerTask : DefaultTask() {
    @Input
    var output: Ref<File>? = null
    @Input
    var fullName: Ref<String>? = null
    @Input
    var version: Ref<String>? = null
    @Input
    var copyright: Ref<String>? = null
    @Input
    var mainClass: Ref<String>? = null
    @Input
    var jre: Ref<File>? = null
    @Input
    var appbundler: Ref<File>? = null
    @Input
    var icon: Ref<File>? = null
    @Input
    var classpath: Ref<FileCollection>? = null

    @InputDirectory
    fun jre() = jre.invoke() ?: throw IllegalStateException(
            "No jre given")

    @InputFile
    fun appbundler() = appbundler.invoke() ?: throw IllegalStateException(
            "No appbundler given")

    @InputFile
    fun icon() = icon.invoke() ?: throw IllegalStateException(
            "No icon given")

    @OutputDirectory
    fun output() = output.invoke() ?: throw IllegalStateException(
            "No output given")

    @InputFiles
    fun classpath() = classpath.invoke() ?: throw IllegalStateException(
            "No classpath given")

    init {
        dependsOn({ _: Any? -> classpath().buildDependencies }.toClosure())
    }

    @TaskAction
    @Suppress("unused")
    fun run() {
        val jre = jre()
        val appbundler = appbundler()
        val icon = icon()
        val output = output()
        val fullName = fullName() ?: throw IllegalStateException(
                "No fullName given")
        val version = version() ?: throw IllegalStateException(
                "No version given")
        val copyright = copyright() ?: throw IllegalStateException(
                "No copyright given")
        val mainClass = mainClass() ?: throw IllegalStateException(
                "No mainClass given")
        val classpath = classpath()
        ant.apply {
            invoke("taskdef") {
                put("name", "bundleapp")
                put("classpath", appbundler.absolutePath)
                put("classname", "com.oracle.appbundler.AppBundlerTask")
            }
            invoke("bundleapp", {
                put("outputdirectory", output.parentFile.absolutePath)
                put("name", output.name.substringBeforeLast(".app"))
                put("displayname", fullName)
                put("identifier", mainClass)
                put("shortversion", version)
                put("icon", icon.absolutePath)
                put("mainclassname", mainClass)
                put("copyright", copyright)
                put("applicationCategory", "public.app-category.games")
                put("executableName", fullName)
            }) {
                classpath.addToAntBuilder(this, "classpath",
                        FileCollection.AntType.FileSet)
                invoke("arch") { put("name", "x86_64") }
                invoke("runtime") {
                    put("dir", "${jre.absolutePath}/Contents/Home")
                }
                invoke("option") { put("value", "-XstartOnFirstThread") }
                invoke("option") { put("value", "-Xms64M") }
                invoke("option") { put("value", "-Xmx2048M") }
                invoke("option") { put("value", "-XX:+UseG1GC") }
                invoke("option") { put("value", "-XX:MaxGCPauseMillis=1") }
                invoke("option") {
                    put("value", "-Xdock:icon=Contents/resources/Icon.icns")
                }
                invoke("argument") {
                    put("value",
                            "\$HOME/Library/Application Support/$fullName")
                }
            }
        }
    }
}