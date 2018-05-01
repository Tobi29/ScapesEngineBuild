package org.tobi29.scapes.engine.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

fun Project.platformRuntimeConfigurations() = platformConfigurations("runtime")

fun Project.platformNativesConfigurations() = platformConfigurations("natives")

fun Project.platformConfigurations(prefix: String) =
    PlatformConfigurations(
        configurations.maybeCreate(prefix),
        configurations.maybeCreate("${prefix}Linux32"),
        configurations.maybeCreate("${prefix}Linux64"),
        configurations.maybeCreate("${prefix}MacOSX"),
        configurations.maybeCreate("${prefix}Windows32"),
        configurations.maybeCreate("${prefix}Windows64"),
        configurations.maybeCreate("${prefix}Platform")
    ).apply {
        val os = System.getProperty("os.name").toLowerCase()
        val arch = System.getProperty("os.arch")
        if (os.contains("linux")) {
            if (arch.contains("64")) {
                platform.extendsFrom(linux64)
            } else {
                platform.extendsFrom(linux32)
            }
        } else if (os.contains("mac")) {
            platform.extendsFrom(macOSX)
        } else if (os.contains("windows")) {
            if (arch.contains("64")) {
                platform.extendsFrom(windows64)
            } else {
                platform.extendsFrom(windows32)
            }
        }
    }

data class PlatformConfigurations(
    val common: Configuration,
    val linux32: Configuration,
    val linux64: Configuration,
    val macOSX: Configuration,
    val windows32: Configuration,
    val windows64: Configuration,
    val platform: Configuration
)
