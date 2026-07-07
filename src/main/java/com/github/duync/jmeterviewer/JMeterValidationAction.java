package com.github.duync.jmeterviewer;

import org.apache.jmeter.gui.tree.JMeterTreeModel;

import javax.swing.JButton;
import java.util.function.Supplier;

final class JMeterValidationAction {
    private final JButton button;

    JMeterValidationAction(Supplier<JMeterTreeModel> modelSupplier, JMeterResultsPanel resultsPanel) {
        button = new JButton("Validate");
        button.addActionListener(event -> resultsPanel.appendDiagnostic(
                JMeterPlanValidator.validate(modelSupplier.get())
        ));
    }

    JButton button() {
        return button;
    }

    void validateNow() {
        button.doClick();
    }
}
