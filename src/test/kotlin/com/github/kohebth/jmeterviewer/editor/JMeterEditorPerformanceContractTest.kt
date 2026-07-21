package com.github.kohebth.jmeterviewer.editor

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import javax.swing.Timer

class JMeterEditorPerformanceContractTest {
    @Test
    fun textareaDiscoveryDoesNotUseARepeatingSwingTimer() {
        val timerFields = JMeterTextAreaAdapters::class.java.declaredFields.filter {
            Timer::class.java.isAssignableFrom(it.type)
        }

        assertFalse(timerFields.any { it.name == "scanTimer" })
    }

    @Test
    fun modifiedStateDoesNotUseARepeatingSwingTimer() {
        val timerFields = JMeterVisualFileEditor::class.java.declaredFields.filter {
            Timer::class.java.isAssignableFrom(it.type)
        }

        assertFalse(timerFields.any { it.name == "modifiedPoller" })
    }
}
