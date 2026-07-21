package com.github.kohebth.jmeterviewer.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import javax.swing.ImageIcon
import javax.xml.parsers.SAXParserFactory

class JMeterRuntimeTest {
    @Test
    fun initializesFromTheExternalJMeterHome() {
        ExternalJMeterTestSupport.openRuntime().use { runtime ->
            val utils = runtime.classLoader.loadClass("org.apache.jmeter.util.JMeterUtils")
            val saveService = runtime.classLoader.loadClass("org.apache.jmeter.save.SaveService")

            assertEquals(
                ExternalJMeterTestSupport.home.toString(),
                runtime.withContextClassLoader {
                    runtime.invoke(utils.getMethod("getJMeterHome"), null)
                },
            )
            assertEquals(
                JMeterInstallation.SUPPORTED_VERSION,
                runtime.withContextClassLoader {
                    runtime.invoke(utils.getMethod("getJMeterVersion"), null)
                },
            )
            assertEquals(
                "5.0",
                runtime.withContextClassLoader {
                    runtime.invoke(saveService.getMethod("getPropertiesVersion"), null)
                },
            )
        }
    }

    @Test
    fun isolatesJMeterAndItsXmlProviderFromThePluginClassloader() {
        ExternalJMeterTestSupport.openRuntime().use { runtime ->
            assertSame(ClassLoader.getPlatformClassLoader(), runtime.classLoader.parent)
            assertThrows<ClassNotFoundException> {
                runtime.classLoader.loadClass(JMeterRuntime::class.java.name)
            }

            val loggerFactory = runtime.classLoader.loadClass("org.slf4j.LoggerFactory")
            assertSame(runtime.classLoader, loggerFactory.classLoader)
            assertNotSame(JMeterRuntime::class.java.classLoader, loggerFactory.classLoader)

            val xercesFactory = runtime.classLoader.loadClass(
                "org.apache.xerces.jaxp.SAXParserFactoryImpl",
            )
            assertSame(runtime.classLoader, xercesFactory.classLoader)
            assertTrue(SAXParserFactory::class.java.isAssignableFrom(xercesFactory))
        }
    }

    @Test
    fun installsNativeElementIconsFromTheExternalJMeterConfiguration() {
        ExternalJMeterTestSupport.openRuntime().use { runtime ->
            val guiFactory = runtime.classLoader.loadClass("org.apache.jmeter.gui.GUIFactory")
            val getIcon = guiFactory.getMethod(
                "getIcon",
                Class::class.java,
                Boolean::class.javaPrimitiveType,
            )
            val categories = mapOf(
                "Thread Group" to "org.apache.jmeter.threads.gui.ThreadGroupGui",
                "Config Element" to "org.apache.jmeter.config.gui.AbstractConfigGui",
                "Sampler" to "org.apache.jmeter.samplers.gui.AbstractSamplerGui",
            )

            categories.forEach { (label, className) ->
                val guiClass = runtime.classLoader.loadClass(className)
                val enabled = runtime.withContextClassLoader {
                    runtime.invoke(getIcon, null, guiClass, true)
                } as? ImageIcon
                val disabled = runtime.withContextClassLoader {
                    runtime.invoke(getIcon, null, guiClass, false)
                } as? ImageIcon

                assertAll(
                    { assertNotNull(enabled, "$label enabled icon") },
                    { assertNotNull(disabled, "$label disabled icon") },
                    { assertEquals(19, enabled?.iconWidth, "$label enabled icon width") },
                    { assertEquals(19, enabled?.iconHeight, "$label enabled icon height") },
                    { assertEquals(19, disabled?.iconWidth, "$label disabled icon width") },
                    { assertEquals(19, disabled?.iconHeight, "$label disabled icon height") },
                )
            }
        }
    }

    @Test
    fun restoresTheCallingThreadsContextClassloader() {
        ExternalJMeterTestSupport.openRuntime().use { runtime ->
            val thread = Thread.currentThread()
            val original = thread.contextClassLoader

            runtime.withContextClassLoader {
                assertSame(runtime.classLoader, thread.contextClassLoader)
            }

            assertSame(original, thread.contextClassLoader)
        }
    }
}
