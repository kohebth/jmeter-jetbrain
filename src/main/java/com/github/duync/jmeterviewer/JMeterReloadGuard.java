package com.github.duync.jmeterviewer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

final class JMeterReloadGuard {
    private JMeterReloadGuard() {
    }

    static boolean canDiscard(Project project, boolean modified) {
        if (!modified) {
            return true;
        }
        return Messages.showYesNoDialog(
                project,
                "Reloading will discard unsaved JMeter changes.",
                "Reload JMeter File",
                Messages.getQuestionIcon()
        ) == Messages.YES;
    }
}
