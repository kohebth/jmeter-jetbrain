package com.github.kohebth.jmeterviewer.runtime

import org.apache.jmeter.gui.EmbeddedJMeterWorkspace
import org.apache.jmeter.gui.MainFrame
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.nio.file.Path
import javax.swing.JComponent

class EmbeddedJMeterWorkspaceContractTest {
    @Test
    fun exposesTheSmallEmbeddingBoundaryUsedByTheIde() {
        val api = EmbeddedJMeterWorkspace::class.java

        assertEquals(JComponent::class.java, api.getMethod("getComponent").returnType)
        assertEquals(
            Void.TYPE,
            api.getMethod("load", InputStream::class.java, Path::class.java).returnType,
        )
        assertEquals(ByteArray::class.java, api.getMethod("snapshot").returnType)
        assertEquals(Boolean::class.javaPrimitiveType, api.getMethod("isDirty").returnType)
        assertEquals(Void.TYPE, api.getMethod("markSaved").returnType)
        assertEquals(Void.TYPE, api.getMethod("close").returnType)
        assertEquals(Boolean::class.javaPrimitiveType, MainFrame::class.java.getMethod("isEmbeddedMode").returnType)
    }
}
