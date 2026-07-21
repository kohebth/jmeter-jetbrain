package com.github.kohebth.jmeterviewer.ide

import com.intellij.icons.AllIcons
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.fileEditor.FileEditorPolicy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test

class JMeterEditorRegistrationTest {
    @Test
    fun keepsJmxBackedByXmlLanguage() {
        assertEquals(XMLLanguage.INSTANCE, JMeterFileType.language)
        assertEquals("jmx", JMeterFileType.defaultExtension)
    }

    @Test
    fun usesTheNativeJMeterFeatherInTheProjectTree() {
        val icon = JMeterFileType.icon

        assertNotSame(AllIcons.FileTypes.Xml, icon)
        assertEquals(19, icon.iconWidth)
        assertEquals(19, icon.iconHeight)
    }

    @Test
    fun keepsTheRawXmlEditorNextToTheVisualEditor() {
        assertEquals(
            FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR,
            JMeterFileEditorProvider().policy,
        )
    }
}
