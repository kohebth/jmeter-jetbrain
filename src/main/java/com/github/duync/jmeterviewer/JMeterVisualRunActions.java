package com.github.duync.jmeterviewer;

import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;

import javax.swing.JTree;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class JMeterVisualRunActions {
    private final Supplier<JMeterTreeModel> model;
    private final Supplier<JTree> tree;
    private final Runnable updateCurrentNode;
    private final JMeterResultsWorkspace resultsWorkspace;
    private final JMeterResultsPanel resultsPanel;
    private final JMeterThreadGroupActivity threadGroupActivity;
    private final JMeterRunController runController;
    private final JMeterRunOptions runOptions;
    private final Consumer<String> runStatus;

    JMeterVisualRunActions(Supplier<JMeterTreeModel> model,
                           Supplier<JTree> tree,
                           Runnable updateCurrentNode,
                           JMeterResultsWorkspace resultsWorkspace,
                           JMeterResultsPanel resultsPanel,
                           JMeterThreadGroupActivity threadGroupActivity,
                           JMeterRunController runController,
                           JMeterRunOptions runOptions,
                           Consumer<String> runStatus) {
        this.model = model;
        this.tree = tree;
        this.updateCurrentNode = updateCurrentNode;
        this.resultsWorkspace = resultsWorkspace;
        this.resultsPanel = resultsPanel;
        this.threadGroupActivity = threadGroupActivity;
        this.runController = runController;
        this.runOptions = runOptions;
        this.runStatus = runStatus;
    }

    void runAuto() {
        run(JMeterRunController.RunTarget.AUTO, false);
    }

    void runLocal() {
        run(JMeterRunController.RunTarget.LOCAL, false);
    }

    void runRemote() {
        run(JMeterRunController.RunTarget.REMOTE, false);
    }

    void runLocalAndRemote() {
        run(JMeterRunController.RunTarget.LOCAL_AND_REMOTE, false);
    }

    void runSelectedThreadGroup() {
        run(JMeterRunController.RunTarget.LOCAL, true);
    }

    private void run(JMeterRunController.RunTarget target, boolean selectedThreadGroupOnly) {
        JMeterTreeModel currentModel = model.get();
        if (currentModel == null || runController.isRunning()) {
            return;
        }

        updateCurrentNode.run();
        resultsPanel.configureNativeResultViews(currentModel);
        resultsPanel.clear();
        threadGroupActivity.prepare(currentModel);
        threadGroupActivity.start();
        resultsPanel.appendDiagnostic("Starting test");
        resultsPanel.runStarted();
        resultsWorkspace.showViewResultsTree();
        runStatus.accept("Starting");
        if (selectedThreadGroupOnly) {
            runController.startSelectedThreadGroup(currentModel, selectedTreeNode(), runOptions, target);
        } else {
            runController.start(currentModel, runOptions, target);
        }
    }

    private JMeterTreeNode selectedTreeNode() {
        JTree currentTree = tree.get();
        if (currentTree == null || currentTree.getSelectionPath() == null) {
            return null;
        }
        Object component = currentTree.getSelectionPath().getLastPathComponent();
        return component instanceof JMeterTreeNode ? (JMeterTreeNode) component : null;
    }
}
