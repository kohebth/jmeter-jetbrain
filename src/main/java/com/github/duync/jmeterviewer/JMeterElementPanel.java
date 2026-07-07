package com.github.duync.jmeterviewer;

import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.JMeterGUIComponent;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;

import javax.swing.*;
import java.awt.BorderLayout;

final class JMeterElementPanel {
    private final JPanel panel;
    private final JEditorPane errorPane;
    private final JMeterGuiDirtyTracker dirtyTracker;

    JMeterElementPanel(Runnable dirty) {
        panel = new JBPanel<>(new BorderLayout());
        errorPane = new JEditorPane("text/plain", "");
        errorPane.setEditable(false);
        dirtyTracker = new JMeterGuiDirtyTracker(dirty);
    }

    JComponent component() {
        return panel;
    }

    void showSelected() {
        panel.removeAll();
        JMeterGUIComponent selectedGui = GuiPackage.getInstance().getCurrentGui();
        if (selectedGui instanceof JComponent) {
            JComponent component = (JComponent) selectedGui;
            dirtyTracker.watch(component);
            panel.add(component, BorderLayout.CENTER);
        } else {
            panel.add(new JLabel(missingGuiMessage()), BorderLayout.NORTH);
        }
        refresh();
    }

    void showError(String message) {
        errorPane.setText(message);
        panel.removeAll();
        panel.add(new JBScrollPane(errorPane), BorderLayout.CENTER);
        refresh();
    }

    private void refresh() {
        panel.revalidate();
        panel.repaint();
    }

    private String missingGuiMessage() {
        JMeterTreeNode currentNode = GuiPackage.getInstance().getCurrentNode();
        Object userObject = currentNode.getUserObject();
        if (!(userObject instanceof TestElement)) {
            return "No JMeter GUI is available for this node.";
        }

        TestElement element = (TestElement) userObject;
        String guiClass = element.getPropertyAsString(TestElement.GUI_CLASS);
        String testClass = element.getPropertyAsString(TestElement.TEST_CLASS);
        return "No JMeter GUI is available for " + element.getName()
                + " (guiclass=" + guiClass + ", testclass=" + testClass + ").";
    }
}
