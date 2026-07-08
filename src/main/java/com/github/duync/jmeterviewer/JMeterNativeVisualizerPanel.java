package com.github.duync.jmeterviewer;

import com.intellij.ui.components.JBScrollPane;
import org.apache.jmeter.gui.JMeterGUIComponent;
import org.apache.jmeter.samplers.Clearable;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.visualizers.Visualizer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;

final class JMeterNativeVisualizerPanel {
    private final JPanel panel = new JPanel(new BorderLayout());
    private final String label;
    private final String className;
    private Visualizer visualizer;
    private Clearable clearable;
    private JMeterGUIComponent guiComponent;
    private JTextArea fallback;
    private boolean loaded;

    JMeterNativeVisualizerPanel(String label, String className) {
        this.label = label;
        this.className = className;
        panel.add(new JLabel("Open this tab to load " + label), BorderLayout.NORTH);
        panel.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && panel.isShowing()) {
                load();
            }
        });
    }

    JComponent component() {
        return panel;
    }

    void clear() {
        if (clearable != null) {
            clearable.clearData();
        }
        if (fallback != null) {
            fallback.setCaretPosition(0);
        }
    }

    void configure(TestElement element) {
        if (element == null) {
            return;
        }
        load();
        if (guiComponent != null) {
            guiComponent.configure(element);
        }
    }

    void add(SampleResult result) {
        if (visualizer != null && result != null) {
            visualizer.add(result);
        }
    }

    private void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Visualizer createdVisualizer = null;
        Clearable createdClearable = null;
        JMeterGUIComponent createdGui = null;
        JTextArea error = null;
        panel.removeAll();
        ClassLoader previousLoader = Thread.currentThread().getContextClassLoader();
        try {
            EmbeddedJMeterRuntime.ensureReady();
            previousLoader = JMeterPluginClasspath.activateThread();
            Object instance = JMeterPluginClasspath.loadClass(className).getDeclaredConstructor().newInstance();
            createdVisualizer = instance instanceof Visualizer ? (Visualizer) instance : null;
            createdClearable = instance instanceof Clearable ? (Clearable) instance : null;
            createdGui = instance instanceof JMeterGUIComponent ? (JMeterGUIComponent) instance : null;
            if (instance instanceof JComponent) {
                JComponent component = (JComponent) instance;
                component.setName(label);
                JMeterTabOverflowSupport.apply(component);
                panel.add(component, BorderLayout.CENTER);
            } else {
                error = new JTextArea(className + " is not a Swing component.");
                error.setEditable(false);
                panel.add(new JBScrollPane(error), BorderLayout.CENTER);
            }
        } catch (Exception | LinkageError exception) {
            error = new JTextArea("Unable to create " + label + ":\n" + rootCause(exception));
            error.setEditable(false);
            panel.add(new JBScrollPane(error), BorderLayout.CENTER);
        } finally {
            JMeterPluginClasspath.restoreThread(previousLoader);
        }
        visualizer = createdVisualizer;
        clearable = createdClearable;
        guiComponent = createdGui;
        fallback = error;
        panel.revalidate();
        panel.repaint();
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof java.lang.reflect.InvocationTargetException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
