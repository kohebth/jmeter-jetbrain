package com.github.kohebth.jmeterviewer.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentSynchronizationVetoer
import com.intellij.openapi.vfs.VirtualFile

class JMeterDocumentSynchronizationVetoer : FileDocumentSynchronizationVetoer() {
    override fun maySaveDocument(document: Document, isSaveExplicit: Boolean): Boolean =
        workspaceIfCreated()?.maySaveDocument(document) ?: true

    override fun mayReloadFileContent(file: VirtualFile, document: Document): Boolean =
        workspaceIfCreated()?.mayReloadFile(file, document) ?: true

    private fun workspaceIfCreated(): JMeterWorkspaceService? =
        ApplicationManager.getApplication().getServiceIfCreated(JMeterWorkspaceService::class.java)
}
