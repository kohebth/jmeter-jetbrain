package com.github.kohebth.jmeterviewer.editor

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

internal object DocumentFingerprint {
    private val hex = "0123456789abcdef".toCharArray()

    fun of(content: CharSequence): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(content.toString().toByteArray(StandardCharsets.UTF_8))
        val result = CharArray(digest.size * 2)
        digest.forEachIndexed { index, byte ->
            val value = byte.toInt() and 0xff
            result[index * 2] = hex[value ushr 4]
            result[index * 2 + 1] = hex[value and 0x0f]
        }
        return String(result)
    }
}

internal enum class DocumentSyncAction {
    NONE,
    RELOAD_DOCUMENT,
    SNAPSHOT_NATIVE,
    CONFLICT,
}

internal class DocumentSyncState(initialFingerprint: String) {
    var loadedFingerprint: String = initialFingerprint
        private set

    fun actionFor(documentFingerprint: String, nativeDirty: Boolean): DocumentSyncAction {
        val documentChanged = documentFingerprint != loadedFingerprint
        return when {
            documentChanged && nativeDirty -> DocumentSyncAction.CONFLICT
            documentChanged -> DocumentSyncAction.RELOAD_DOCUMENT
            nativeDirty -> DocumentSyncAction.SNAPSHOT_NATIVE
            else -> DocumentSyncAction.NONE
        }
    }

    fun accept(documentFingerprint: String) {
        loadedFingerprint = documentFingerprint
    }
}
