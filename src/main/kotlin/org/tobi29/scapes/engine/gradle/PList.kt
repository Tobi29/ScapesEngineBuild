/*
 * Copyright 2012, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.tobi29.scapes.engine.gradle

import java.io.*
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamWriter

data class AppPList(
    val name: String = "",
    val displayName: String? = null,
    val identifier: String = "",
    val icon: String? = null,
    val executableName: String = "JavaAppLauncher",
    val shortVersion: String = "1.0",
    val version: String = "1.0",
    val signature: String = "????",
    val copyright: String? = null,
    val workingDirectoryInLibrary: Boolean = false,
    val minimumSystemVersion: String? = null,
    val applicationCategory: String = "",
    val highResolutionCapable: Boolean = true,
    val supportsAutomaticGraphicsSwitching: Boolean = true,
    val hideDockIcon: Boolean = false,
    val mainClassName: String = "",
    val runtime: String,
    val options: List<Option> = emptyList(),
    val arguments: List<String> = emptyList(),
    val architectures: List<String> = emptyList(),
    val registeredProtocols: List<String> = emptyList(),
    val bundleDocuments: List<BundleDocument> = emptyList(),
    val exportedTypeDeclarations: List<TypeDeclaration> = emptyList(),
    val importedTypeDeclarations: List<TypeDeclaration> = emptyList(),
    val plistEntries: List<PListEntry> = emptyList(),
    val environments: List<Environment> = emptyList()
) : Serializable

data class JREPList(
    val name: String = "",
    val identifier: String = "",
    val executableName: String = "libjli.dylib",
    val shortVersion: String = "1.0",
    val version: String = "1.0",
    val signature: String = "????",
    val minimumSystemVersion: String? = null,
    val jvmMinimumFrameworkVersion: String,
    val jvmMinimumSystemVersion: String,
    val jvmPlatformVersion: String,
    val jvmVendor: String,
    val jvmVersion: String
) : Serializable

data class TypeDeclaration(
    val identifier: String,
    val referenceUrl: String? = null,
    val description: String? = null,
    val icon: String? = null,
    val conformsTo: List<String> = listOf("public.data"),
    val osTypes: List<String>? = null,
    val mimeTypes: List<String>? = null,
    val extensions: List<String>? = null
) : Serializable

data class BundleDocument(
    val name: String = "",
    val role: String = "Editor",
    val icon: String?,
    val handlerRank: String?,
    val extensions: List<String>? = null,
    val contentTypes: List<String>? = null,
    val exportableTypes: List<String>? = null,
    val isPackage: Boolean = false
) : Serializable

data class Option(
    val key: String? = null,
    val value: String
) : Serializable

data class Environment(
    val key: String,
    val value: String
) : Serializable

data class PListEntry(
    val type: String,
    val key: String,
    val value: String
) : Serializable

fun AppPList.writeInfoPlist(file: File) {
    writeInfoPList(file) {
        // Write bundle properties
        writeBundle(
            name = name,
            displayName = displayName,
            identifier = identifier,
            icon = icon,
            executableName = executableName,
            packageType = "APPL",
            shortVersion = shortVersion,
            version = version,
            signature = signature,
            copyright = copyright,
            minimumSystemVersion = minimumSystemVersion
        )

        // Write application properties
        writeProperty("LSApplicationCategoryType", applicationCategory)
        if (hideDockIcon) {
            writeProperty("LSUIElement", true)
        }
        if (highResolutionCapable) {
            writeProperty("NSHighResolutionCapable", true)
        }
        if (supportsAutomaticGraphicsSwitching) {
            writeProperty("NSSupportsAutomaticGraphicsSwitching", true)
        }

        if (registeredProtocols.isNotEmpty()) {
            writeKey("CFBundleURLTypes")
            writeStartElement(ARRAY_TAG)
            writeCharacters("\n")
            writeStartElement(DICT_TAG)
            writeCharacters("\n")

            writeProperty("CFBundleURLName", identifier)
            writeStringArray("CFBundleURLSchemes", registeredProtocols)

            writeEndElement()
            writeCharacters("\n")
            writeEndElement()
            writeCharacters("\n")
        }

        writeProperty("JVMRuntime", runtime)

        if (workingDirectoryInLibrary) {
            writeProperty("WorkingDirectoryInLibrary", true)
        }

        writeProperty("JVMMainClassName", mainClassName)

        // Write CFBundleDocument entries
        writeKey("CFBundleDocumentTypes")
        writeBundleDocuments(bundleDocuments)

        // Write Type Declarations
        if (!exportedTypeDeclarations.isEmpty()) {
            writeKey("UTExportedTypeDeclarations")
            writeTypeDeclarations(exportedTypeDeclarations)
        }
        if (!importedTypeDeclarations.isEmpty()) {
            writeKey("UTImportedTypeDeclarations")
            writeTypeDeclarations(importedTypeDeclarations)
        }

        // Write architectures
        writeStringArray("LSArchitecturePriority", architectures)

        // Write Environment
        writeKey("LSEnvironment")
        writeStartElement(DICT_TAG)
        writeCharacters("\n")
        writeKey("LC_CTYPE")
        writeString("UTF-8")

        for ((key, value) in environments) {
            writeProperty(key, value)
        }

        writeEndElement()
        writeCharacters("\n")

        // Write options
        writeKey("JVMOptions")

        writeStartElement(ARRAY_TAG)
        writeCharacters("\n")

        for ((key, value) in options) {
            if (key == null) {
                writeString(value)
            }
        }

        writeEndElement()
        writeCharacters("\n")

        // Write default options
        writeKey("JVMDefaultOptions")

        writeStartElement(DICT_TAG)
        writeCharacters("\n")

        for ((key, value) in options) {
            if (key != null) {
                writeProperty(key, value)
            }
        }

        writeEndElement()
        writeCharacters("\n")

        // Write arguments
        writeStringArray("JVMArguments", arguments)

        // Write arbitrary key-value pairs
        for ((type, key, value) in plistEntries) {
            writeKey(key)
            writeValue(type, value)
        }
    }
}

fun JREPList.writeInfoPlist(file: File) {
    writeInfoPList(file) {
        // Write bundle properties
        writeBundle(
            name = name,
            identifier = identifier,
            executableName = executableName,
            packageType = "BNDL",
            shortVersion = shortVersion,
            version = version,
            signature = signature,
            minimumSystemVersion = minimumSystemVersion
        )

        // Write jvm properties
        writeKey("JavaVM")

        writeStartElement(DICT_TAG)
        writeCharacters("\n")

        writeProperty("JVMMinimumFrameworkVersion", jvmMinimumFrameworkVersion)
        writeProperty("JVMMinimumSystemVersion", jvmMinimumSystemVersion)
        writeProperty("JVMPlatformVersion", jvmPlatformVersion)
        writeProperty("JVMVendor", jvmVendor)
        writeProperty("JVMVersion", jvmVersion)


        writeEndElement()
        writeCharacters("\n")
    }
}

fun AppPList.writePkgInfo(file: File) {
    file.writer().use { writer ->
        writer.write("APPL$signature")
    }
}

private const val PLIST_DTD =
    "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">"
private const val PLIST_TAG = "plist"
private const val PLIST_VERSION_ATTRIBUTE = "version"
private const val DICT_TAG = "dict"
private const val KEY_TAG = "key"
private const val ARRAY_TAG = "array"
private const val STRING_TAG = "string"

private fun <R> writeInfoPList(
    file: File,
    contents: XMLStreamWriter.() -> R
): R? {
    val out = BufferedWriter(FileWriter(file))
    val output = XMLOutputFactory.newInstance()

    try {
        val result = output.createXMLStreamWriter(out).writeInfoPList(contents)
        out.flush()
        return result
    } catch (exception: XMLStreamException) {
        throw IOException(exception)
    } finally {
        out.close()
    }
}

private fun <R> XMLStreamWriter.writeInfoPList(
    contents: XMLStreamWriter.() -> R
): R {
    // Write XML declaration
    writeStartDocument()
    writeCharacters("\n")

    // Write plist DTD declaration
    writeDTD(PLIST_DTD)
    writeCharacters("\n")

    // Begin root element
    writeStartElement(PLIST_TAG)
    writeAttribute(PLIST_VERSION_ATTRIBUTE, "1.0")
    writeCharacters("\n")

    // Begin root dictionary
    writeStartElement(DICT_TAG)
    writeCharacters("\n")

    val result = contents()

    // End root dictionary
    writeEndElement()
    writeCharacters("\n")

    // End root element
    writeEndElement()
    writeCharacters("\n")

    // Close document
    writeEndDocument()
    writeCharacters("\n")

    return result
}

private fun XMLStreamWriter.writeBundle(
    name: String,
    displayName: String? = null,
    identifier: String,
    icon: String? = null,
    executableName: String,
    packageType: String,
    shortVersion: String,
    version: String,
    signature: String,
    copyright: String? = null,
    minimumSystemVersion: String? = null
) {
    writeProperty("CFBundleDevelopmentRegion", "English")
    writeProperty("CFBundleExecutable", executableName)
    icon?.let { writeProperty("CFBundleIconFile", it) }
    writeProperty("CFBundleIdentifier", identifier)
    displayName?.let { writeProperty("CFBundleDisplayName", it) }
    writeProperty("CFBundleInfoDictionaryVersion", "6.0")
    writeProperty("CFBundleName", name)
    writeProperty("CFBundlePackageType", packageType)
    writeProperty("CFBundleShortVersionString", shortVersion)
    writeProperty("CFBundleVersion", version)
    writeProperty("CFBundleSignature", signature)
    copyright?.let { writeProperty("NSHumanReadableCopyright", it) }
    minimumSystemVersion?.let {
        writeProperty("LSMinimumSystemVersion", it)
    }
}

private fun XMLStreamWriter.writeKey(key: String) {
    writeStartElement(KEY_TAG)
    writeCharacters(key)
    writeEndElement()
    writeCharacters("\n")
}

private fun XMLStreamWriter.writeValue(
    type: String = STRING_TAG,
    value: String
) {
    if ("boolean" == type) {
        writeBoolean("true" == value)
    } else {
        writeStartElement(type)
        writeCharacters(value)
        writeEndElement()
        writeCharacters("\n")
    }
}

private fun XMLStreamWriter.writeString(value: String) {
    writeStartElement(STRING_TAG)
    writeCharacters(value)
    writeEndElement()
    writeCharacters("\n")
}

private fun XMLStreamWriter.writeBoolean(value: Boolean) {
    writeEmptyElement(if (value) "true" else "false")
    writeCharacters("\n")
}

private fun XMLStreamWriter.writeProperty(
    key: String,
    value: Boolean
) {
    writeKey(key)
    writeBoolean(value)
}

private fun XMLStreamWriter.writeProperty(
    key: String,
    value: String
) {
    writeKey(key)
    writeString(value)
}

private fun XMLStreamWriter.writeStringArray(
    key: String,
    values: List<String>?
) {
    if (values != null) {
        writeKey(key)
        writeStartElement(ARRAY_TAG)
        writeCharacters("\n")
        for (singleValue in values) {
            writeString(singleValue)
        }
        writeEndElement()
        writeCharacters("\n")
    }
}

private fun XMLStreamWriter.writeBundleDocuments(bundleDocuments: List<BundleDocument>) {

    writeStartElement(ARRAY_TAG)
    writeCharacters("\n")

    for ((name, role, icon, handlerRank, extensions, contentTypes,
            exportableTypes, isPackage) in bundleDocuments) {
        writeStartElement(DICT_TAG)
        writeCharacters("\n")

        if (contentTypes != null) {
            writeStringArray("LSItemContentTypes", contentTypes)
        } else {
            writeStringArray("CFBundleTypeExtensions", extensions)
            writeProperty("LSTypeIsPackage", isPackage)
        }
        writeStringArray("NSExportableTypes", exportableTypes)

        icon?.let {
            writeProperty("CFBundleTypeIconFile", it)
        }

        writeProperty("CFBundleTypeName", name)
        writeProperty("CFBundleTypeRole", role)
        handlerRank?.let {
            writeProperty("LSHandlerRank", it)
        }

        writeEndElement()
        writeCharacters("\n")
    }

    writeEndElement()
    writeCharacters("\n")
}

private fun XMLStreamWriter.writeTypeDeclarations(typeDeclarations: List<TypeDeclaration>) {
    writeStartElement(ARRAY_TAG)
    writeCharacters("\n")
    for ((identifier, referenceUrl, description, icon, conformsTo,
            osTypes, mimeTypes, extensions) in typeDeclarations) {

        writeStartElement(DICT_TAG)
        writeCharacters("\n")

        writeProperty("UTTypeIdentifier", identifier)
        referenceUrl?.let {
            writeProperty("UTTypeReferenceURL", it)
        }
        description?.let {
            writeProperty("UTTypeDescription", it)
        }

        icon?.let {
            writeProperty("UTTypeIconFile", icon)
        }

        writeStringArray("UTTypeConformsTo", conformsTo)

        writeKey("UTTypeTagSpecification")

        writeStartElement(DICT_TAG)
        writeCharacters("\n")

        writeStringArray("com.apple.ostype", osTypes)
        writeStringArray("public.filename-extension", extensions)
        writeStringArray("public.mime-type", mimeTypes)

        writeEndElement()
        writeCharacters("\n")

        writeEndElement()
        writeCharacters("\n")
    }

    writeEndElement()
    writeCharacters("\n")
}
