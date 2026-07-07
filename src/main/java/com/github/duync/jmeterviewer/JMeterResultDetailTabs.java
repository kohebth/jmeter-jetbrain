package com.github.duync.jmeterviewer;

import com.intellij.ui.components.JBScrollPane;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.samplers.SampleResult;

import javax.swing.*;

final class JMeterResultDetailTabs {
    private final JTabbedPane tabs = new JTabbedPane();
    private final JTextArea sampler = area();
    private final JTextArea request = area();
    private final JTextArea response = area();
    private final JTextArea assertions = area();

    JMeterResultDetailTabs() {
        tabs.addTab("Sampler Result", new JBScrollPane(sampler));
        tabs.addTab("Request", new JBScrollPane(request));
        tabs.addTab("Response Data", new JBScrollPane(response));
        tabs.addTab("Assertions", new JBScrollPane(assertions));
    }

    JComponent component() {
        return tabs;
    }

    void clear() {
        set("", "", "", "");
    }

    void showSample(SampleResult result) {
        set(
                sampler(result),
                request(result),
                response(result),
                assertions(result.getAssertionResults())
        );
    }

    void showAssertion(AssertionResult result) {
        set(JMeterResultDetails.assertion(result), "", "", JMeterResultDetails.assertion(result));
        tabs.setSelectedIndex(3);
    }

    void showText(String text) {
        set(text, "", "", "");
    }

    private void set(String samplerText, String requestText, String responseText, String assertionText) {
        setText(sampler, samplerText);
        setText(request, requestText);
        setText(response, responseText);
        setText(assertions, assertionText);
    }

    private String sampler(SampleResult result) {
        return "Label: " + safe(result.getSampleLabel()) + "\n"
                + "Status: " + (result.isSuccessful() ? "OK" : "FAIL") + "\n"
                + "Thread: " + safe(result.getThreadName()) + "\n"
                + "Time: " + result.getTime() + " ms\n"
                + "Latency: " + result.getLatency() + " ms\n"
                + "Connect: " + result.getConnectTime() + " ms\n"
                + "Idle: " + result.getIdleTime() + " ms\n"
                + "Response Code: " + safe(result.getResponseCode()) + "\n"
                + "Response Message: " + safe(result.getResponseMessage()) + "\n"
                + "Content Type: " + safe(result.getContentType()) + "\n"
                + "Data Encoding: " + safe(result.getDataEncodingNoDefault()) + "\n"
                + "URL: " + safe(result.getUrlAsString()) + "\n"
                + "Bytes: " + result.getBytesAsLong() + "\n"
                + "Sent Bytes: " + result.getSentBytes() + "\n"
                + "Headers Size: " + result.getHeadersSize() + "\n"
                + "Body Size: " + result.getBodySizeAsLong() + "\n";
    }

    private String request(SampleResult result) {
        return "Request Headers:\n" + safe(result.getRequestHeaders()) + "\n"
                + "\nSampler Data:\n" + safe(result.getSamplerData());
    }

    private String response(SampleResult result) {
        return "Response Headers:\n" + safe(result.getResponseHeaders()) + "\n"
                + "\nResponse Data:\n" + safe(result.getResponseDataAsString());
    }

    private String assertions(AssertionResult[] results) {
        if (results == null || results.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (AssertionResult result : results) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(JMeterResultDetails.assertion(result));
        }
        return builder.toString();
    }

    private JTextArea area() {
        JTextArea area = new JTextArea(8, 80);
        area.setEditable(false);
        return area;
    }

    private void setText(JTextArea area, String text) {
        area.setText(text == null ? "" : text);
        area.setCaretPosition(0);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
