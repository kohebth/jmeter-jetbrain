package com.github.duync.jmeterviewer;

import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;

final class JMeterViewResultsTreeLocator {
    private static final String SHORT_GUI_CLASS = "ViewResultsFullVisualizer";
    private static final String FULL_GUI_CLASS = "org.apache.jmeter.visualizers.ViewResultsFullVisualizer";

    private JMeterViewResultsTreeLocator() {
    }

    static TestElement find(JMeterTreeModel model) {
        if (model == null || !(model.getRoot() instanceof JMeterTreeNode)) {
            return null;
        }
        return find((JMeterTreeNode) model.getRoot());
    }

    static boolean isViewResultsTree(TestElement element) {
        if (element == null) {
            return false;
        }
        String guiClass = element.getPropertyAsString(TestElement.GUI_CLASS);
        return SHORT_GUI_CLASS.equals(guiClass) || FULL_GUI_CLASS.equals(guiClass);
    }

    private static TestElement find(JMeterTreeNode node) {
        TestElement element = node.getTestElement();
        if (isViewResultsTree(element)) {
            return element;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            TestElement found = find((JMeterTreeNode) node.getChildAt(i));
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
