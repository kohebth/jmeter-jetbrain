package com.github.duync.jmeterviewer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.impl.ContentImpl;
import org.jetbrains.annotations.NotNull;

public final class JMeterToolsToolWindowFactory implements ToolWindowFactory {
    static final String TOOL_WINDOW_ID = "JMeter Tools";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        toolWindow.getContentManager().addContent(
                new ContentImpl(JMeterLeftPanel.create(project), "Palette", false)
        );
    }
}
