package com.github.duync.jmeterviewer;

import org.apache.jmeter.testelement.TestElement;

final class JMeterElementMetadata {
    private JMeterElementMetadata() {
    }

    static void normalize(TestElement element) {
        if (element == null) {
            return;
        }
        ensureTestClass(element);
        ensureGuiClass(element);
    }

    private static void ensureTestClass(TestElement element) {
        if (hasValue(element, TestElement.TEST_CLASS)) {
            return;
        }
        element.setProperty(TestElement.TEST_CLASS, element.getClass().getName());
    }

    private static void ensureGuiClass(TestElement element) {
        if (hasValue(element, TestElement.GUI_CLASS)) {
            return;
        }
        JMeterPaletteItem item = JMeterPaletteItem.findByTestClass(
                element.getPropertyAsString(TestElement.TEST_CLASS)
        );
        if (item != null && item.guiClassName() != null) {
            element.setProperty(TestElement.GUI_CLASS, item.guiClassName());
        }
    }

    private static boolean hasValue(TestElement element, String key) {
        String value = element.getPropertyAsString(key);
        return value != null && !value.trim().isEmpty();
    }
}
