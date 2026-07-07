package com.github.duync.jmeterviewer;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;

final class JMeterEditorBody {
    private JMeterEditorBody() {
    }

    static JComponent create(Project project, JTree tree, JComponent details, JMeterSourcePanel sourcePanel) {
        JBSplitter treeAndDetails = new JBSplitter(false, 0.34f);
        treeAndDetails.setFirstComponent(new JBScrollPane(tree));
        treeAndDetails.setSecondComponent(details);

        JBSplitter paletteAndEditor = new JBSplitter(false, 0.18f);
        paletteAndEditor.setFirstComponent(JMeterLeftPanel.create(project));
        paletteAndEditor.setSecondComponent(treeAndDetails);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Visual", paletteAndEditor);
        tabs.addTab("JMX Source", sourcePanel.component());
        return tabs;
    }
}
