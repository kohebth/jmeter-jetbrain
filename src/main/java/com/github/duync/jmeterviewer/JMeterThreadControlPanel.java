package com.github.duync.jmeterviewer;

import com.intellij.ui.components.JBPanel;

import javax.swing.*;
import java.awt.*;

final class JMeterThreadControlPanel {
    private final JPanel panel;
    private final JTextField threadName;

    JMeterThreadControlPanel(JMeterRunController runController) {
        panel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 4, 0));
        threadName = new JTextField(12);
        JButton stop = new JButton("Stop Thread");
        JButton stopNow = new JButton("Stop Thread Now");
        stop.addActionListener(event -> runController.stopThread(threadName.getText(), false));
        stopNow.addActionListener(event -> runController.stopThread(threadName.getText(), true));
        panel.add(new JLabel("Thread"));
        panel.add(threadName);
        panel.add(stop);
        panel.add(stopNow);
    }

    JComponent component() {
        return panel;
    }
}
