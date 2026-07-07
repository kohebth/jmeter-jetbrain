package com.github.duync.jmeterviewer;

import com.intellij.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.project.Project;

public final class JMeterIdeSaveIntegration implements FileDocumentManagerListener {
    private final Project project;
    private boolean saving;

    public JMeterIdeSaveIntegration(Project project) {
        this.project = project;
    }

    static void install(Project project) {
        ApplicationManager.getApplication()
                .getMessageBus()
                .connect(project)
                .subscribe(AppTopics.FILE_DOCUMENT_SYNC, new JMeterIdeSaveIntegration(project));
    }

    @Override
    public void beforeAllDocumentsSaving() {
        saveModifiedEditors();
    }

    private void saveModifiedEditors() {
        if (saving || project.isDisposed()) {
            return;
        }
        saving = true;
        try {
            for (JMeterVisualFileEditor editor : JMeterOpenEditors.modified(project)) {
                editor.saveSilently();
            }
        } finally {
            saving = false;
        }
    }
}
