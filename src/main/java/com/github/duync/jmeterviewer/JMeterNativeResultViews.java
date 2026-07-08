package com.github.duync.jmeterviewer;

import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;

import javax.swing.*;
import java.util.*;

final class JMeterNativeResultViews {
    private final EnumMap<JMeterNativeResultView, JMeterNativeVisualizerPanel> panels =
            new EnumMap<>(JMeterNativeResultView.class);

    JMeterNativeResultViews() {
        for (JMeterNativeResultView view : JMeterNativeResultView.values()) {
            panels.put(view, new JMeterNativeVisualizerPanel(view.title(), view.visualizerClass()));
        }
    }

    JComponent component(JMeterNativeResultView view) {
        return panels.get(view).component();
    }

    void clear() {
        panels.values().forEach(JMeterNativeVisualizerPanel::clear);
    }

    void add(SampleResult result) {
        panels.values().forEach(panel -> panel.add(result));
    }

    void configure(JMeterNativeResultView view, TestElement element) {
        panels.get(view).configure(element);
    }

    void configureFromModel(JMeterTreeModel model) {
        for (JMeterNativeResultView view : JMeterNativeResultView.values()) {
            configure(view, JMeterViewResultsTreeLocator.find(model, view.guiClasses()));
        }
    }

    EnumSet<JMeterNativeResultView> availableViews(JMeterTreeModel model) {
        EnumSet<JMeterNativeResultView> available = EnumSet.noneOf(JMeterNativeResultView.class);
        for (JMeterNativeResultView view : JMeterNativeResultView.values()) {
            if (JMeterViewResultsTreeLocator.find(model, view.guiClasses()) != null) {
                available.add(view);
            }
        }
        return available;
    }

    JMeterNativeResultView matchingView(TestElement element) {
        for (JMeterNativeResultView view : JMeterNativeResultView.values()) {
            if (JMeterViewResultsTreeLocator.hasGuiClass(element, view.guiClasses())) {
                return view;
            }
        }
        return null;
    }
}
