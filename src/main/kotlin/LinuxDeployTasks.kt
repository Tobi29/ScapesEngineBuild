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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

open class StartupScriptTask : DefaultTask() {
    @Input
    var output: Ref<File>? = null
    @Input
    var execName: Ref<String>? = null
    @Input
    var libPath: Ref<File>? = null
    @Input
    var mainClass: Ref<String>? = null

    @OutputFile
    fun output() = output.invoke() ?: throw IllegalStateException(
            "No output given")

    @TaskAction
    fun run() {
        val execName = execName() ?: throw IllegalStateException(
                "No execName given")
        val libPath = libPath() ?: throw IllegalStateException(
                "No libPath given")
        val mainClass = mainClass() ?: throw IllegalStateException(
                "No mainClass given")
        output().printWriter().use { writer ->
            writer.println("#!/bin/bash")
            writer.println("export CLASSPATH=\"$libPath/*\"")
            writer.println(
                    "exec -a $execName java -Djava.library.path=$libPath $mainClass \$@")
        }
    }
}
