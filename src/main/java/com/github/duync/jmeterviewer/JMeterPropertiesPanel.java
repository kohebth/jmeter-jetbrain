package com.github.duync.jmeterviewer;

import com.intellij.ui.components.JBScrollPane;
import org.apache.jmeter.util.JMeterUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;

final class JMeterPropertiesPanel {
    private JMeterPropertiesPanel() {
    }

    static JComponent create() {
        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> list = new JList<>(model);
        JTextField key = new JTextField(16);
        JTextField value = new JTextField(18);
        JButton set = new JButton("Set");
        JButton remove = new JButton("Remove");
        JButton refresh = new JButton("Refresh");
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        form.add(new JLabel("Key"));
        form.add(key);
        form.add(new JLabel("Value"));
        form.add(value);
        form.add(set);
        form.add(remove);
        form.add(refresh);
        set.addActionListener(event -> setProperty(key, value, model));
        remove.addActionListener(event -> removeProperty(key, model));
        refresh.addActionListener(event -> refresh(model));
        list.addListSelectionListener(event -> select(list, key, value));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(form, BorderLayout.NORTH);
        panel.add(new JBScrollPane(list), BorderLayout.CENTER);
        refresh(model);
        return panel;
    }

    private static void setProperty(JTextField key, JTextField value, DefaultListModel<String> model) {
        if (!key.getText().trim().isEmpty()) {
            JMeterUtils.setProperty(key.getText().trim(), value.getText());
            refresh(model);
        }
    }

    private static void removeProperty(JTextField key, DefaultListModel<String> model) {
        JMeterUtils.getJMeterProperties().remove(key.getText().trim());
        refresh(model);
    }

    private static void refresh(DefaultListModel<String> model) {
        model.clear();
        TreeSet<String> keys = new TreeSet<>();
        for (Object key : JMeterUtils.getJMeterProperties().keySet()) {
            keys.add(String.valueOf(key));
        }
        for (String key : keys) {
            model.addElement(key + "=" + JMeterUtils.getProperty(key));
        }
    }

    private static void select(JList<String> list, JTextField key, JTextField value) {
        String selected = list.getSelectedValue();
        if (selected == null) {
            return;
        }
        int separator = selected.indexOf('=');
        key.setText(separator < 0 ? selected : selected.substring(0, separator));
        value.setText(separator < 0 ? "" : selected.substring(separator + 1));
    }
}
