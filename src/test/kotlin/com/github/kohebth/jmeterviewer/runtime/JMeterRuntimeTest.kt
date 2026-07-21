package com.github.kohebth.jmeterviewer.runtime

import org.apache.jmeter.save.SaveService
import org.apache.jmeter.util.JMeterUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class JMeterRuntimeTest {
    @Test
    fun initializesFromVendoredJMeterHome() {
        val home = Path.of("vendor/apache-jmeter-5.6.3")

        JMeterRuntime.initialize(home)

        assertEquals(home.toAbsolutePath().normalize().toString(), JMeterUtils.getJMeterHome())
        assertEquals("5.0", SaveService.getPropertiesVersion())
    }

    @Test
    fun buildsStableSearchPathFromBundledJMeterModules(@TempDir pluginLib: Path) {
        Files.createFile(pluginLib.resolve("ApacheJMeter_http-5.6.3.jar"))
        Files.createFile(pluginLib.resolve("ApacheJMeter_core-5.6.3.jar"))
        Files.createFile(pluginLib.resolve("commons-lang3-3.12.0.jar"))
        Files.createDirectory(pluginLib.resolve("ApacheJMeter_not-a-jar"))

        val searchPath = JMeterRuntime.bundledModuleSearchPath(pluginLib)

        assertEquals(
            listOf(
                pluginLib.resolve("ApacheJMeter_core-5.6.3.jar").toAbsolutePath().normalize().toString(),
                pluginLib.resolve("ApacheJMeter_http-5.6.3.jar").toAbsolutePath().normalize().toString(),
            ),
            searchPath.split(';'),
        )
        assertFalse(searchPath.contains("commons-lang3"))
    }
}
