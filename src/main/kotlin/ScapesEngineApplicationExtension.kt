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

import org.gradle.api.tasks.Input

open class ScapesEngineApplicationExtension {
    @Input
    var name: Any? = null
    @Input
    var fullName: Any? = null
    @Input
    var version: Any? = null
    @Input
    var company: Any? = null
    @Input
    var url: Any? = null
    @Input
    var copyright: Any? = null
    @Input
    var category: Any? = ApplicationType.UTILITY
    @Input
    var uuid: Any? = null
    @Input
    var mainClass: Any? = null
    @Input
    var workingDirectoryInLibrary: Any? = null
    @Input
    var adoptOpenJDKVersion: Any? = "jdk8u152-b04"
    @Input
    var ojdkBuildVersion: Any? = Pair("1.8.0.131-1", "1.8.0.131-1.b11")
}

enum class ApplicationType {
    DEVELOPMENT,
    GAME,
    GRAPHICS,
    INTERNET,
    MULTIMEDIA,
    OFFICE,
    UTILITY
}
