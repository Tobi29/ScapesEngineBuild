import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project

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

fun Project.addShadowTask(platform: String,
                          taskName: String): ShadowJar {
    val jarTask = tasks.findByName("jar")
    val task = tasks.create(taskName, ShadowJar::class.java)
    task.group = "Deployment"
    task.description = "Create a fat jar containing all dependencies of this project"
    task.classifier = "all-${platform.toLowerCase()}"
    files(jarTask).forEach { task.from(zipTree(it)) }
    task.configurations = listOf(configurations.getByName("runtime"),
            configurations.getByName("runtime$platform"),
            configurations.getByName("natives$platform"))
    task.dependsOn(jarTask)
    return task
}
