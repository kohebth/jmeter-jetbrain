package com.github.kohebth.jmeterviewer.testplugin;

import org.apache.jmeter.gui.TestElementMetadata;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.TestBean;

/** Test-only component proving that the selected installation's lib/ext is scanned. */
@TestElementMetadata(labelResource = "displayName")
public final class ExternalSampler extends AbstractSampler implements TestBean {
    private static final long serialVersionUID = 1L;

    @Override
    public SampleResult sample(Entry entry) {
        SampleResult result = new SampleResult();
        result.setSampleLabel(getName());
        result.setSuccessful(true);
        return result;
    }
}
