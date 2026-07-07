package com.github.duync.jmeterviewer;

import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.gui.util.MenuFactory;
import org.apache.jmeter.testelement.TestElement;

final class JMeterAddRules {
    private JMeterAddRules() {
    }

    static boolean canAdd(JMeterTreeNode parent, JMeterPaletteItem item) {
        return createAddableElement(parent, item) != null;
    }

    static TestElement createAddableElement(JMeterTreeNode parent, JMeterPaletteItem item) {
        if (parent == null || item == null) {
            return null;
        }
        try {
            TestElement element = item.createTestElement();
            return MenuFactory.canAddTo(parent, element) ? element : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    static boolean canAddElement(JMeterTreeNode parent, TestElement element) {
        if (parent == null || element == null) {
            return false;
        }
        return MenuFactory.canAddTo(parent, element);
    }
}
