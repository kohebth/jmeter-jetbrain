package com.github.kohebth.jmeterviewer.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.assertThrows
import java.awt.Component
import java.awt.Container
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.text.JTextComponent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class JMeterSwingClassLoaderTest {
    @TempDir
    lateinit var tempDirectory: Path

    @Test
    fun runtimeLoaderOnlyExposesHostSwingUiDelegates() {
        openRuntime().use { runtime ->
            assertSame(
                HostTreeUI::class.java,
                runtime.classLoader.loadClass(HostTreeUI::class.java.name),
            )
            assertThrows<ClassNotFoundException> {
                runtime.classLoader.loadClass(JMeterSwingClassLoaderTest::class.java.name)
            }
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "JMETER_GUI_SMOKE", matches = "true")
    fun opensAndReopensAComplexPlanWithHostLookAndFeelAndRecordedResults() {
        openRuntime().use { runtime ->
            withHostTreeUi {
                val workspace = onEdt(runtime::createWorkspace)
                try {
                    val fixture = resourcePath(COMPLEX_PLAN_RESOURCE)
                    val resultsTree = onEdt {
                        Files.newInputStream(fixture).use { input ->
                            workspace.load(input, fixture)
                        }

                        val outlineTree = descendants<JTree>(workspace.outlineComponent).single()
                        assertSame(runtime.classLoader, outlineTree.javaClass.classLoader)
                        assertSame(HostTreeUI::class.java, outlineTree.ui.javaClass)
                        assertComplexPlan(workspace)

                        val reopened = tempDirectory.resolve("reopened-complex-localhost-plan.jmx")
                        Files.write(reopened, workspace.snapshot())
                        Files.newInputStream(reopened).use { input ->
                            workspace.load(input, reopened)
                        }
                        assertComplexPlan(workspace)

                        val component = workspace.resultsTreeComponent(RESULT_SESSION)
                        val recordedSample = requireNotNull(
                            javaClass.getResourceAsStream(RECORDED_SAMPLE_RESOURCE),
                        ).use { it.readBytes() }
                        workspace.appendSampleResult(RESULT_SESSION, recordedSample)
                        component
                    }

                    val (tree, sampleNode) = awaitRecordedSample(resultsTree)
                    onEdt {
                        tree.selectionPath = TreePath(sampleNode.path)
                        val renderedText = descendants<JTextComponent>(resultsTree)
                            .joinToString("\n") { it.text }
                        listOf(
                            "GET http://127.0.0.1:18080/recorded",
                            "X-Smoke-Request: request-42",
                            "X-Smoke-Response: response-42",
                            "{\"status\":\"recorded\",\"requestId\":\"request-42\"}",
                        ).forEach { expected ->
                            assertTrue(
                                renderedText.contains(expected),
                                "Results Tree did not render: $expected",
                            )
                        }
                    }
                } finally {
                    onEdt(workspace::close)
                }
            }
        }
    }

    private fun assertComplexPlan(workspace: JMeterWorkspace) {
        val expectedNames = listOf(
            "Shared CSV Users",
            "01 Authentication Thread Group",
            "02 Catalog Thread Group",
            "03 Observability Thread Group",
            "Login Request",
            "Profile Request",
            "Catalog Request",
            "Product Request",
            "Health Request",
            "Metrics Request",
            "Build Login Correlation ID",
            "Extract Login Token",
            "Attach Profile Region",
            "Capture Profile Title",
            "Build Catalog Query",
            "Extract First Product",
            "Attach Product Correlation ID",
            "Record Product Status",
            "Prepare Health Probe",
            "Extract Health Status",
            "Prepare Metrics Probe",
            "Record Metrics Response",
        )
        expectedNames.forEach { name ->
            assertTrue(
                workspace.searchTestPlan(name, caseSensitive = true, regexp = false)
                    .any { it.name == name },
                "Native workspace did not open $name",
            )
        }

        val threadGroups = workspace.searchTestPlan(
            "Thread Group",
            caseSensitive = true,
            regexp = false,
        ).filter { it.name.matches(Regex("0[1-3] .+ Thread Group")) }
        assertEquals(3, threadGroups.size)

        val product = workspace.searchTestPlan(
            "Product Request",
            caseSensitive = true,
            regexp = false,
        ).single { it.name == "Product Request" }
        assertTrue(product.breadcrumb.contains("Browse Catalog Transaction"))
    }

    private fun awaitRecordedSample(resultsTree: JComponent): Pair<JTree, DefaultMutableTreeNode> {
        val deadline = System.nanoTime() + RESULT_TIMEOUT_NANOS
        while (System.nanoTime() < deadline) {
            val result = onEdt {
                descendants<JTree>(resultsTree).firstNotNullOfOrNull { tree ->
                    val root = tree.model.root
                    (0 until tree.model.getChildCount(root))
                        .map { tree.model.getChild(root, it) }
                        .filterIsInstance<DefaultMutableTreeNode>()
                        .firstOrNull { it.toString() == RECORDED_SAMPLE_LABEL }
                        ?.let { tree to it }
                }
            }
            if (result != null) {
                return result
            }
            Thread.sleep(25)
        }
        throw AssertionError("Results Tree did not receive $RECORDED_SAMPLE_LABEL")
    }

    private inline fun <reified T : Component> descendants(root: Component): List<T> {
        val matches = mutableListOf<T>()
        val pending = java.util.ArrayDeque<Component>()
        pending.add(root)
        while (pending.isNotEmpty()) {
            val component = pending.removeFirst()
            if (component is T) {
                matches.add(component)
            }
            if (component is Container) {
                component.components.forEach(pending::addLast)
            }
        }
        return matches
    }

    private fun resourcePath(resource: String): Path =
        Path.of(requireNotNull(javaClass.getResource(resource)).toURI())

    private fun <T> withHostTreeUi(action: () -> T): T {
        val previous = onEdt {
            val defaults = UIManager.getDefaults()
            UiDefaultsState(
                hadTreeUi = defaults.containsKey("TreeUI"),
                treeUi = defaults["TreeUI"],
                hadClassLoader = defaults.containsKey("ClassLoader"),
                classLoader = defaults["ClassLoader"],
            ).also {
                defaults["TreeUI"] = HostTreeUI::class.java.name
                defaults.remove("ClassLoader")
            }
        }
        return try {
            action()
        } finally {
            onEdt {
                val defaults = UIManager.getDefaults()
                restoreDefault(defaults, "TreeUI", previous.hadTreeUi, previous.treeUi)
                restoreDefault(
                    defaults,
                    "ClassLoader",
                    previous.hadClassLoader,
                    previous.classLoader,
                )
            }
        }
    }

    private fun restoreDefault(
        defaults: javax.swing.UIDefaults,
        key: String,
        existed: Boolean,
        value: Any?,
    ) {
        if (existed) {
            defaults[key] = requireNotNull(value)
        } else {
            defaults.remove(key)
        }
    }

    private fun openRuntime(): JMeterRuntime = JMeterRuntime.open(
        installation = JMeterInstallation.validate(ExternalJMeterTestSupport.home),
        bridgeJar = ExternalJMeterTestSupport.bridge,
        hostClassLoader = HostTreeUI::class.java.classLoader,
    )

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

    private companion object {
        const val COMPLEX_PLAN_RESOURCE = "/jmx/smoke/complex-localhost-plan.jmx"
        const val RECORDED_SAMPLE_RESOURCE = "/jmx/smoke/recorded-sample.xml"
        const val RECORDED_SAMPLE_LABEL = "Recorded Localhost Request"
        const val RESULT_SESSION = "complex-localhost-smoke"
        const val RESULT_TIMEOUT_NANOS = 5_000_000_000L
    }

    private data class UiDefaultsState(
        val hadTreeUi: Boolean,
        val treeUi: Any?,
        val hadClassLoader: Boolean,
        val classLoader: Any?,
    )
}
