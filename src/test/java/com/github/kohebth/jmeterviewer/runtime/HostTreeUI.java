package com.github.kohebth.jmeterviewer.runtime;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTreeUI;

/** Host-only look-and-feel delegate used to exercise Swing's classloader lookup. */
public final class HostTreeUI extends BasicTreeUI {
    public static ComponentUI createUI(JComponent component) {
        return new HostTreeUI();
    }
}
