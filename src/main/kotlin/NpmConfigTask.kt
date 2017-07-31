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
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.tobi29.scapes.engine.gradle.getValue
import org.tobi29.scapes.engine.gradle.property
import org.tobi29.scapes.engine.gradle.setValue
import org.tobi29.scapes.engine.utils.io.BufferedWriteChannelStream
import org.tobi29.scapes.engine.utils.io.tag.json.writeJSON
import org.tobi29.scapes.engine.utils.tag.MutableTagMap
import org.tobi29.scapes.engine.utils.tag.mapMut
import org.tobi29.scapes.engine.utils.tag.toTag
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

open class NpmConfigTask : DefaultTask() {
    val outputProvider = project.property<File>()

    var output by outputProvider
        @OutputFile get

    val config = MutableTagMap()
    @Input internal fun configStr() = config.toString()

    var pkgName: String?
        get() = config["name"]?.toString()
        set(value) {
            if (value == null) config.remove("name")
            else config["name"] = value.toTag()
        }

    var pkgVersion: String?
        get() = config["version"]?.toString()
        set(value) {
            if (value == null) config.remove("version")
            else config["version"] = value.toTag()
        }

    var pkgDescription: String?
        get() = config["description"]?.toString()
        set(value) {
            if (value == null) config.remove("description")
            else config["description"] = value.toTag()
        }

    var pkgMain: String?
        get() = config["main"]?.toString()
        set(value) {
            if (value == null) config.remove("main")
            else config["main"] = value.toTag()
        }

    var dependencies: MutableTagMap
        get() = config.mapMut("dependencies")
        set(value) {
            if (value == null) config.remove("dependencies")
            else config["dependencies"] = value
        }

    @JvmOverloads
    fun dependency(name: String,
                   version: String = "*") {
        dependencies[name] = version.toTag()
    }

    var devDependencies: MutableTagMap
        get() = config.mapMut("devDependencies")
        set(value) {
            if (value == null) config.remove("devDependencies")
            else config["devDependencies"] = value.toTag()
        }

    @JvmOverloads
    fun devDependency(name: String,
                      version: String = "*") {
        devDependencies[name] = version.toTag()
    }

    @TaskAction
    fun run() {
        FileChannel.open(output.also {
            it.parentFile.mkdirs()
        }.toPath(), StandardOpenOption.WRITE,
                StandardOpenOption.CREATE).use { channel ->
            val stream = BufferedWriteChannelStream(channel)
            config.toTag().writeJSON(stream)
            stream.flush()
        }
    }
}