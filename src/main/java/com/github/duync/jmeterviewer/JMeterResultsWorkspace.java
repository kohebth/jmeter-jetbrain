package com.github.duync.jmeterviewer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;

public final class JMeterResultsWorkspace {
    static final String TOOL_WINDOW_ID = "JMeter";

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
        Content content = toolWindow.getContentManager().findContent(contentName);
        if (content != null) {
            toolWindow.getContentManager().setSelectedContent(content, true);
        }
        toolWindow.show();
    }
}
