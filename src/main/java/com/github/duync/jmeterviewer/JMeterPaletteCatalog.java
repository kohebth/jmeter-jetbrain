package com.github.duync.jmeterviewer;

import java.util.*;

final class JMeterPaletteCatalog {
    private static volatile List<JMeterPaletteItem> cachedItems;

    private JMeterPaletteCatalog() {
    }

    static List<JMeterPaletteItem> items() {
        List<JMeterPaletteItem> local = cachedItems;
        if (local == null) {
            local = loadItems();
            cachedItems = local;
        }
        return local;
    }

    static void reset() {
        cachedItems = null;
    }

    private static List<JMeterPaletteItem> loadItems() {
        Map<String, JMeterPaletteItem> items = new LinkedHashMap<>();
        for (JMeterPaletteItem item : JMeterPaletteItem.DEFAULT_ITEMS) {
            items.put(item.key(), item);
        }
        for (JMeterPaletteItem item : JMeterPaletteDiscovery.discover()) {
            items.putIfAbsent(item.key(), item);
        }
        return Collections.unmodifiableList(new ArrayList<>(items.values()));
    }
}
