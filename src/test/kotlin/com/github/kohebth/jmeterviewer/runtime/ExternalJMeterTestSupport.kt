package com.github.kohebth.jmeterviewer.runtime

import java.nio.file.Path

internal object ExternalJMeterTestSupport {
    val home: Path
        get() = requiredPath("jmeter.test.home")

    val bridge: Path
        get() = requiredPath("jmeter.bridge.jar")

    fun openRuntime(): JMeterRuntime = JMeterRuntime.open(
        JMeterInstallation.validate(home),
        bridge,
    )

    private fun requiredPath(property: String): Path {
        val value = System.getProperty(property)
            ?: error("Missing test system property: $property")
        return Path.of(value).toAbsolutePath().normalize()
    }
}
