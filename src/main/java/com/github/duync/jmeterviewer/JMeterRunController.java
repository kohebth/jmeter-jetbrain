package com.github.duync.jmeterviewer;

import org.apache.jmeter.engine.*;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.collections.HashTree;

import javax.swing.SwingUtilities;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

final class JMeterRunController {
    private final Listener listener;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final java.util.List<JMeterEngine> engines = new ArrayList<>();

    JMeterRunController(Listener listener) {
        this.listener = listener;
    }

    boolean isRunning() {
        return running.get();
    }

    void start(JMeterTreeModel model, JMeterRunOptions options) {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        try {
            JMeterPluginClasspath.activate();
            GuiPackage guiPackage = GuiPackage.getInstance();
            if (guiPackage != null) {
                guiPackage.updateCurrentNode();
            }
            if (options != null) {
                options.apply();
            }

            HashTree testTree = JMeterTreeLoader.toHashTree(model);
            EditorResultCollector collector = new EditorResultCollector(listener, () -> running.set(false));
            if (options != null && options.resultFile() != null) {
                collector.setFilename(options.resultFile());
            }
            testTree.add(collector);
            engines.clear();
            engines.addAll(createEngines(options));
            for (JMeterEngine engine : engines) {
                engine.configure(testTree);
            }
            notifyStatus("Running");
            for (JMeterEngine engine : engines) {
                engine.runTest();
            }
        } catch (Exception exception) {
            running.set(false);
            notifyStatus("Run failed: " + exception.getMessage());
            SwingUtilities.invokeLater(() -> listener.log("Run failed: " + exception));
        }
    }

    void stop() {
        if (running.get()) {
            stopAll(true, "Stopping");
        }
    }

    void shutdown() {
        if (!running.get()) {
            return;
        }
        notifyStatus("Shutting down");
        for (JMeterEngine engine : engines) {
            if (engine instanceof StandardJMeterEngine) {
                ((StandardJMeterEngine) engine).askThreadsToStop();
            } else {
                engine.stopTest(false);
            }
        }
    }

    void stopThread(String threadName, boolean now) {
        if (threadName == null || threadName.trim().isEmpty()) {
            return;
        }
        boolean stopped = now
                ? StandardJMeterEngine.stopThreadNow(threadName.trim())
                : StandardJMeterEngine.stopThread(threadName.trim());
        SwingUtilities.invokeLater(() -> listener.log((stopped ? "Stopped " : "No thread named ") + threadName));
    }

    void resetEngines() {
        for (JMeterEngine engine : engines) {
            engine.reset();
        }
        SwingUtilities.invokeLater(() -> listener.log("Reset JMeter engines"));
    }

    void exitEngines() {
        for (JMeterEngine engine : engines) {
            engine.exit();
        }
        engines.clear();
        running.set(false);
        notifyStatus("Idle");
        SwingUtilities.invokeLater(() -> listener.log("Exited JMeter engines"));
    }

    private java.util.List<JMeterEngine> createEngines(JMeterRunOptions options) throws Exception {
        java.util.List<String> hosts = options == null ? Collections.emptyList() : options.remoteHosts();
        if (hosts.isEmpty()) {
            return Collections.singletonList(new StandardJMeterEngine());
        }
        java.util.List<JMeterEngine> remoteEngines = new ArrayList<>();
        for (String host : hosts) {
            remoteEngines.add(new ClientJMeterEngine(host));
            SwingUtilities.invokeLater(() -> listener.log("Configured remote engine " + host));
        }
        return remoteEngines;
    }

    private void stopAll(boolean now, String status) {
        notifyStatus(status);
        for (JMeterEngine engine : engines) {
            engine.stopTest(now);
        }
    }

    private void notifyStatus(String status) {
        SwingUtilities.invokeLater(() -> listener.statusChanged(status));
    }

    interface Listener {
        void statusChanged(String status);

        void log(String message);

        void sampleOccurred(SampleResult result);
    }

    private static final class EditorResultCollector extends ResultCollector {
        private final Listener listener;
        private final Runnable finished;

        private EditorResultCollector(Listener listener, Runnable finished) {
            this.listener = listener;
            this.finished = finished;
        }

        @Override
        public void testStarted() {
            SwingUtilities.invokeLater(() -> {
                listener.statusChanged("Running");
                listener.log("Test started");
            });
        }

        @Override
        public void testEnded() {
            finished.run();
            SwingUtilities.invokeLater(() -> {
                listener.statusChanged("Finished");
                listener.log("Test finished");
            });
        }

        @Override
        public void sampleOccurred(SampleEvent event) {
            SampleResult result = event.getResult();
            SwingUtilities.invokeLater(() -> listener.sampleOccurred(result));
        }
    }
}
