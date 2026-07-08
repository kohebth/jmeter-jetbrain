package com.github.duync.jmeterviewer;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.BorderLayout;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

public final class JMeterVisualFileEditor implements FileEditor, Disposable {
    private final VirtualFile file;
    private final Project project;
    private final JPanel component;
    private final JMeterElementPanel elementPanel;
    private final JEditorPane errorPane;
    private final PropertyChangeSupport propertyChangeSupport;
    private final JButton saveButton, reloadButton, runButton, runSelectedButton, runLocalButton;
    private final JButton runRemoteButton, runAllButton, stopButton, shutdownButton, resetEnginesButton, exitEnginesButton;
    private final JMeterEditorToolbarState toolbarState;
    private final JMeterValidationAction validationAction;
    private final JMeterStatsAction statsAction;
    private final JMeterResultExportActions exportActions;
    private final JMeterResultFileLoader resultFileLoader;
    private final JMeterReportAction reportAction;
    private final JLabel runStatusLabel;
    private final JMeterResultsWorkspace resultsWorkspace;
    private final JMeterResultsPanel resultsPanel;
    private final JMeterRunOptions runOptions;
    private final JMeterThreadControlPanel threadControl;
    private final JMeterThreadGroupActivity threadGroupActivity;
    private final JMeterRunController runController;
    private final JMeterVisualRunActions runActions;
    private final JMeterIdeUndoSupport undoSupport;
    private final UserDataHolderBase userData;
    private final Disposable editorDisposable;
    private JMeterTreeModel model;
    private JTree tree;
    private JMeterTreeActions treeActions;
    private JMeterCommandPalette commandPalette;
    private JMeterTemplateDialog templateDialog;
    private boolean modified;
    private boolean updatingCurrentNode;
    private boolean disposed;
    private boolean suppressNextFileChange;
    private int suppressedGuiDirtyEvents;

    public JMeterVisualFileEditor(Project project, VirtualFile file) {
        this.project = project;
        this.file = file;
        this.component = new JBPanel<>(new BorderLayout());
        this.elementPanel = new JMeterElementPanel(this::markGuiModified);
        this.errorPane = new JEditorPane("text/plain", "");
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        this.editorDisposable = Disposer.newDisposable("JMeter visual editor " + file.getPath());
        Disposer.register(project, editorDisposable);
        this.saveButton = new JButton("Save");
        this.reloadButton = new JButton("Reload");
        this.runButton = new JButton("Run Plan");
        this.runSelectedButton = new JButton("Run Thread Group");
        this.runLocalButton = new JButton("Run Local Plan");
        this.runRemoteButton = new JButton("Run Remote Plan");
        this.runAllButton = new JButton("Run Local+Remote");
        this.stopButton = new JButton("Stop");
        this.shutdownButton = new JButton("Shutdown");
        this.resetEnginesButton = new JButton("Reset Engines");
        this.exitEnginesButton = new JButton("Exit Engines");
        this.runStatusLabel = new JLabel("Idle");
        this.resultsWorkspace = JMeterResultsWorkspace.get(project);
        this.resultsPanel = resultsWorkspace.resultsPanel();
        this.exportActions = new JMeterResultExportActions(project, file, resultsPanel);
        this.resultFileLoader = new JMeterResultFileLoader(project, resultsPanel);
        this.reportAction = new JMeterReportAction(project, resultsPanel);
        this.validationAction = new JMeterValidationAction(() -> model, resultsPanel);
        this.statsAction = new JMeterStatsAction(() -> model, resultsPanel);
        this.runOptions = new JMeterRunOptions(project);
        this.threadGroupActivity = new JMeterThreadGroupActivity();
        this.runController = new JMeterRunController(
                new JMeterEditorRunListener(this::setRunStatus, resultsPanel, threadGroupActivity));
        this.runActions = new JMeterVisualRunActions(() -> model, () -> tree, this::flushGuiChanges,
                resultsWorkspace, resultsPanel, threadGroupActivity, runController, runOptions, this::setRunStatus);
        this.threadControl = new JMeterThreadControlPanel(runController);
        this.undoSupport = new JMeterIdeUndoSupport(project, file, this::restoreModel);
        this.userData = new UserDataHolderBase();
        this.toolbarState = new JMeterEditorToolbarState(saveButton, reloadButton, runButton, runSelectedButton,
                runLocalButton,
                runRemoteButton, runAllButton, stopButton,
                shutdownButton, resetEnginesButton, exitEnginesButton, runStatusLabel, resultFileLoader,
                exportActions, reportAction, validationAction, statsAction, runOptions, threadControl, resultsPanel);

        JMeterEditorControls.wire(
                component,
                editorDisposable,
                toolbarState,
                this::save,
                this::reloadFromFile,
                runActions::runAuto,
                runActions::runSelectedThreadGroup,
                runActions::runLocal,
                runActions::runRemote,
                runActions::runLocalAndRemote,
                runController,
                validationAction,
                this::showCommands
        );
        JMeterEditorShortcuts.installTreeEditing(component, editorDisposable, () -> treeActions);
        JMeterFileChangeWatcher.install(file, editorDisposable, this::handleExternalFileChange, this::load);
        load();
    }

