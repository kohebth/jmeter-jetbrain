package com.github.duync.jmeterviewer;

public final class JMeterStopAction extends JMeterEditorAction {
    @Override
    protected void perform(JMeterVisualFileEditor editor) {
        editor.stopTest();
    }
}
