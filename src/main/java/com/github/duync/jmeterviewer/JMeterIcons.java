package com.github.duync.jmeterviewer;

import com.intellij.ide.highlighter.XmlFileType;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.net.URL;

final class JMeterIcons {
    static final Icon FILE = icon();
    static final Icon TOOL_WINDOW = small(FILE, 13);

    private JMeterIcons() {
    }

    private static Icon icon() {
        URL resource = JMeterIcons.class.getClassLoader().getResource("org/apache/jmeter/images/meter.png");
        return resource == null ? XmlFileType.INSTANCE.getIcon() : new ImageIcon(resource);
    }

    private static Icon small(Icon icon, int size) {
        if (!(icon instanceof ImageIcon)) {
            return icon;
        }
        Image image = ((ImageIcon) icon).getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }
}
