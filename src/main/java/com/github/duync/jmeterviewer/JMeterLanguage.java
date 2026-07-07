package com.github.duync.jmeterviewer;

import com.intellij.lang.xml.XMLLanguage;

public final class JMeterLanguage extends XMLLanguage {
    public static final JMeterLanguage INSTANCE = new JMeterLanguage();

    private JMeterLanguage() {
        super(XMLLanguage.INSTANCE, "JMeter", "application/x-jmeter-jmx");
    }
}
