package com.github.kohebth.jmeterviewer.editor

import com.github.kohebth.jmeterviewer.runtime.JMeterConfigurationException
import com.github.kohebth.jmeterviewer.runtime.JMeterRuntimeService
import com.github.kohebth.jmeterviewer.runtime.JMeterWorkspace
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JComponent

@Service(Service.Level.APP)
class JMeterWorkspaceService : Disposable {
    private val fileDocumentManager = FileDocumentManager.getInstance()
    private var workspace: JMeterWorkspace? = null
    private var loadedFile: VirtualFile? = null
    private var syncState: DocumentSyncState? = null
    private var activeEditor: JMeterVisualFileEditor? = null
    private var switchBlocked = false
    private var internalDocumentSave = false
    private var reloadAfterExternalChange = false
    private var disposed = false

    fun activate(editor: JMeterVisualFileEditor) = onEdt {
        if (disposed || editor.project.isDisposed || !editor.virtualFile.isValid) {
            return@onEdt
        }

        val previous = activeEditor
        if (previous != null && previous !== editor) {
            if (switchBlocked || !persist(previous, saveToDisk = true)) {
                switchBlocked = true
                editor.showSwitchBlocked()
                previous.requestReselect()
                return@onEdt
            }
            detach(previous)
            activeEditor = null
        }

        switchBlocked = false
        val nativeWorkspace = try {
            ensureWorkspace()
        } catch (failure: JMeterConfigurationException) {
            editor.showLoadError(readableMessage(failure), offerConfiguration = true)
            return@onEdt
        } catch (failure: Exception) {
            editor.showLoadError(readableMessage(failure))
            return@onEdt
        } catch (failure: LinkageError) {
            editor.showLoadError(readableMessage(failure))
            return@onEdt
        }
        val documentFingerprint = DocumentFingerprint.of(editor.document.immutableCharSequence)

        try {
            when {
                loadedFile != editor.virtualFile -> loadDocument(editor, nativeWorkspace)
                syncState == null -> loadDocument(editor, nativeWorkspace)
                documentFingerprint != syncState!!.loadedFingerprint && nativeIsDirty(nativeWorkspace) -> {
                    when (askConflict(editor)) {
                        ConflictChoice.RELOAD_DOCUMENT -> loadDocument(editor, nativeWorkspace)
                        ConflictChoice.OVERWRITE_WITH_VISUAL -> snapshotIntoDocument(editor, nativeWorkspace)
                        ConflictChoice.CANCEL -> {
                            editor.showLoadError("The visual and text editors both contain unsaved changes.")
                            return@onEdt
                        }
                    }
                }
                documentFingerprint != syncState!!.loadedFingerprint -> loadDocument(editor, nativeWorkspace)
            }
        } catch (failure: Exception) {
            editor.showLoadError(readableMessage(failure))
            return@onEdt
        } catch (failure: LinkageError) {
            editor.showLoadError(readableMessage(failure))
            return@onEdt
        }

        attach(editor, nativeWorkspace.component)
        nativeWorkspace.setDialogParent(editor.component)
        activeEditor = editor
    }

    fun retry(editor: JMeterVisualFileEditor) {
        activate(editor)
    }

    fun deactivate(editor: JMeterVisualFileEditor) = onEdt {
        if (activeEditor !== editor) {
            return@onEdt
        }

        if (!persist(editor, saveToDisk = true)) {
            switchBlocked = true
            editor.requestReselect()
            return@onEdt
        }

        detach(editor)
        activeEditor = null
        workspace?.setDialogParent(null)
        switchBlocked = false
    }

    fun unregister(editor: JMeterVisualFileEditor) = onEdt {
        if (activeEditor !== editor) {
            return@onEdt
        }

        if (!editor.project.isDisposed && editor.virtualFile.isValid) {
            persist(editor, saveToDisk = true)
        }
        detach(editor)
        activeEditor = null
        workspace?.setDialogParent(null)
        switchBlocked = false
    }

    fun isModified(editor: JMeterVisualFileEditor): Boolean = onEdt {
        val documentUnsaved = fileDocumentManager.isDocumentUnsaved(editor.document)
        if (activeEditor !== editor) {
            return@onEdt documentUnsaved
        }

        documentUnsaved || (workspace?.let(::nativeIsDirty) ?: false)
    }

    /** Called by IntelliJ before explicit Save, Save All, and autosave. */
    fun maySaveDocument(document: com.intellij.openapi.editor.Document): Boolean = onEdt {
        if (internalDocumentSave) {
            return@onEdt true
        }
        val editor = activeEditor
        if (editor == null || editor.document !== document) {
            return@onEdt true
        }

        val synchronized = synchronizeDocument(editor)
        if (synchronized) {
            scheduleSavedBaseline(editor)
        } else {
            switchBlocked = true
            editor.requestReselect()
        }
        synchronized
    }

