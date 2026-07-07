package com.github.duync.jmeterviewer;

import com.intellij.ui.ListSpeedSearch;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.util.Locale;

final class JMeterPalettePanel {
    private JMeterPalettePanel() {
    }

    static JComponent create() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextField filter = new JTextField();
        DefaultListModel<JMeterPaletteItem> model = new DefaultListModel<>();
        JList<JMeterPaletteItem> list = new JList<>(model);
        JButton refresh = new JButton("Refresh");
        refresh(model, "");
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setDragEnabled(true);
        list.setTransferHandler(new JMeterPaletteTransferHandler());
        list.setCellRenderer(new Renderer());
        new ListSpeedSearch<>(list, item -> item == null ? "" : item.toString());
        filter.getDocument().addDocumentListener(new FilterListener(model, filter));
        refresh.addActionListener(event -> {
            JMeterPaletteCatalog.reset();
            refresh(model, filter.getText());
        });
        JPanel top = new JPanel(new BorderLayout(4, 0));
        top.add(filter, BorderLayout.CENTER);
        top.add(refresh, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JBScrollPane(list), BorderLayout.CENTER);
        return panel;
    }

    private static void refresh(DefaultListModel<JMeterPaletteItem> model, String query) {
        model.clear();
        String lowerQuery = query.trim().toLowerCase(Locale.ROOT);
        for (JMeterPaletteItem item : JMeterPaletteCatalog.items()) {
            if (lowerQuery.isEmpty() || item.toString().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                model.addElement(item);
            }
        }
    }

    private static final class FilterListener implements DocumentListener {
        private final DefaultListModel<JMeterPaletteItem> model;
        private final JTextField filter;

        private FilterListener(DefaultListModel<JMeterPaletteItem> model, JTextField filter) {
            this.model = model;
            this.filter = filter;
        }

        @Override
        public void insertUpdate(DocumentEvent event) {
            refresh(model, filter.getText());
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            refresh(model, filter.getText());
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
            refresh(model, filter.getText());
        }
    }

    private static final class Renderer extends DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean selected,
                boolean focus
        ) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
            if (value instanceof JMeterPaletteItem) {
                JMeterPaletteItem item = (JMeterPaletteItem) value;
                label.setText(item.kind().name().replace('_', ' ') + " - " + item);
            }
            return label;
        }
    }
}
