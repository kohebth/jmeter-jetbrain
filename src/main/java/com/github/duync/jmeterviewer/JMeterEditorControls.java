package com.github.duync.jmeterviewer;

import javax.swing.*;

final class JMeterEditorControls {
    private JMeterEditorControls() {
    }

    static void wire(JComponent component,
                     JButton saveButton,
                     JButton reloadButton,
                     JButton runButton,
                     JButton stopButton,
                     JButton shutdownButton,
                     JButton resetEnginesButton,
                     JButton exitEnginesButton,
                     Runnable save,
                     Runnable reload,
                     Runnable run,
                     JMeterRunController runController,
                     JMeterValidationAction validationAction,
                     Runnable commands) {
        saveButton.addActionListener(event -> save.run());
        reloadButton.addActionListener(event -> reload.run());
        saveButton.setEnabled(false);
        runButton.addActionListener(event -> run.run());
        stopButton.addActionListener(event -> runController.stop());
        shutdownButton.addActionListener(event -> runController.shutdown());
        resetEnginesButton.addActionListener(event -> runController.resetEngines());
        exitEnginesButton.addActionListener(event -> runController.exitEngines());
        stopButton.setEnabled(false);
        shutdownButton.setEnabled(false);
        JMeterEditorShortcuts.install(component, save, reload, run, runController::stop,
                validationAction::validateNow, commands);
    }
}
