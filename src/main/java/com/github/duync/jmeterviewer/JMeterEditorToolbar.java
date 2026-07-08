package com.github.duync.jmeterviewer;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.JBPanel;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Icon;

final class JMeterEditorToolbar {
    private JMeterEditorToolbar() {
    }

    static JComponent create(JButton saveButton,
                             JButton saveAsButton,
                             JButton reloadButton,
                             JButton runButton,
                             JButton runSelectedButton,
                             JButton runLocalButton,
                             JButton runRemoteButton,
                             JButton runAllButton,
                             JButton stopButton,
                             JButton shutdownButton,
                             JButton resetEnginesButton,
                             JButton exitEnginesButton,
                             JButton loadJtlButton,
                             JButton exportSamplesButton,
                             JButton exportJtlXmlButton,
                             JButton exportJtlCsvButton,
                             JButton exportLogButton,
                             JButton htmlReportButton,
                             JButton validateButton,
                             JButton statsButton,
                             JLabel runStatusLabel,
                             JMeterRunOptions runOptions,
                             JMeterThreadControlPanel threadControl,
                             JMeterResultsPanel resultsPanel,
                             JMeterTreeActions actions,
                             JMeterTreeFileActions fileActions,
                             JMeterAddElementDialog addDialog,
                             JMeterTemplateDialog templates,
                             JMeterCommandPalette commandPalette,
                             JMeterSearchController search) {
        JPanel toolbar = new JBPanel<>();
        toolbar.add(compact(saveButton, "Save", AllIcons.Actions.MenuSaveall));
        toolbar.add(compact(saveAsButton, "Save As", AllIcons.Actions.MenuSaveall));
        toolbar.add(compact(reloadButton, "Reload", AllIcons.Actions.Refresh));
        toolbar.add(compact(runButton, "Run Plan", AllIcons.Actions.Execute));
        toolbar.add(compact(runSelectedButton, "Run Thread Group", AllIcons.Actions.RunToCursor));
        toolbar.add(compact(runLocalButton, "Run Local Plan", AllIcons.Actions.Execute));
        toolbar.add(compact(runRemoteButton, "Run Remote Plan", AllIcons.Actions.Upload));
        toolbar.add(compact(runAllButton, "Run Local and Remote", AllIcons.Actions.RunAll));
        toolbar.add(compact(stopButton, "Stop", AllIcons.Actions.Cancel));
        toolbar.add(compact(shutdownButton, "Shutdown", AllIcons.Actions.Suspend));
        toolbar.add(compact(resetEnginesButton, "Reset Engines", AllIcons.Actions.Restart));
        toolbar.add(compact(exitEnginesButton, "Exit Engines", AllIcons.Actions.Exit));
        toolbar.add(runOptions.component());
        toolbar.add(threadControl.component());
        toolbar.add(button("Clear Results", AllIcons.Actions.GC, resultsPanel::clear));
        toolbar.add(button("Clear Samples", AllIcons.Actions.DeleteTag, resultsPanel::clearResults));
        toolbar.add(button("Clear Log", AllIcons.Actions.DeleteTag, resultsPanel::clearLog));
        toolbar.add(compact(loadJtlButton, "Load JTL", AllIcons.Actions.Download));
        toolbar.add(compact(exportSamplesButton, "Export Samples", AllIcons.Actions.Upload));
        toolbar.add(compact(exportJtlXmlButton, "Export JTL XML", AllIcons.Actions.Upload));
        toolbar.add(compact(exportJtlCsvButton, "Export JTL CSV", AllIcons.Actions.Upload));
        toolbar.add(compact(exportLogButton, "Export Log", AllIcons.Actions.Upload));
        toolbar.add(compact(htmlReportButton, "HTML Report", AllIcons.Actions.PreviewDetails));
        toolbar.add(button("Next Failure", AllIcons.Actions.NextOccurence, resultsPanel::selectNextFailure));
        toolbar.add(button("Previous Failure", AllIcons.Actions.PreviousOccurence, resultsPanel::selectPreviousFailure));
        toolbar.add(compact(validateButton, "Validate", AllIcons.Actions.Checked));
        toolbar.add(compact(statsButton, "Statistics", AllIcons.Actions.Profile));
        toolbar.add(runStatusLabel);
        toolbar.add(compact(addDialog.button(), "Add Element", AllIcons.General.Add));
        toolbar.add(compact(templates.button(), "Templates", AllIcons.Actions.ListFiles));
        toolbar.add(compact(commandPalette.button(), "Commands", AllIcons.Actions.Run_anything));
        toolbar.add(button("Delete", AllIcons.Actions.DeleteTag, actions::deleteSelected));
        toolbar.add(button("Duplicate", AllIcons.Actions.Copy, actions::duplicateSelected));
        toolbar.add(button("Duplicate Disabled", AllIcons.Actions.Copy, actions::duplicateSelectedDisabled));
        toolbar.add(button("Copy", AllIcons.Actions.Copy, actions::copySelected));
        toolbar.add(button("Cut", AllIcons.Actions.MenuCut, actions::cutSelected));
        toolbar.add(button("Paste", AllIcons.Actions.MenuPaste, actions::pasteIntoSelected));
        toolbar.add(button("Import JMX", AllIcons.Actions.Download, fileActions::importJmx));
        toolbar.add(button("Export Node", AllIcons.Actions.Upload, fileActions::exportSelected));
        toolbar.add(button("Export Names", AllIcons.Actions.Upload, fileActions::exportNames));
        toolbar.add(button("Copy Names", AllIcons.Actions.Copy, fileActions::copyNames));
        toolbar.add(button("Copy Outline", AllIcons.Actions.Copy, fileActions::copyOutline));
        toolbar.add(button("Copy Code", AllIcons.Actions.ShowCode, fileActions::copyCodeOutline));
        toolbar.add(button("Toggle Enabled", AllIcons.Actions.Checked, actions::toggleSelectedEnabled));
        toolbar.add(button("Enable", AllIcons.General.InspectionsOK, actions::enableSelected));
        toolbar.add(button("Disable", AllIcons.Actions.Cancel, actions::disableSelected));
        toolbar.add(button("Enable Tree", AllIcons.Actions.Expandall, actions::enableSelectedTree));
        toolbar.add(button("Disable Tree", AllIcons.Actions.Collapseall, actions::disableSelectedTree));
        toolbar.add(button("Move Up", AllIcons.Actions.MoveUp, actions::moveSelectedUp));
        toolbar.add(button("Move Down", AllIcons.Actions.MoveDown, actions::moveSelectedDown));
        toolbar.add(button("Wrap in Simple Controller", AllIcons.Actions.SplitVertically, actions::insertSimpleControllerParent));
        toolbar.add(button("Change Parent to Simple Controller", AllIcons.Actions.ChangeView, actions::changeSelectedParentToSimpleController));
        toolbar.add(button("Add Think Times", AllIcons.Actions.Pause, actions::addThinkTimes));
        toolbar.add(button("Expand", AllIcons.Actions.ArrowExpand, actions::expandSelected));
        toolbar.add(button("Collapse", AllIcons.Actions.ArrowCollapse, actions::collapseSelected));
        toolbar.add(new JLabel(AllIcons.Actions.Search));
        toolbar.add(search.field());
        toolbar.add(button("Find Next", AllIcons.Actions.FindAndShowNextMatchesSmall, search::findNext));
        toolbar.add(button("Find Previous", AllIcons.Actions.FindAndShowPrevMatchesSmall, search::findPrevious));
        toolbar.add(button("Search Plan", AllIcons.Actions.Search, search::showDialog));
        toolbar.add(search.statusLabel());
        return toolbar;
    }

    private static JButton compact(JButton button, String tooltip, Icon icon) {
        return JMeterIconButtons.compact(button, tooltip, icon);
    }

    private static JButton button(String tooltip, Icon icon, Runnable action) {
        return JMeterIconButtons.create(tooltip, icon, action);
    }
}
