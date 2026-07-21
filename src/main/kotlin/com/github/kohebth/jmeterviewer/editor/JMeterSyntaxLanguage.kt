package com.github.kohebth.jmeterviewer.editor

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.UnknownFileType
import java.util.Locale

internal data class JMeterEditorLanguage(
    val extension: String,
    val displayName: String,
    val fileType: FileType,
) {
    override fun toString(): String = displayName
}

internal object JMeterSyntaxLanguage {
    private val extensionsByStyle = mapOf(
        "application/json" to "json",
        "application/javascript" to "js",
        "application/xml" to "xml",
        "text/groovy" to "groovy",
        "text/java" to "java",
        "text/javascript" to "js",
        "text/json" to "json",
        "text/python" to "py",
        "text/sql" to "sql",
        "text/xml" to "xml",
    )

    fun preferredExtension(syntaxStyle: String?): String? = syntaxStyle
        ?.trim()
        ?.lowercase(Locale.ROOT)
        ?.takeIf(String::isNotEmpty)
        ?.let(extensionsByStyle::get)

    fun installedLanguages(): List<JMeterEditorLanguage> {
        val manager = FileTypeManager.getInstance()
        val extensions = linkedSetOf("txt")
        extensions.addAll(extensionsByStyle.values)
        return extensions.mapNotNull { extension ->
            val fileType = if (extension == "txt") {
                PlainTextFileType.INSTANCE
            } else {
                manager.getFileTypeByExtension(extension)
            }
            if (fileType is UnknownFileType) {
                null
            } else {
                JMeterEditorLanguage(extension, fileType.description, fileType)
            }
        }.distinctBy { it.fileType.name }
    }

    fun resolve(syntaxStyle: String?, overrideExtension: String?): JMeterEditorLanguage {
        val languages = installedLanguages()
        val extension = overrideExtension?.takeIf(String::isNotBlank)
            ?: preferredExtension(syntaxStyle)
        return languages.firstOrNull { it.extension.equals(extension, ignoreCase = true) }
            ?: languages.firstOrNull { it.extension == "txt" }
            ?: JMeterEditorLanguage("txt", PlainTextFileType.INSTANCE.description, PlainTextFileType.INSTANCE)
    }
}
