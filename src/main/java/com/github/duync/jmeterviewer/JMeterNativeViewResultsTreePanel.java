package com.github.duync.jmeterviewer;

import com.intellij.ui.components.JBScrollPane;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.visualizers.ViewResultsFullVisualizer;

import javax.swing.*;
import java.awt.*;

final class JMeterNativeViewResultsTreePanel {
    private final JPanel panel = new JPanel(new BorderLayout());
    private final ViewResultsFullVisualizer visualizer;
    private final JTextArea fallback;

    JMeterNativeViewResultsTreePanel() {
        ViewResultsFullVisualizer created = null;
        JTextArea error = null;
        try {
            EmbeddedJMeterRuntime.ensureReady();
            created = new ViewResultsFullVisualizer();
            created.setName("View Results Tree");
            panel.add(created, BorderLayout.CENTER);
        } catch (Exception | LinkageError exception) {
            error = new JTextArea("Unable to create JMeter View Results Tree:\n" + exception);
            error.setEditable(false);
            panel.add(new JBScrollPane(error), BorderLayout.CENTER);
        }
        visualizer = created;
        fallback = error;
    }

    JComponent component() {
        return panel;
    }

    void clear() {
        if (visualizer != null) {
            visualizer.clearData();
        }
        if (fallback != null) {
            fallback.setCaretPosition(0);
        }
    }

    void add(SampleResult result) {
        if (visualizer != null && result != null) {
            visualizer.add(result);
        }
    }
}
