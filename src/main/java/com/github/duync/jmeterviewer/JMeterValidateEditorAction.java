package com.github.duync.jmeterviewer;

public final class JMeterValidateEditorAction extends JMeterEditorAction {
    @Override
    protected void perform(JMeterVisualFileEditor editor) {
        editor.validatePlan();
    }
}
