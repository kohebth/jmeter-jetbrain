package com.github.duync.jmeterviewer;

import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class JMeterNativeVisualizerPanelTest {
    @Test
    void createsViewResultsTreeWithActivatedJMeterContext() throws Exception {
        EmbeddedJMeterRuntime.ensureReady();
        ClassLoader previous = JMeterPluginClasspath.activateThread();
        try {
            Object instance = JMeterPluginClasspath
                    .loadClass("org.apache.jmeter.visualizers.ViewResultsFullVisualizer")
                    .getDeclaredConstructor()
                    .newInstance();

            assertInstanceOf(JComponent.class, instance);
        } finally {
            JMeterPluginClasspath.restoreThread(previous);
        }
    }
}
