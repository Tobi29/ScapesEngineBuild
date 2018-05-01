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

package org.tobi29.scapes.engine.gradle

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import java.io.File

fun Project.allJars(platform: String): FileCollection =
    allCommonJars() + configurations.getByName("runtime$platform")

fun Project.allCommonJars(): FileCollection =
    configurations.getByName("runtime") + files(tasks.getByName("jar"))

fun Project.fetchNativesLinux(jars: FileCollection) =
    fetchNatives(jars, soRegex)

fun Project.fetchNativesMacOSX(jars: FileCollection) =
    fetchNatives(jars, libRegex)

fun Project.fetchNativesWindows(jars: FileCollection) =
    fetchNatives(jars, dllRegex)

fun Project.fetchNatives(
    jars: FileCollection,
    regex: Regex
): FileCollection = run {
    files(jars.asSequence().map<File, Any> {
        if (it.name.endsWith(".jar")) {
            zipTree(it).files.asSequence().filter {
                it.isFile && it.name.matches(regex)
            }.toList()
        } else {
            it
        }
    }.toList())
}

private val soRegex = "(.+)\\.so(.[0-9]+)?".toRegex()
private val libRegex = "(.+)\\.(jnilib|dylib)".toRegex()
private val dllRegex = "(.+)\\.dll".toRegex()
