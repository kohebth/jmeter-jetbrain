package com.github.kohebth.jmeterviewer.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test
import java.awt.Component
import java.awt.Container
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JComponent
import javax.swing.JToolBar
import javax.swing.JTree
import javax.swing.SwingUtilities

class EmbeddedJMeterOutlineTest {
    @Test
    fun keepsAuthoringAndOutlineTreeGeometryIndependentWhileSynchronizingSelection() {
        ExternalJMeterTestSupport.openRuntime().use { runtime ->
            onEdt {
                runtime.withContextClassLoader {
                    val workspaceType = runtime.classLoader.loadClass(
                        "org.apache.jmeter.gui.EmbeddedJMeterWorkspace",
                    )
                    val workspace = workspaceType.getMethod("create").invoke(null)
                    try {
                        val authoringRoot = workspaceType.getMethod("getComponent")
                            .invoke(workspace) as JComponent
                        val outlineRoot = workspaceType.getMethod("getOutlineComponent")
                            .invoke(workspace) as JComponent
                        val authoringTree = descendants(authoringRoot).filterIsInstance<JTree>().single()
                        val outlineTree = descendants(outlineRoot).filterIsInstance<JTree>().single()

                        assertNotSame(authoringTree.selectionModel, outlineTree.selectionModel)

                        authoringTree.setSelectionRow(1)
                        assertEquals(authoringTree.selectionPath, outlineTree.selectionPath)

                        outlineTree.setSelectionRow(0)
                        assertEquals(outlineTree.selectionPath, authoringTree.selectionPath)
                    } finally {
                        workspaceType.getMethod("close").invoke(workspace)
                    }
                }
            }
        }
    }

    @Test
    fun doesNotAddAnExecutionToolbarAboveTheNativeJMeterEditor() {
        ExternalJMeterTestSupport.openRuntime().use { runtime ->
            onEdt {
                runtime.withContextClassLoader {
                    val workspaceType = runtime.classLoader.loadClass(
                        "org.apache.jmeter.gui.EmbeddedJMeterWorkspace",
                    )
                    val workspace = workspaceType.getMethod("create").invoke(null)
                    try {
                        val component = workspaceType.getMethod("getComponent")
                            .invoke(workspace) as JComponent

                        assertFalse(descendants(component).any { it is JToolBar })
                    } finally {
                        workspaceType.getMethod("close").invoke(workspace)
                    }
                }
            }
        }
    }

    private fun descendants(root: Component): Sequence<Component> = sequence {
        yield(root)
        if (root is Container) {
            root.components.forEach { child -> yieldAll(descendants(child)) }
        }
    }

    private fun <T> onEdt(action: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) {
            return action()
        }
        val result = AtomicReference<T>()
        val failure = AtomicReference<Throwable?>()
        SwingUtilities.invokeAndWait {
            try {
                result.set(action())
            } catch (throwable: Throwable) {
                failure.set(throwable)
            }
        }
        failure.get()?.let { throw it }
        return result.get()
    }
}
