package com.github.duync.jmeterviewer;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.List;

final class JMeterOpenEditors {
    private JMeterOpenEditors() {
    }

    static JMeterVisualFileEditor selected(Project project) {
        if (project == null) {
            return null;
        }
        FileEditor selected = FileEditorManager.getInstance(project).getSelectedEditor();
        return selected instanceof JMeterVisualFileEditor ? (JMeterVisualFileEditor) selected : null;
    }

    static List<JMeterVisualFileEditor> all(Project project) {
        List<JMeterVisualFileEditor> editors = new ArrayList<>();
        if (project == null) {
            return editors;
        }
        for (FileEditor editor : FileEditorManager.getInstance(project).getAllEditors()) {
            if (editor instanceof JMeterVisualFileEditor) {
                editors.add((JMeterVisualFileEditor) editor);
            }
        }
        return editors;
    }

    static List<JMeterVisualFileEditor> modified(Project project) {
        List<JMeterVisualFileEditor> editors = new ArrayList<>();
        for (JMeterVisualFileEditor editor : all(project)) {
            if (editor.isModified()) {
                editors.add(editor);
            }
        }
        return editors;
    }
}
