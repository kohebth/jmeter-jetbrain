package com.github.duync.jmeterviewer;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class SaveJMeterEditorsAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        JMeterVisualFileEditor selected = selectedEditor(project);
        if (selected != null) {
            selected.save();
            return;
        }
        for (JMeterVisualFileEditor editor : openEditors(project)) {
            if (editor.isModified()) {
                editor.save();
            }
        }
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        boolean enabled = project != null && hasJMeterEditor(project);
        event.getPresentation().setEnabledAndVisible(enabled);
    }

    private boolean hasJMeterEditor(Project project) {
        if (selectedEditor(project) != null) {
            return true;
        }
        return !openEditors(project).isEmpty();
    }

    private JMeterVisualFileEditor selectedEditor(Project project) {
        FileEditor selected = FileEditorManager.getInstance(project).getSelectedEditor();
        return selected instanceof JMeterVisualFileEditor ? (JMeterVisualFileEditor) selected : null;
    }

    private java.util.List<JMeterVisualFileEditor> openEditors(Project project) {
        java.util.List<JMeterVisualFileEditor> editors = new java.util.ArrayList<>();
        for (FileEditor editor : FileEditorManager.getInstance(project).getAllEditors()) {
            if (editor instanceof JMeterVisualFileEditor) {
                editors.add((JMeterVisualFileEditor) editor);
            }
        }
        return editors;
    }
}
