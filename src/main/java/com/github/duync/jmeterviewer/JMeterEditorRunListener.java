package com.github.duync.jmeterviewer;

import org.apache.jmeter.samplers.SampleResult;

import java.util.function.Consumer;

final class JMeterEditorRunListener implements JMeterRunController.Listener {
    private final Consumer<String> statusConsumer;
    private final JMeterResultsPanel resultsPanel;

    JMeterEditorRunListener(Consumer<String> statusConsumer, JMeterResultsPanel resultsPanel) {
        this.statusConsumer = statusConsumer;
        this.resultsPanel = resultsPanel;
    }

    @Override
    public void statusChanged(String status) {
        statusConsumer.accept(status);
    }

    @Override
    public void log(String message) {
        resultsPanel.appendDiagnostic(message);
    }

    @Override
    public void sampleOccurred(SampleResult result) {
        resultsPanel.appendSample(result);
    }
}
