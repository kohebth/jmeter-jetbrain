package com.github.duync.jmeterviewer;

import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;

final class JMeterPlanValidator {
    private JMeterPlanValidator() {
    }

    static String validate(JMeterTreeModel model) {
        if (model == null) {
            return "No JMeter model loaded.";
        }
        StringBuilder output = new StringBuilder();
        validateNode((JMeterTreeNode) model.getRoot(), output);
        return output.length() == 0 ? "Validation OK" : output.toString();
    }

    private static void validateNode(JMeterTreeNode node, StringBuilder output) {
        Object userObject = node.getUserObject();
        if (userObject instanceof TestElement) {
            validateElement(node, (TestElement) userObject, output);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            validateNode((JMeterTreeNode) node.getChildAt(i), output);
        }
    }

    private static void validateElement(JMeterTreeNode node, TestElement element, StringBuilder output) {
        requireClass(node, element, TestElement.GUI_CLASS, output);
        requireClass(node, element, TestElement.TEST_CLASS, output);
        String name = element.getName();
        if (name == null || name.trim().isEmpty()) {
            output.append("Unnamed element at ").append(node.getPath()).append('\n');
        }
    }

    private static void requireClass(JMeterTreeNode node, TestElement element, String property, StringBuilder output) {
        String className = element.getPropertyAsString(property);
        if (className == null || className.trim().isEmpty()) {
            output.append(node.getName()).append(" missing ").append(property).append('\n');
            return;
        }
        try {
            JMeterPluginClasspath.loadClass(className);
        } catch (ClassNotFoundException exception) {
            output.append(node.getName()).append(" cannot load ").append(className).append('\n');
        }
    }
}
