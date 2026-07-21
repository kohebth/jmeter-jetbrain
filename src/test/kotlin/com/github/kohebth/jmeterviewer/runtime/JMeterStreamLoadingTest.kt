package com.github.kohebth.jmeterviewer.runtime

import org.apache.jmeter.save.SaveService
import org.apache.jmeter.testelement.TestPlan
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path

class JMeterStreamLoadingTest {
    @Test
    fun loadsAPlanFromTheCurrentIdeDocument() {
        val source = Files.readAllBytes(
            Path.of("vendor/apache-jmeter-5.6.3/xdocs/demos/SimpleTestPlan.jmx"),
        )

        val tree = SaveService.loadTree(ByteArrayInputStream(source))

        val root = tree.array.firstOrNull()
        assertNotNull(root)
        assertInstanceOf(TestPlan::class.java, root)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun initializeJMeter() {
            JMeterRuntime.initialize(Path.of("vendor/apache-jmeter-5.6.3"))
        }
    }
}
