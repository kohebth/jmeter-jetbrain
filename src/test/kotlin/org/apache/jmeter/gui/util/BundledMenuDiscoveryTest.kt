package org.apache.jmeter.gui.util

import com.github.kohebth.jmeterviewer.runtime.JMeterRuntime
import org.apache.jmeter.util.JMeterUtils
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.nio.file.Path

class BundledMenuDiscoveryTest {
    @Test
    fun discoversNativeAddMenusAcrossBundledModules() {
        val classes = MenuFactory.getMenuMap().values
            .flatten()
            .map(MenuInfo::getClassName)
            .toSet()

        assertTrue(classes.contains("org.apache.jmeter.threads.gui.ThreadGroupGui"))
        assertTrue(classes.contains("org.apache.jmeter.assertions.gui.DurationAssertionGui"))
        assertTrue(classes.contains("org.apache.jmeter.extractor.json.jsonpath.gui.JSONPostProcessorGui"))
        assertTrue(classes.contains("org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui"))
    }

    @Test
    fun everyCoreAuthoringCategoryHasNativeEntries() {
        val menus = MenuFactory.getMenuMap()

        listOf(
            MenuFactory.THREADS,
            MenuFactory.CONTROLLERS,
            MenuFactory.SAMPLERS,
            MenuFactory.CONFIG_ELEMENTS,
            MenuFactory.PRE_PROCESSORS,
            MenuFactory.POST_PROCESSORS,
            MenuFactory.ASSERTIONS,
            MenuFactory.TIMERS,
            MenuFactory.LISTENERS,
        ).forEach { category ->
            assertFalse(menus.getValue(category).isEmpty(), "Empty JMeter menu category: $category")
        }
    }

    @Test
    fun embeddedPopupLeavesFileOwnershipToTheHost() {
        assertFalse(MenuFactory.includesStandaloneFileActions(true))
        assertTrue(MenuFactory.includesStandaloneFileActions(false))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun configureNativeDiscovery() {
            JMeterRuntime.initialize(Path.of("vendor/apache-jmeter-5.6.3"))
            val libraryDirectory = Path.of(
                "build/idea-sandbox/plugins-test/jmeter-jetbrains-plugin/lib",
            )
            val searchPath = JMeterRuntime.bundledModuleSearchPath(libraryDirectory)
            assertTrue(searchPath.isNotEmpty(), "Testing sandbox has no ApacheJMeter modules")
            JMeterUtils.setProperty("search_paths", searchPath)
            Thread.currentThread().contextClassLoader = BundledMenuDiscoveryTest::class.java.classLoader
        }
    }
}
