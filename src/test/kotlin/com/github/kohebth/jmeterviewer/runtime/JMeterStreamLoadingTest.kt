package com.github.kohebth.jmeterviewer.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path

class JMeterStreamLoadingTest {
    @Test
    fun loadsAPlanFromTheCurrentIdeDocumentThroughTheIsolatedRuntime() {
        val source = Files.readAllBytes(
            Path.of("vendor/apache-jmeter-5.6.3/xdocs/demos/SimpleTestPlan.jmx"),
        )

        ExternalJMeterTestSupport.openRuntime().use { runtime ->
            val tree = runtime.withContextClassLoader {
                val saveService = runtime.classLoader.loadClass("org.apache.jmeter.save.SaveService")
                runtime.invoke(
                    saveService.getMethod("loadTree", java.io.InputStream::class.java),
                    null,
                    ByteArrayInputStream(source),
                )
            }

            assertNotNull(tree)
            val roots = runtime.withContextClassLoader {
                @Suppress("UNCHECKED_CAST")
                runtime.invoke(tree!!.javaClass.getMethod("getArray"), tree) as Array<Any?>
            }
            assertEquals("org.apache.jmeter.testelement.TestPlan", roots.first()!!.javaClass.name)
            assertEquals(runtime.classLoader, roots.first()!!.javaClass.classLoader)
        }
    }
}
