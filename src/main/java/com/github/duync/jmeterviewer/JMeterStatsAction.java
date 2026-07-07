package com.github.duync.jmeterviewer;

import org.apache.jmeter.gui.tree.JMeterTreeModel;

import javax.swing.JButton;
import java.util.function.Supplier;

final class JMeterStatsAction {
    private final JButton button;

    JMeterStatsAction(Supplier<JMeterTreeModel> modelSupplier, JMeterResultsPanel resultsPanel) {
        button = new JButton("Stats");
        button.addActionListener(event -> resultsPanel.appendDiagnostic(
                JMeterPlanStats.describe(modelSupplier.get())
        ));
    }

    JButton button() {
        return button;
    }
}
