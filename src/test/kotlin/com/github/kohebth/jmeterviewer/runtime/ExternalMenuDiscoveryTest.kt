package com.github.kohebth.jmeterviewer.runtime

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ExternalMenuDiscoveryTest {
    @Test
    fun discoversNativeAddMenusAcrossTheExternalInstallation() {
        val classes = menuMap().values
            .flatten()
            .mapNotNull { item ->
                val getter = item.javaClass.methods.firstOrNull { method ->
                    method.name == "getClassName" && method.parameterCount == 0
                } ?: return@mapNotNull null
                runtime.invoke(getter, item) as? String
            }
            .toSet()

        assertTrue(classes.contains("org.apache.jmeter.threads.gui.ThreadGroupGui"))
        assertTrue(classes.contains("org.apache.jmeter.assertions.gui.DurationAssertionGui"))
        assertTrue(classes.contains("org.apache.jmeter.extractor.json.jsonpath.gui.JSONPostProcessorGui"))
        assertTrue(classes.contains("org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui"))
        assertTrue(classes.contains("com.github.kohebth.jmeterviewer.testplugin.ExternalSampler"))
    }

    @Test
    fun everyCoreAuthoringCategoryHasNativeEntries() {
        val menus = menuMap()

        listOf(
            "THREADS",
            "CONTROLLERS",
            "SAMPLERS",
            "CONFIG_ELEMENTS",
            "PRE_PROCESSORS",
            "POST_PROCESSORS",
            "ASSERTIONS",
            "TIMERS",
            "LISTENERS",
        ).forEach { fieldName ->
            val category = menuFactory.getField(fieldName).get(null) as String
            assertFalse(menus.getValue(category).isEmpty(), "Empty JMeter menu category: $category")
        }
    }

    @Test
    fun embeddedPopupLeavesFileOwnershipToTheHost() {
        val method = menuFactory.getDeclaredMethod(
            "includesStandaloneFileActions",
            Boolean::class.javaPrimitiveType,
        ).apply { isAccessible = true }

        runtime.withContextClassLoader {
            assertFalse(runtime.invoke(method, null, true) as Boolean)
            assertTrue(runtime.invoke(method, null, false) as Boolean)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun menuMap(): Map<String, List<Any>> = runtime.withContextClassLoader {
        val method = menuFactory.getDeclaredMethod("getMenuMap").apply { isAccessible = true }
        runtime.invoke(method, null) as Map<String, List<Any>>
    }

    companion object {
        private lateinit var runtime: JMeterRuntime
        private lateinit var menuFactory: Class<*>

        @JvmStatic
        @BeforeAll
        fun initializeExternalJMeter() {
            runtime = ExternalJMeterTestSupport.openRuntime()
            menuFactory = runtime.classLoader.loadClass("org.apache.jmeter.gui.util.MenuFactory")
        }

        @JvmStatic
        @AfterAll
        fun closeExternalJMeter() {
            runtime.close()
        }
    }
}
