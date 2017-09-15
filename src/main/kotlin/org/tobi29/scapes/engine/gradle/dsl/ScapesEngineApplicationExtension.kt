package org.tobi29.scapes.engine.gradle.dsl

import ApplicationType
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.setValue
import org.tobi29.scapes.engine.gradle.property

open class ScapesEngineApplicationExtension(target: Project) {
    val nameProvider = target.property<String>()

    var name by nameProvider

    val fullNameProvider = target.property<String>()

    var fullName by fullNameProvider

    val versionProvider = target.property<String>()

    var version by versionProvider

    val companyProvider = target.property<String>()

    var company by companyProvider

    val urlProvider = target.property<String>()

    var url by urlProvider

    val copyrightProvider = target.property<String>()

    var copyright by copyrightProvider

    val categoryProvider = target.property<ApplicationType>()
            .apply { set(ApplicationType.UTILITY) }

    var category by categoryProvider

    val uuidProvider = target.property<String>()

    var uuid by uuidProvider

    val mainClassProvider = target.property<String>()

    var mainClass by mainClassProvider

    val workingDirectoryInLibraryProvider = target.property<Boolean>()
            .apply { set(false) }

    var workingDirectoryInLibrary by workingDirectoryInLibraryProvider

    val adoptOpenJDKVersionProvider = target.property<String>()
            .apply { set("jdk8u144-b01") }

    var adoptOpenJDKVersion by adoptOpenJDKVersionProvider

    val ojdkBuildVersionProvider = target.property<Pair<String, String>>()
            .apply { set(Pair("1.8.0.131-1", "1.8.0.131-1.b11")) }

    var ojdkBuildVersion by ojdkBuildVersionProvider
}
