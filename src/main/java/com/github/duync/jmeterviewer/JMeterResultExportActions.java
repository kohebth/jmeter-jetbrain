package com.github.duync.jmeterviewer;

import com.intellij.openapi.fileChooser.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.JButton;
import java.io.File;

final class JMeterResultExportActions {
    private final JButton samplesButton;
    private final JButton logButton;

    JMeterResultExportActions(Project project, VirtualFile file, JMeterResultsPanel resultsPanel) {
        samplesButton = new JButton("Export Samples");
        logButton = new JButton("Export Log");
        samplesButton.addActionListener(event -> {
            File folder = chooseFolder(project, file);
            if (folder != null) {
                resultsPanel.exportSamples(new File(folder, file.getName() + ".samples.csv"));
            }
        });
        logButton.addActionListener(event -> {
            File folder = chooseFolder(project, file);
            if (folder != null) {
                resultsPanel.exportLog(new File(folder, file.getName() + ".run.log"));
            }
        });
    }

    JButton samplesButton() {
        return samplesButton;
    }

    JButton logButton() {
        return logButton;
    }

    private File chooseFolder(Project project, VirtualFile file) {
        VirtualFile initial = file.getParent();
        VirtualFile folder = FileChooser.chooseFile(
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                project,
                initial
        );
        return folder == null ? null : new File(folder.getPath());
    }
}