    void reloadFromFile() {
        JMeterActionTrace.info("editor.reload.request", traceState());
        if (JMeterReloadGuard.canDiscard(project, modified)) {
            load();
        } else {
            JMeterActionTrace.info("editor.reload.cancelled", traceState());
        }
    }

    private void load() {
        try {
            JMeterActionTrace.info("editor.load.start", traceState());
            EmbeddedJMeterRuntime.ensureReady();
            JMeterPluginClasspathStore.get(project).applyToClasspath();
            model = JMeterTreeLoader.load(new File(file.getPath()));
            undoSupport.reset(model);
            installModel();
            setModified(false);
            JMeterActionTrace.info("editor.load.success", traceState());
        } catch (Exception exception) {
            JMeterActionTrace.warn("editor.load.failed " + traceState(), exception);
            JMeterIdeNotifications.error(project, "Unable to load JMX: " + exception.getMessage());
            showLoadError(exception);
        }
    }

    private void installModel() {
        JMeterActionTrace.info("editor.model.install", traceState());
        JMeterVisualModelInstaller.Installed installed = JMeterVisualModelInstaller.install(
                project, model, component, toolbarState, elementPanel, resultsPanel, resultsWorkspace,
                threadGroupActivity, this::markTreeModified);
        tree = installed.tree();
        treeActions = installed.treeActions();
        commandPalette = installed.commandPalette();
        templateDialog = installed.templateDialog();
        refreshResultTabs();
    }

    void showCommands() { if (commandPalette != null) commandPalette.show(); }

    void showTemplates() { if (templateDialog != null) templateDialog.show(); }

    private void restoreModel(JMeterTreeModel restoredModel) {
        model = restoredModel;
        installModel();
        setModified(true);
    }

    void save() { save(true); }

    void saveSilently() { save(false); }

    private void save(boolean notifySuccess) {
        JMeterActionTrace.info("editor.save.start", traceState());
        flushGuiChanges();
        suppressNextFileChange = true;
        if (JMeterFileSaver.save(project, file, model, elementPanel, notifySuccess)) {
            setModified(false);
            JMeterActionTrace.info("editor.save.success", traceState());
        } else {
            suppressNextFileChange = false;
            JMeterActionTrace.info("editor.save.failed", traceState());
        }
    }

    private void handleExternalFileChange() {
        if (suppressNextFileChange) {
            suppressNextFileChange = false;
            JMeterActionTrace.info("editor.file.change.ignored", traceState());
            return;
        }
        JMeterActionTrace.info("editor.file.change.external", traceState());
        if (JMeterReloadGuard.canReloadExternalChange(project, modified)) {
            load();
        } else {
            JMeterActionTrace.info("editor.file.change.reload.cancelled", traceState());
        }
    }

    void runTest() {
        JMeterActionTrace.info("editor.run.action", traceState());
        runActions.runAuto();
    }

    void stopTest() {
        JMeterActionTrace.info("editor.stop.action", traceState());
        runController.stop();
    }

    void validatePlan() { validationAction.validateNow(); }

    private void setRunStatus(String status) {
        JMeterActionTrace.info("editor.run.status", "status=\"" + status + "\" " + traceState());
        runStatusLabel.setText(status);
        boolean running = runController.isRunning();
        runButton.setEnabled(!running);
        runSelectedButton.setEnabled(!running);
        runLocalButton.setEnabled(!running);
        runRemoteButton.setEnabled(!running);
        runAllButton.setEnabled(!running);
        stopButton.setEnabled(running);
        shutdownButton.setEnabled(running);
    }

