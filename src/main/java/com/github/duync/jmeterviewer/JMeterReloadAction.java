package com.github.duync.jmeterviewer;

public final class JMeterReloadAction extends JMeterEditorAction {
    @Override
    protected void perform(JMeterVisualFileEditor editor) {
        editor.reloadFromFile();
    }
}
