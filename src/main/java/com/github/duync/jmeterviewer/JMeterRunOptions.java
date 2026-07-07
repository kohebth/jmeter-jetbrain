package com.github.duync.jmeterviewer;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import org.apache.jmeter.util.JMeterUtils;

import javax.swing.*;
import java.awt.*;

final class JMeterRunOptions {
    private final JPanel panel;
    private final JTextField remoteHosts;
    private final JTextField resultFile;
    private final JComboBox<String> logLevel;
    private final JMeterAdvancedRunOptions advancedOptions;

    JMeterRunOptions(Project project) {
        panel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 4, 0));
        remoteHosts = new JTextField(12);
        resultFile = new JTextField(14);
        logLevel = new JComboBox<>(new String[]{"", "INFO", "DEBUG", "WARN", "ERROR"});
        advancedOptions = new JMeterAdvancedRunOptions(project);

        panel.add(new JLabel("Remote"));
        panel.add(remoteHosts);
        panel.add(new JLabel("JTL"));
        panel.add(resultFile);
        panel.add(new JLabel("Log"));
        panel.add(logLevel);
        panel.add(advancedOptions.button());
    }

    JComponent component() {
        return panel;
    }

    void apply() {
        setOrClear("remote_hosts", remoteHosts.getText());
        setOrClear("jmeter.save.saveservice.output_format", "xml");
        setOrClear("jmeterengine.nongui.maxport", "");
        setOrClear("jmeterengine.nongui.port", "");
        setOrClear("log_level.jmeter", selectedLogLevel());
        advancedOptions.apply();
    }

    String resultFile() {
        String value = resultFile.getText().trim();
        return value.isEmpty() ? null : value;
    }

    java.util.List<String> remoteHosts() {
        java.util.List<String> hosts = new java.util.ArrayList<>();
        for (String host : remoteHosts.getText().split(",")) {
            String trimmed = host.trim();
            if (!trimmed.isEmpty()) {
                hosts.add(trimmed);
            }
        }
        return hosts;
    }

    private String selectedLogLevel() {
        Object selected = logLevel.getSelectedItem();
        return selected == null ? "" : selected.toString();
    }

    private void setOrClear(String key, String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            JMeterUtils.getJMeterProperties().remove(key);
        } else {
            JMeterUtils.setProperty(key, trimmed);
        }
    }
}
