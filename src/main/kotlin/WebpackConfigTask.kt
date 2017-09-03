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
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.setValue
import org.tobi29.scapes.engine.gradle.property
import java.io.File

open class WebpackConfigTask : DefaultTask() {
    val outputProvider = project.property<File>()

    var output by outputProvider
        @OutputFile get

    private val configMut = StringBuilder().apply {
        append("""var webpack = require("webpack");
var path = require("path");

var config = {
    output: {},
    module: {rules: []},
    resolve: {modules: []},
    plugins: [],
    node: {}
};

module.exports = config;
""")
    }

    fun entry(path: String) {
        configMut.append("""
config.entry = "$path";
""")
    }

    @JvmOverloads
    fun output(path: String?,
               filename: String? = "[name].bundle.js",
               chunkFilename: String? = "[id].bundle.js") {
        path?.let {
            configMut.append("""
config.output.path = path.resolve(__dirname, "$it");
""")
        }
        filename?.let {
            configMut.append("""
config.output.filename = "$it";
""")
        }
        chunkFilename?.let {
            configMut.append("""
config.output.chunkFilename = "$it";
""")
        }
    }

    fun resolvePath(path: String) {
        configMut.append("""
config.resolve.modules.push(path.resolve(__dirname, "$path"));
""")
    }

    @JvmOverloads
    fun moduleRule(loader: String,
                   test: String,
                   options: String = "{}",
                   include: String? = null,
                   exclude: String? = null) {
        configMut.append("""
config.module.rules.push({
    test: /$test/""")
        include?.let {
            configMut.append(",\n    include: /$it/,")
        }
        exclude?.let {
            configMut.append(",\n    include: /$it/,")
        }
        configMut.append("""
    use: {
        loader: "$loader",
        options: $options
    }
})
""")
    }

    fun plugin(plugin: String) {
        configMut.append("""
config.plugins.push($plugin);
""")
    }

    fun node(module: String,
             mode: Boolean) {
        configMut.append("""
config.node.$module = $mode;
""")
    }

    fun node(module: String,
             mode: String) {
        configMut.append("""
config.node.$module = "$mode";
""")
    }

    val config @Input get() = configMut.toString()

    @TaskAction
    fun run() {
        output.also {
            it.parentFile.mkdirs()
        }.printWriter().use { writer ->
            writer.print(config)
        }
    }
}