    /** Called before IntelliJ replaces a Document after an external file change. */
    fun mayReloadFile(file: VirtualFile, document: com.intellij.openapi.editor.Document): Boolean = onEdt {
        val editor = activeEditor
        val nativeWorkspace = workspace
        if (editor == null || nativeWorkspace == null || editor.virtualFile != file || editor.document !== document) {
            return@onEdt true
        }
        try {
            if (!nativeIsDirty(nativeWorkspace)) {
                reloadAfterExternalChange = true
                return@onEdt true
            }

            when (askConflict(editor)) {
                ConflictChoice.RELOAD_DOCUMENT -> {
                    reloadAfterExternalChange = true
                    true
                }
                ConflictChoice.OVERWRITE_WITH_VISUAL -> {
                    if (snapshotIntoDocument(editor, nativeWorkspace)) {
                        ApplicationManager.getApplication().invokeLater {
                            if (activeEditor === editor) {
                                persist(editor, saveToDisk = true)
                            }
                        }
                    }
                    false
                }
                ConflictChoice.CANCEL -> false
            }
        } catch (failure: Exception) {
            editor.showLoadError(readableMessage(failure))
            false
        } catch (failure: LinkageError) {
            editor.showLoadError(readableMessage(failure))
            false
        }
    }

    /** Called after IntelliJ accepted external file contents into the Document. */
    fun fileContentReloaded(file: VirtualFile, document: com.intellij.openapi.editor.Document) = onEdt {
        if (!reloadAfterExternalChange) {
            return@onEdt
        }
        reloadAfterExternalChange = false
        val editor = activeEditor
        val nativeWorkspace = workspace
        if (editor == null || nativeWorkspace == null || editor.virtualFile != file || editor.document !== document) {
            return@onEdt
        }

        try {
            loadDocument(editor, nativeWorkspace)
            attach(editor, nativeWorkspace.component)
        } catch (failure: Exception) {
            editor.showLoadError(readableMessage(failure))
        } catch (failure: LinkageError) {
            editor.showLoadError(readableMessage(failure))
        }
        editor.refreshModifiedState()
    }

    private fun ensureWorkspace(): JMeterWorkspace {
        workspace?.let { return it }
        return ApplicationManager.getApplication()
            .getService(JMeterRuntimeService::class.java)
            .createWorkspace()
            .also { workspace = it }
    }

    private fun loadDocument(
        editor: JMeterVisualFileEditor,
        nativeWorkspace: JMeterWorkspace,
    ) {
        val text = editor.document.immutableCharSequence.toString()
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        nativeWorkspace.load(
            ByteArrayInputStream(bytes),
            editor.virtualFile.toNioPath(),
        )
        val fingerprint = DocumentFingerprint.of(text)
        loadedFile = editor.virtualFile
        syncState = DocumentSyncState(fingerprint)
        nativeMarkSaved(nativeWorkspace)
    }

    private fun persist(editor: JMeterVisualFileEditor, saveToDisk: Boolean): Boolean {
        if (!synchronizeDocument(editor)) {
            return false
        }
        if (!saveToDisk) {
            return true
        }

        if (fileDocumentManager.isDocumentUnsaved(editor.document)) {
            internalDocumentSave = true
            try {
                fileDocumentManager.saveDocument(editor.document)
            } finally {
                internalDocumentSave = false
            }
        }

        if (fileDocumentManager.isDocumentUnsaved(editor.document)) {
            Messages.showErrorDialog(
                editor.project,
                "The JMeter test plan could not be saved. The tab switch was cancelled.",
                "Unable to Save JMeter Test Plan",
            )
            return false
        }

        workspace?.let(::nativeMarkSaved)
        syncState?.accept(DocumentFingerprint.of(editor.document.immutableCharSequence))
        editor.refreshModifiedState()
        return true
    }

