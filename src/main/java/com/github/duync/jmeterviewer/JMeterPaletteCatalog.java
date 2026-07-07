package com.github.duync.jmeterviewer;

import com.intellij.openapi.application.ApplicationManager;

import javax.swing.SwingUtilities;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

final class JMeterPaletteCatalog {
    private static final List<JMeterPaletteItem> DEFAULTS =
            Collections.unmodifiableList(new ArrayList<>(JMeterPaletteItem.DEFAULT_ITEMS));
    private static final AtomicBoolean DISCOVERING = new AtomicBoolean(false);
    private static volatile List<JMeterPaletteItem> cachedItems = DEFAULTS;

    private JMeterPaletteCatalog() {
    }

    static List<JMeterPaletteItem> items() {
        return cachedItems;
    }

    static void reset() {
        cachedItems = DEFAULTS;
    }

    static boolean isDiscovering() {
        return DISCOVERING.get();
    }

    static void discoverAsync(Runnable afterUpdate) {
        if (!DISCOVERING.compareAndSet(false, true)) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                cachedItems = loadItems();
            } finally {
                DISCOVERING.set(false);
                if (afterUpdate != null) {
                    SwingUtilities.invokeLater(afterUpdate);
                }
            }
        });
    }

    private static List<JMeterPaletteItem> loadItems() {
        Map<String, JMeterPaletteItem> items = new LinkedHashMap<>();
        for (JMeterPaletteItem item : DEFAULTS) {
            items.put(item.key(), item);
        }
        for (JMeterPaletteItem item : JMeterPaletteDiscovery.discover()) {
            items.putIfAbsent(item.key(), item);
        }
        return Collections.unmodifiableList(new ArrayList<>(items.values()));
    }
}
