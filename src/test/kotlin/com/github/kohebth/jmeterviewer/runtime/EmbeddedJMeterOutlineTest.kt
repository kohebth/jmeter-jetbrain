package com.github.kohebth.jmeterviewer.runtime

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class EmbeddedJMeterOutlineTest {
    @Test
    fun exposesTheAuthoritativeNativeTreeOnlyInTheToolWindowSurface() {
        val workspaceSource = Files.readString(
            Path.of(
                "vendor/apache-jmeter-5.6.3/src/core/src/main/java/" +
                    "org/apache/jmeter/gui/EmbeddedJMeterWorkspace.java",
            ),
        )
        val mainFrameSource = Files.readString(
            Path.of(
                "vendor/apache-jmeter-5.6.3/src/core/src/main/java/" +
                    "org/apache/jmeter/gui/MainFrame.java",
            ),
        )

        assertTrue(workspaceSource.contains("mainFrame.getEmbeddedTreeComponent()"))
        assertFalse(workspaceSource.contains("synchronizeTrees("))
        assertFalse(workspaceSource.contains("new JTree(authoringTree.getModel())"))
        assertTrue(mainFrameSource.contains("embeddedComponent = mainPanel;"))
        assertTrue(mainFrameSource.contains("getEmbeddedTreeComponent()"))
    }

    @Test
    fun doesNotAddAnExecutionToolbarAboveTheNativeJMeterEditor() {
        val source = Files.readString(
            Path.of(
                "vendor/apache-jmeter-5.6.3/src/core/src/main/java/" +
                    "org/apache/jmeter/gui/EmbeddedJMeterWorkspace.java",
            ),
        )
        assertTrue(source.contains("mainFrame.getEmbeddedComponent()"))
        assertFalse(source.contains("new JToolBar"))
        assertFalse(source.contains("createToolbarButton("))
    }
}
