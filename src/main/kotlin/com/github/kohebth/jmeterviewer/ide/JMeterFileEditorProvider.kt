package com.github.kohebth.jmeterviewer.ide

import com.github.kohebth.jmeterviewer.editor.JMeterVisualFileEditor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class JMeterFileEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean =
        file.isInLocalFileSystem &&
            !file.isDirectory &&
            file.extension.equals(JMeterFileType.defaultExtension, ignoreCase = true)

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        JMeterVisualFileEditor(project, file)

    override fun getEditorTypeId(): String = EDITOR_TYPE_ID

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR

    companion object {
        const val EDITOR_TYPE_ID: String = "jmeter-native-visual-editor"
    }
}
