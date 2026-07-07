package com.github.duync.jmeterviewer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

public final class JMeterStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        JMeterIdeSaveIntegration.install(project);
    }
}
