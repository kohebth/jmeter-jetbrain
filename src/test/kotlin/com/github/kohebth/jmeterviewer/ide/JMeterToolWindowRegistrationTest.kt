package com.github.kohebth.jmeterviewer.ide

import com.github.kohebth.jmeterviewer.toolwindow.JMeterToolWindowFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JMeterToolWindowRegistrationTest {
    @Test
    fun registersTheBottomJMeterToolWindow() {
        val descriptor = checkNotNull(javaClass.getResource("/META-INF/plugin.xml")).readText()

        assertTrue(descriptor.contains("id=\"JMeter\""))
        assertTrue(descriptor.contains("anchor=\"bottom\""))
        assertTrue(descriptor.contains("icon=\"/icons/jmeter-tool-window.svg\""))
        assertEquals("JMeterToolWindowFactory", JMeterToolWindowFactory::class.java.simpleName)

        val icon = checkNotNull(javaClass.getResource("/icons/jmeter-tool-window.svg")).readText()
        assertTrue(icon.contains("width=\"13\""))
        assertTrue(icon.contains("height=\"13\""))
    }

    @Test
    fun registersAnIntellijRunConfigurationForJMeter() {
        val descriptor = checkNotNull(javaClass.getResource("/META-INF/plugin.xml")).readText()

        assertTrue(descriptor.contains("<configurationType"))
        assertTrue(descriptor.contains("JMeterRunConfigurationType"))
    }
}
