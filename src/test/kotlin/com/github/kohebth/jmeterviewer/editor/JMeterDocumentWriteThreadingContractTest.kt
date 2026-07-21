package com.github.kohebth.jmeterviewer.editor

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class JMeterDocumentWriteThreadingContractTest {
    @Test
    fun autosaveReentersAWriteSafeIntellijContext() {
        val source = Files.readString(
            Path.of(
                "src/main/kotlin/com/github/kohebth/jmeterviewer/editor/" +
                    "JMeterWorkspaceService.kt",
            ),
        )

        assertTrue(
            source.contains(
                "Timer(FIELD_SNAPSHOT_DELAY_MS) { schedulePendingFieldEditFlush() }",
            ),
        )
        assertTrue(source.contains("application.invokeLater("))
        assertTrue(source.contains("ModalityState.defaultModalityState()"))
        assertFalse(
            source.contains(
                "Timer(FIELD_SNAPSHOT_DELAY_MS) { flushPendingFieldEdit() }",
            ),
        )
    }

    @Test
    fun cancellingAutosaveInvalidatesAnAlreadyQueuedFlush() {
        val source = Files.readString(
            Path.of(
                "src/main/kotlin/com/github/kohebth/jmeterviewer/editor/" +
                    "JMeterWorkspaceService.kt",
            ),
        )

        assertTrue(source.contains("val expectedGeneration = fieldSnapshotGeneration"))
        assertTrue(source.contains("if (expectedGeneration == fieldSnapshotGeneration)"))
        assertTrue(source.contains("fieldSnapshotGeneration++"))
    }
}
