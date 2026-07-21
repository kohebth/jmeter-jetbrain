package com.github.kohebth.jmeterviewer.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class EmbeddedJMeterOutlineTest {
    @Test
    fun keepsAuthoringAndOutlineTreeGeometryIndependentWhileSynchronizingSelection() {
        ExternalJMeterTestSupport.openRuntime().use { runtime ->
            runtime.withContextClassLoader {
                val root = DefaultMutableTreeNode("root")
                val child = DefaultMutableTreeNode("child")
                root.add(child)
                val model = DefaultTreeModel(root)
                val authoringTree = JTree(model)
                val outlineTree = JTree(model)
                val workspaceType = runtime.classLoader.loadClass(
                    "org.apache.jmeter.gui.EmbeddedJMeterWorkspace",
                )
                val synchronize = workspaceType.getDeclaredMethod(
                    "synchronizeTrees",
                    JTree::class.java,
                    JTree::class.java,
                ).apply { isAccessible = true }
                synchronize.invoke(null, authoringTree, outlineTree)

                assertNotSame(authoringTree.selectionModel, outlineTree.selectionModel)
                val childPath = TreePath(child.path)
                authoringTree.selectionPath = childPath
                assertEquals(childPath, outlineTree.selectionPath)

                outlineTree.selectionPath = TreePath(root.path)
                assertEquals(outlineTree.selectionPath, authoringTree.selectionPath)
            }
        }
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
