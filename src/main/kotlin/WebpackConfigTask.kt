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
        append(
            """var webpack = require("webpack");
var path = require("path");

var config = {
    output: {},
    mode: "production",
    module: {rules: []},
    resolve: {modules: []},
    plugins: [],
    node: {}
};

module.exports = config;
"""
        )
    }

    fun entry(path: String) {
        append(
            """
config.entry = "$path";
"""
        )
    }

    @JvmOverloads
    fun output(
        path: String?,
        filename: String? = "[name].bundle.js",
        chunkFilename: String? = "[id].bundle.js"
    ) {
        path?.let {
            append(
                """
config.output.path = path.resolve(__dirname, "$it");
"""
            )
        }
        filename?.let {
            append(
                """
config.output.filename = "$it";
"""
            )
        }
        chunkFilename?.let {
            append(
                """
config.output.chunkFilename = "$it";
"""
            )
        }
    }

    fun mode(value: String) {
        append(
            """
config.mode = "$value";
"""
        )
    }

    fun devtool(value: String?) {
        append(
            """
config.devtool = ${if (value == null) "false" else "\"$value\""};
"""
        )
    }

    fun resolvePath(path: String) {
        append(
            """
config.resolve.modules.push(path.resolve(__dirname, "$path"));
"""
        )
    }

    @JvmOverloads
    fun moduleRule(
        loader: String,
        test: String,
        options: String = "{}",
        include: String? = null,
        exclude: String? = null
    ) {
        append(
            """
config.module.rules.push({
    test: /$test/"""
        )
        include?.let {
            append(",\n    include: /$it/,")
        }
        exclude?.let {
            append(",\n    include: /$it/,")
        }
        append(
            """
    use: {
        loader: "$loader",
        options: $options
    }
})
"""
        )
    }

    fun plugin(plugin: String) {
        append(
            """
config.plugins.push($plugin);
"""
        )
    }

    fun node(module: String, mode: Boolean) {
        append(
            """
config.node.$module = $mode;
"""
        )
    }

    fun node(module: String, mode: String) {
        append(
            """
config.node.$module = "$mode";
"""
        )
    }

    fun append(str: String) {
        configMut.append(str)
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
