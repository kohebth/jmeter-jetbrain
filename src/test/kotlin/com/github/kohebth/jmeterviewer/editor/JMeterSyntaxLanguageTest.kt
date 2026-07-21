package com.github.kohebth.jmeterviewer.editor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.api.Test

class JMeterSyntaxLanguageTest {
    @ParameterizedTest
    @CsvSource(
        "text/groovy,groovy",
        "text/java,java",
        "text/javascript,js",
        "text/python,py",
        "text/sql,sql",
        "text/json,json",
        "text/xml,xml",
        "application/json,json",
    )
    fun mapsJMeterSyntaxStylesToIdeFileExtensions(style: String, extension: String) {
        assertEquals(extension, JMeterSyntaxLanguage.preferredExtension(style))
    }

    @Test
    fun leavesUnknownOrMissingStylesAsPlainText() {
        assertNull(JMeterSyntaxLanguage.preferredExtension(null))
        assertNull(JMeterSyntaxLanguage.preferredExtension(""))
        assertNull(JMeterSyntaxLanguage.preferredExtension("text/x-custom"))
    }
}