    private fun synchronizeDocument(editor: JMeterVisualFileEditor): Boolean {
        val nativeWorkspace = workspace ?: return true
        if (loadedFile != editor.virtualFile) {
            return true
        }

        val documentFingerprint = DocumentFingerprint.of(editor.document.immutableCharSequence)
        val state = syncState ?: DocumentSyncState(documentFingerprint).also { syncState = it }
        return try {
            when (state.actionFor(documentFingerprint, nativeIsDirty(nativeWorkspace))) {
                DocumentSyncAction.NONE -> true
                DocumentSyncAction.RELOAD_DOCUMENT -> {
                    loadDocument(editor, nativeWorkspace)
                    true
                }
                DocumentSyncAction.SNAPSHOT_NATIVE -> snapshotIntoDocument(editor, nativeWorkspace)
                DocumentSyncAction.CONFLICT -> when (askConflict(editor)) {
                    ConflictChoice.RELOAD_DOCUMENT -> {
                        loadDocument(editor, nativeWorkspace)
                        true
                    }
                    ConflictChoice.OVERWRITE_WITH_VISUAL -> snapshotIntoDocument(editor, nativeWorkspace)
                    ConflictChoice.CANCEL -> false
                }
            }
        } catch (failure: Exception) {
            editor.showLoadError(readableMessage(failure))
            false
        } catch (failure: LinkageError) {
            editor.showLoadError(readableMessage(failure))
            false
        }
    }

    private fun snapshotIntoDocument(
        editor: JMeterVisualFileEditor,
        nativeWorkspace: JMeterWorkspace,
    ): Boolean {
        val snapshot = nativeWorkspace.snapshot()
        val text = String(snapshot, StandardCharsets.UTF_8)
        if (editor.document.immutableCharSequence.toString() != text) {
            val replaceDocument = Runnable { editor.document.setText(text) }
            if (ApplicationManager.getApplication().isWriteAccessAllowed) {
                replaceDocument.run()
            } else {
                WriteCommandAction.runWriteCommandAction(editor.project, replaceDocument)
            }
        }
        val fingerprint = DocumentFingerprint.of(editor.document.immutableCharSequence)
        (syncState ?: DocumentSyncState(fingerprint).also { syncState = it }).accept(fingerprint)
        return true
    }

    private fun scheduleSavedBaseline(editor: JMeterVisualFileEditor) {
        val expectedFingerprint = DocumentFingerprint.of(editor.document.immutableCharSequence)
        ApplicationManager.getApplication().invokeLater {
            if (
                activeEditor === editor &&
                !fileDocumentManager.isDocumentUnsaved(editor.document) &&
                DocumentFingerprint.of(editor.document.immutableCharSequence) == expectedFingerprint
            ) {
                workspace?.let(::nativeMarkSaved)
                syncState?.accept(expectedFingerprint)
                editor.refreshModifiedState()
            }
        }
    }

    private fun attach(editor: JMeterVisualFileEditor, component: JComponent) {
        activeEditor?.takeIf { it !== editor }?.detachNative(component)
        editor.attachNative(component)
    }

    private fun detach(editor: JMeterVisualFileEditor) {
        workspace?.component?.let(editor::detachNative)
    }

    private fun askConflict(editor: JMeterVisualFileEditor): ConflictChoice {
        val selected = Messages.showDialog(
            editor.project,
            "The JMX text changed after the visual editor loaded it, and the visual model also has changes. " +
                "Choose which version to keep.",
            "JMeter Test Plan Conflict",
            arrayOf("Reload external", "Overwrite visual", "Cancel"),
            2,
            Messages.getWarningIcon(),
        )
        return when (selected) {
            0 -> ConflictChoice.RELOAD_DOCUMENT
            1 -> ConflictChoice.OVERWRITE_WITH_VISUAL
            else -> ConflictChoice.CANCEL
        }
    }

    private fun readableMessage(failure: Throwable): String {
        var root = failure
        while (root.cause != null && root.cause !== root) {
            root = root.cause!!
        }
        return root.message ?: root.javaClass.simpleName
    }

    private fun nativeIsDirty(nativeWorkspace: JMeterWorkspace): Boolean =
        nativeWorkspace.isDirty

    private fun nativeMarkSaved(nativeWorkspace: JMeterWorkspace) {
        nativeWorkspace.markSaved()
    }

    private fun <T> onEdt(action: () -> T): T {
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) {
            return action()
        }

        val value = AtomicReference<Any?>()
        val failure = AtomicReference<Throwable?>()
        application.invokeAndWait {
            try {
                value.set(action())
            } catch (throwable: Throwable) {
                failure.set(throwable)
            }
        }
        failure.get()?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return value.get() as T
    }

    override fun dispose() {
        if (disposed) {
            return
        }
        disposed = true
        onEdt {
            workspace?.close()
            workspace = null
            activeEditor = null
            loadedFile = null
            syncState = null
        }
    }

    private enum class ConflictChoice {
        RELOAD_DOCUMENT,
        OVERWRITE_WITH_VISUAL,
        CANCEL,
    }
}
