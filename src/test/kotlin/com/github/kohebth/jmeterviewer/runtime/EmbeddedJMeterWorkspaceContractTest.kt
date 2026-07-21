package com.github.kohebth.jmeterviewer.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.nio.file.Path
import javax.swing.JComponent

class EmbeddedJMeterWorkspaceContractTest {
    @Test
    fun exposesTheSmallEmbeddingBoundaryUsedByTheIdeFromTheBridge() {
        ExternalJMeterTestSupport.openRuntime().use { runtime ->
            val api = runtime.classLoader.loadClass(
                "org.apache.jmeter.gui.EmbeddedJMeterWorkspace",
            )

            assertEquals(JComponent::class.java, api.getMethod("getComponent").returnType)
            assertEquals(
                Void.TYPE,
                api.getMethod("load", InputStream::class.java, Path::class.java).returnType,
            )
            assertEquals(ByteArray::class.java, api.getMethod("snapshot").returnType)
            assertEquals(Boolean::class.javaPrimitiveType, api.getMethod("isDirty").returnType)
            assertEquals(Void.TYPE, api.getMethod("markSaved").returnType)
            assertEquals(Void.TYPE, api.getMethod("close").returnType)

            val mainFrame = runtime.classLoader.loadClass("org.apache.jmeter.gui.MainFrame")
            assertEquals(
                Boolean::class.javaPrimitiveType,
                mainFrame.getMethod("isEmbeddedMode").returnType,
            )

            val source = Path.of(api.protectionDomain.codeSource.location.toURI())
                .toAbsolutePath()
                .normalize()
            assertEquals(ExternalJMeterTestSupport.bridge, source)
        }
    }
}
