package com.github.kohebth.jmeterviewer.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JMeterSettingsTest {
    @Test
    fun persistsTheConfiguredJMeterHome() {
        val settings = JMeterSettings()

        settings.loadState(JMeterSettings.SettingsState(" /opt/apache-jmeter-5.6.3 "))
        assertEquals("/opt/apache-jmeter-5.6.3", settings.jmeterHome)

        settings.jmeterHome = " /srv/jmeter "
        assertEquals("/srv/jmeter", settings.state.jmeterHome)
    }
}
