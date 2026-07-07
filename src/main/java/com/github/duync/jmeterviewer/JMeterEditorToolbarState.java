package com.github.duync.jmeterviewer;

import javax.swing.JButton;
import javax.swing.JLabel;

final class JMeterEditorToolbarState {
    JButton saveButton;
    JButton reloadButton;
    JButton runButton;
    JButton stopButton;
    JButton shutdownButton;
    JButton resetEnginesButton;
    JButton exitEnginesButton;
    JLabel runStatusLabel;
    JMeterResultFileLoader resultFileLoader;
    JMeterResultExportActions exportActions;
    JMeterReportAction reportAction;
    JMeterValidationAction validationAction;
    JMeterStatsAction statsAction;
    JMeterRunOptions runOptions;
    JMeterThreadControlPanel threadControl;
    JMeterResultsPanel resultsPanel;

    JMeterEditorToolbarState(JButton saveButton,
                             JButton reloadButton,
                             JButton runButton,
                             JButton stopButton,
                             JButton shutdownButton,
                             JButton resetEnginesButton,
                             JButton exitEnginesButton,
                             JLabel runStatusLabel,
                             JMeterResultFileLoader resultFileLoader,
                             JMeterResultExportActions exportActions,
                             JMeterReportAction reportAction,
                             JMeterValidationAction validationAction,
                             JMeterStatsAction statsAction,
                             JMeterRunOptions runOptions,
                             JMeterThreadControlPanel threadControl,
                             JMeterResultsPanel resultsPanel) {
        this.saveButton = saveButton;
        this.reloadButton = reloadButton;
        this.runButton = runButton;
        this.stopButton = stopButton;
        this.shutdownButton = shutdownButton;
        this.resetEnginesButton = resetEnginesButton;
        this.exitEnginesButton = exitEnginesButton;
        this.runStatusLabel = runStatusLabel;
        this.resultFileLoader = resultFileLoader;
        this.exportActions = exportActions;
        this.reportAction = reportAction;
        this.validationAction = validationAction;
        this.statsAction = statsAction;
        this.runOptions = runOptions;
        this.threadControl = threadControl;
        this.resultsPanel = resultsPanel;
    }
}
