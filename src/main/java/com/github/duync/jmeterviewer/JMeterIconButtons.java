package com.github.duync.jmeterviewer;

import com.intellij.util.ui.JBUI;

import javax.swing.Icon;
import javax.swing.JButton;

final class JMeterIconButtons {
    private JMeterIconButtons() {
    }

    static JButton create(String tooltip, Icon icon, Runnable action) {
        JButton button = compact(new JButton(), tooltip, icon);
        button.addActionListener(event -> action.run());
        return button;
    }

    static JButton compact(JButton button, String tooltip, Icon icon) {
        button.setText(null);
        button.setIcon(icon);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setBorder(JBUI.Borders.empty(3, 5));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setMargin(JBUI.emptyInsets());
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.putClientProperty("JButton.buttonType", "toolbar");
        return button;
    }
}
