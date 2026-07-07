package com.github.duync.jmeterviewer;

import javax.swing.*;
import java.awt.event.*;

final class JMeterEditorShortcuts {
    private JMeterEditorShortcuts() {
    }

    static void install(JComponent component,
                        Runnable save,
                        Runnable reload,
                        Runnable run,
                        Runnable stop,
                        Runnable validate,
                        Runnable commands) {
        bind(component, KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, "jmeter.save", save);
        bind(component, KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK, "jmeter.reload", reload);
        bind(component, KeyEvent.VK_F5, 0, "jmeter.run", run);
        bind(component, KeyEvent.VK_F5, InputEvent.SHIFT_DOWN_MASK, "jmeter.stop", stop);
        bind(component, KeyEvent.VK_F8, 0, "jmeter.validate", validate);
        bind(component, KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, "jmeter.commands", commands);
    }

    private static void bind(JComponent component, int key, int modifiers, String name, Runnable action) {
        component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(key, modifiers), name);
        component.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }
}
