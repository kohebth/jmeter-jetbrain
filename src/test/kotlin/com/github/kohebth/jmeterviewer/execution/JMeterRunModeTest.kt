package com.github.kohebth.jmeterviewer.execution

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.api.Test

class JMeterRunModeTest {
    @ParameterizedTest
    @CsvSource(
        "run_tg,AS_IS",
        "run_tg_no_timers,IGNORE_TIMERS",
        "validate_tg,VALIDATE",
    )
    fun mapsNativeJMeterThreadGroupCommands(command: String, expected: String) {
        assertEquals(expected, JMeterRunMode.fromActionCommand(command)?.name)
    }

    @Test
    fun rejectsWholePlanAndUnknownCommands() {
        assertNull(JMeterRunMode.fromActionCommand("start"))
        assertNull(JMeterRunMode.fromActionCommand("unknown"))
    }
}
