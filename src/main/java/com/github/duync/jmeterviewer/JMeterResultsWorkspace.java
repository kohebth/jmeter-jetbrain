package com.github.duync.jmeterviewer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import java.util.*;

public final class JMeterResultsWorkspace {
    static final String TOOL_WINDOW_ID = "JMeter";

    private final Project project;
    private final JMeterResultsPanel resultsPanel = new JMeterResultsPanel();
    private final List<TabSpec> leadingTabs = new ArrayList<>();
    private final List<TabSpec> trailingTabs = new ArrayList<>();
    private final EnumMap<JMeterNativeResultView, TabSpec> nativeTabs =
            new EnumMap<>(JMeterNativeResultView.class);
    private EnumSet<JMeterNativeResultView> availableNativeViews = EnumSet.noneOf(JMeterNativeResultView.class);
    private JTabbedPane toolWindowTabs;

    public JMeterResultsWorkspace(Project project) {
        this.project = project;
    }

    static JMeterResultsWorkspace get(Project project) {
        return project.getService(JMeterResultsWorkspace.class);
    }

    JMeterResultsPanel resultsPanel() {
        return resultsPanel;
    }

    void installTabs(JTabbedPane toolWindowTabs,
                     List<TabSpec> leadingTabs,
                     Map<JMeterNativeResultView, TabSpec> nativeTabs,
                     List<TabSpec> trailingTabs) {
        this.toolWindowTabs = toolWindowTabs;
        this.leadingTabs.clear();
        this.leadingTabs.addAll(leadingTabs);
        this.nativeTabs.clear();
        this.nativeTabs.putAll(nativeTabs);
        this.trailingTabs.clear();
        this.trailingTabs.addAll(trailingTabs);
        rebuildTabs();
    }

    void updateNativeResultViews(EnumSet<JMeterNativeResultView> views) {
        EnumSet<JMeterNativeResultView> next = views == null || views.isEmpty()
                ? EnumSet.noneOf(JMeterNativeResultView.class)
                : EnumSet.copyOf(views);
        if (availableNativeViews.equals(next)) {
            return;
        }
        availableNativeViews = next;
        rebuildTabs();
    }

    void show() {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow != null) {
            toolWindow.show();
        }
    }

    void showViewResultsTree() {
        if (!availableNativeViews.contains(JMeterNativeResultView.VIEW_RESULTS_TREE)) {
            showContent("Table");
            return;
        }
        showNativeView(JMeterNativeResultView.VIEW_RESULTS_TREE);
    }

    void showNativeView(JMeterNativeResultView view) {
        showContent(view.title());
    }

    private void showContent(String contentName) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow == null) {
            return;
        }
        if (toolWindowTabs != null) {
            for (int i = 0; i < toolWindowTabs.getTabCount(); i++) {
                if (contentName.equals(toolWindowTabs.getTitleAt(i))) {
                    toolWindowTabs.setSelectedIndex(i);
                    break;
                }
            }
        }
        toolWindow.show();
    }

    private void rebuildTabs() {
        if (toolWindowTabs == null) {
            return;
        }
        String selected = selectedTitle();
        toolWindowTabs.removeAll();
        addTabs(leadingTabs);
        for (JMeterNativeResultView view : JMeterNativeResultView.values()) {
            if (availableNativeViews.contains(view)) {
                addTab(nativeTabs.get(view));
            }
        }
        addTabs(trailingTabs);
        restoreSelection(selected);
    }

    private String selectedTitle() {
        if (toolWindowTabs == null || toolWindowTabs.getSelectedIndex() < 0) {
            return null;
        }
        return toolWindowTabs.getTitleAt(toolWindowTabs.getSelectedIndex());
    }

    private void addTabs(List<TabSpec> specs) {
        for (TabSpec spec : specs) {
            addTab(spec);
        }
    }

    private void addTab(TabSpec spec) {
        if (spec == null) {
            return;
        }
        toolWindowTabs.addTab(spec.title, spec.icon, spec.component);
    }

    private void restoreSelection(String selected) {
        if (selected == null) {
            return;
        }
        for (int i = 0; i < toolWindowTabs.getTabCount(); i++) {
            if (selected.equals(toolWindowTabs.getTitleAt(i))) {
                toolWindowTabs.setSelectedIndex(i);
                return;
            }
        }
    }

    static TabSpec tab(String title, Icon icon, JComponent component) {
        return new TabSpec(title, icon, component);
    }

    static final class TabSpec {
        private final String title;
        private final Icon icon;
        private final JComponent component;

        private TabSpec(String title, Icon icon, JComponent component) {
            this.title = title;
            this.icon = icon;
            this.component = component;
        }
    }
}
