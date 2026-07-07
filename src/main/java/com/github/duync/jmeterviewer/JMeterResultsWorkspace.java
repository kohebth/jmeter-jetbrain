package com.github.duync.jmeterviewer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;

public final class JMeterResultsWorkspace {
    static final String TOOL_WINDOW_ID = "JMeter";
    static final String VIEW_RESULTS_TREE_CONTENT = "View Results Tree";

    private final Project project;
    private final JMeterResultsPanel resultsPanel = new JMeterResultsPanel();

    public JMeterResultsWorkspace(Project project) {
        this.project = project;
    }

    static JMeterResultsWorkspace get(Project project) {
        return project.getService(JMeterResultsWorkspace.class);
    }

    JMeterResultsPanel resultsPanel() {
        return resultsPanel;
    }

    void show() {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow != null) {
            toolWindow.show();
        }
    }

    void showViewResultsTree() {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow == null) {
            return;
        }
        Content content = toolWindow.getContentManager().findContent(VIEW_RESULTS_TREE_CONTENT);
        if (content != null) {
            toolWindow.getContentManager().setSelectedContent(content, true);
        }
        toolWindow.show();
    }
}
