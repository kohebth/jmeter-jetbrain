package com.github.duync.jmeterviewer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.save.SaveService;

import java.io.ByteArrayOutputStream;

final class JMeterFileSaver {
    private JMeterFileSaver() {
    }

    static boolean save(Project project, VirtualFile file, JMeterTreeModel model, JMeterElementPanel errors) {
        if (model == null) {
            return false;
        }

        try {
            GuiPackage guiPackage = GuiPackage.getInstance();
            if (guiPackage != null) {
                guiPackage.updateCurrentNode();
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            SaveService.saveTree(JMeterTreeLoader.toHashTree(model), output);
            JMeterVirtualFileWriter.write(file, output.toByteArray());
            JMeterIdeNotifications.info(project, "Saved " + file.getName());
            return true;
        } catch (Exception exception) {
            JMeterIdeNotifications.error(project, "Unable to save JMX: " + exception.getMessage());
            errors.showError("Unable to save JMX file: " + exception.getMessage());
            return false;
        }
    }
}
