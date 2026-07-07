package com.github.duync.jmeterviewer;

import org.apache.jmeter.gui.tree.JMeterTreeModel;

final class JMeterPlanValidator {
    private JMeterPlanValidator() {
    }

    static String validate(JMeterTreeModel model) {
        return JMeterPlanDiagnostics.inspect(model).format();
    }
}
