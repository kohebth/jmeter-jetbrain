package com.github.kohebth.jmeterviewer.editor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class DocumentFingerprintTest {
    @Test
    fun isStableForTheSameDocument() {
        val document = "<jmeterTestPlan version=\"1.2\"/>"

        assertEquals(
            DocumentFingerprint.of(document),
            DocumentFingerprint.of(document),
        )
    }

    @Test
    fun detectsRawXmlAndExternalChanges() {
        assertNotEquals(
            DocumentFingerprint.of("<jmeterTestPlan version=\"1.2\"/>"),
            DocumentFingerprint.of("<jmeterTestPlan version=\"1.3\"/>"),
        )
    }
}
