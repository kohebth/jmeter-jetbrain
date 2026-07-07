package com.github.duync.jmeterviewer;

import com.intellij.openapi.fileChooser.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;
import java.io.File;

final class JMeterPluginPanel {
    private JMeterPluginPanel() {
    }

    static JComponent create(Project project) {
        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> list = new JList<>(model);
        JButton add = new JButton("Add JAR/Folder");
        JButton refresh = new JButton("Refresh");
        add.addActionListener(event -> addPath(project, model));
        refresh.addActionListener(event -> refresh(model));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.add(add);
        actions.add(refresh);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JBScrollPane(list), BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        refresh(model);
        return panel;
    }

    private static void addPath(Project project, DefaultListModel<String> model) {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, true, true, false, true)
                .withFileFilter(file -> file.isDirectory() || file.getName().endsWith(".jar"));
        VirtualFile[] files = FileChooser.chooseFiles(descriptor, project, null);
        for (VirtualFile file : files) {
            JMeterPluginClasspath.add(file);
        }
        refresh(model);
    }

    private static void refresh(DefaultListModel<String> model) {
        model.clear();
        for (File path : JMeterPluginClasspath.paths()) {
            model.addElement(path.getAbsolutePath());
        }
    }
}
