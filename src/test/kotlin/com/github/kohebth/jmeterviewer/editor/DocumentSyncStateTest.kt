package com.github.kohebth.jmeterviewer.editor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DocumentSyncStateTest {
    @Test
    fun reloadsAChangedDocumentWhenNativeModelIsClean() {
        val state = DocumentSyncState("loaded")

        assertEquals(
            DocumentSyncAction.RELOAD_DOCUMENT,
            state.actionFor("changed", nativeDirty = false),
        )
    }

    @Test
    fun snapshotsANativeChangeWhenDocumentDidNotChange() {
        val state = DocumentSyncState("loaded")

        assertEquals(
            DocumentSyncAction.SNAPSHOT_NATIVE,
            state.actionFor("loaded", nativeDirty = true),
        )
    }

    @Test
    fun requiresAChoiceWhenBothSidesChanged() {
        val state = DocumentSyncState("loaded")

        assertEquals(
            DocumentSyncAction.CONFLICT,
            state.actionFor("changed", nativeDirty = true),
        )
    }

    @Test
    fun doesNothingWhenBothSidesAreClean() {
        val state = DocumentSyncState("loaded")

        assertEquals(
            DocumentSyncAction.NONE,
            state.actionFor("loaded", nativeDirty = false),
        )
    }
}
