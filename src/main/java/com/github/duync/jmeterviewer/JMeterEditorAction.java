package com.github.duync.jmeterviewer;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

abstract class JMeterEditorAction extends AnAction {
    @Override
    public final void actionPerformed(@NotNull AnActionEvent event) {
        JMeterVisualFileEditor editor = selectedEditor(event.getProject());
        if (editor != null) {
            perform(editor);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(selectedEditor(event.getProject()) != null);
    }

    protected abstract void perform(JMeterVisualFileEditor editor);

    private JMeterVisualFileEditor selectedEditor(Project project) {
        return JMeterOpenEditors.selected(project);
    }
}