    private void markGuiModified() {
        if (updatingCurrentNode || suppressedGuiDirtyEvents > 0) {
            JMeterActionTrace.info("editor.gui.dirty.suppressed",
                    "updating=" + updatingCurrentNode + " suppressed=" + suppressedGuiDirtyEvents + " "
                            + traceState());
            return;
        }
        JMeterActionTrace.info("editor.gui.dirty", traceState());
        setModified(true);
    }

    private void flushGuiChanges() {
        updateCurrentJMeterNode(true);
    }

    private void updateCurrentJMeterNode() {
        updateCurrentJMeterNode(false);
    }

    private void updateCurrentJMeterNode(boolean rearmGui) {
        try {
            updatingCurrentNode = true;
            GuiPackage guiPackage = GuiPackage.getInstance();
            if (guiPackage == null || model == null || tree == null) {
                JMeterActionTrace.info("editor.gui.flush.skipped", traceState());
                return;
            }
            JMeterActionTrace.info("editor.gui.flush.start", traceState());
            guiPackage.updateCurrentNode();
            JMeterTreeNode currentNode = guiPackage.getCurrentNode();
            if (currentNode != null) {
                model.nodeChanged(currentNode);
                tree.repaint();
            }
            if (rearmGui) {
                suppressDelayedGuiDirty();
                elementPanel.showSelected();
            }
            JMeterActionTrace.info("editor.gui.flush.done", traceState());
        } finally {
            updatingCurrentNode = false;
        }
    }

    private void suppressDelayedGuiDirty() {
        suppressedGuiDirtyEvents++;
        SwingUtilities.invokeLater(() -> {
            if (suppressedGuiDirtyEvents > 0) {
                suppressedGuiDirtyEvents--;
            }
            JMeterActionTrace.info("editor.gui.dirty.suppression.done", traceState());
        });
    }

    private void setModified(boolean modified) {
        boolean oldValue = this.modified;
        this.modified = modified;
        saveButton.setEnabled(modified);
        propertyChangeSupport.firePropertyChange(FileEditor.PROP_MODIFIED, oldValue, modified);
        if (oldValue != modified) {
            JMeterActionTrace.info("editor.modified",
                    "old=" + oldValue + " new=" + modified + " " + traceState());
        }
    }

    private void markTreeModified() {
        JMeterActionTrace.info("editor.tree.dirty", traceState());
        undoSupport.record(model);
        refreshResultTabs();
        setModified(true);
    }

    private void refreshResultTabs() {
        if (model == null) {
            return;
        }
        resultsWorkspace.updateNativeResultViews(resultsPanel.availableNativeResultViews(model));
    }

    private void showLoadError(Exception exception) {
        errorPane.setEditable(false);
        errorPane.setText(exception.getMessage());
        component.removeAll();
        component.add(new JBScrollPane(errorPane));
    }

    @Override public @NotNull JComponent getComponent() { return component; }
    @Override public @Nullable JComponent getPreferredFocusedComponent() { return null; }
    @Override public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() { return "JMeter"; }
    @Override public @NotNull VirtualFile getFile() { return file; }
    @Override public void setState(@NotNull FileEditorState state) { }
    @Override public @NotNull FileEditorState getState(@NotNull FileEditorStateLevel level) { return FileEditorState.INSTANCE; }
    @Override public boolean isModified() { return modified; }
    @Override public boolean isValid() { return file.isValid(); }
    @Override public void selectNotify() { }

    @Override public void deselectNotify() { }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) { propertyChangeSupport.addPropertyChangeListener(listener); }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) { propertyChangeSupport.removePropertyChangeListener(listener); }

    @Override public @Nullable FileEditorLocation getCurrentLocation() { return null; }

    @Override public <T> @Nullable T getUserData(@NotNull Key<T> key) { return userData.getUserData(key); }

    @Override public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) { userData.putUserData(key, value); }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        JMeterActionTrace.info("editor.dispose", traceState());
        disposed = true;
        runController.exitEngines();
        Disposer.dispose(editorDisposable);
    }

    private String traceState() {
        return "file=" + JMeterActionTrace.file(file) + " node=" + JMeterActionTrace.currentNode()
                + " modified=" + modified;
    }
}
