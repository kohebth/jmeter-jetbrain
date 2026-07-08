package com.github.duync.jmeterviewer;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.impl.ContentImpl;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.Icon;
import java.awt.BorderLayout;
import java.util.*;

public final class JMeterResultsToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JMeterResultsWorkspace workspace = JMeterResultsWorkspace.get(project);
        JMeterResultsPanel panel = workspace.resultsPanel();
        JTabbedPane tabs = JMeterTabOverflowSupport.createTabbedPane();
        List<JMeterResultsWorkspace.TabSpec> leadingTabs = new ArrayList<>();
        leadingTabs.add(JMeterResultsWorkspace.tab("Tools", JMeterIcons.TOOL_WINDOW, JMeterLeftPanel.create(project)));
        leadingTabs.add(JMeterResultsWorkspace.tab("Run", null, wrap(project, panel, panel.monitorComponent())));
        leadingTabs.add(JMeterResultsWorkspace.tab("Table", null, wrap(project, panel, panel.tableComponent())));
        Map<JMeterNativeResultView, JMeterResultsWorkspace.TabSpec> nativeTabs =
                new EnumMap<>(JMeterNativeResultView.class);
        for (JMeterNativeResultView view : JMeterNativeResultView.values()) {
            nativeTabs.put(view, JMeterResultsWorkspace.tab(view.title(), null,
                    wrap(project, panel, panel.nativeComponent(view))));
        }
        List<JMeterResultsWorkspace.TabSpec> trailingTabs = new ArrayList<>();
        trailingTabs.add(JMeterResultsWorkspace.tab("Summary", null, wrap(project, panel, panel.summaryComponent())));
        trailingTabs.add(JMeterResultsWorkspace.tab("Log", null, wrap(project, panel, panel.logComponent())));
        workspace.installTabs(tabs, leadingTabs, nativeTabs, trailingTabs);
        toolWindow.setIcon(JMeterIcons.TOOL_WINDOW);
        Content content = new ContentImpl(tabs, "JMeter", false);
        content.setIcon(JMeterIcons.TOOL_WINDOW);
        content.setPopupIcon(JMeterIcons.TOOL_WINDOW);
        toolWindow.getContentManager().addContent(content);
    }

    private JComponent wrap(Project project, JMeterResultsPanel panel, JComponent component) {
        JPanel wrapper = new JPanel(new BorderLayout());
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
        toolbar.add(JMeterIconButtons.compact(new JMeterResultFileLoader(project, panel).button(),
                "Load JTL", AllIcons.Actions.Download));
        toolbar.add(JMeterIconButtons.compact(new JMeterReportAction(project, panel).button(),
                "HTML Report", AllIcons.Actions.PreviewDetails));
        toolbar.addSeparator();
        toolbar.add(button("Clear Samples", AllIcons.Actions.DeleteTag, panel::clearResults));
        toolbar.add(button("Clear Log", AllIcons.Actions.DeleteTag, panel::clearLog));
        wrapper.add(toolbar, BorderLayout.NORTH);
        wrapper.add(JMeterTabOverflowSupport.apply(component), BorderLayout.CENTER);
        return JMeterTabOverflowSupport.apply(wrapper);
    }

    private JButton button(String tooltip, Icon icon, Runnable action) {
        return JMeterIconButtons.create(tooltip, icon, action);
    }
}
