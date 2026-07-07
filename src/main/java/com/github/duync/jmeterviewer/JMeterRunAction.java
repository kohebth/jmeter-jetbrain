package com.github.duync.jmeterviewer;

public final class JMeterRunAction extends JMeterEditorAction {
    @Override
    protected void perform(JMeterVisualFileEditor editor) {
        editor.runTest();
    }
}
