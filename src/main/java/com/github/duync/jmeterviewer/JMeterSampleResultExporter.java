package com.github.duync.jmeterviewer;

import org.apache.jmeter.samplers.SampleResult;

import java.io.*;
import java.nio.charset.StandardCharsets;

final class JMeterSampleResultExporter {
    private JMeterSampleResultExporter() {
    }

    static void csv(JMeterSampleResultTableModel model, File file) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write("label,success,thread,time,latency,connect,responseCode,responseMessage,url,bytes\n");
            for (int i = 0; i < model.getRowCount(); i++) {
                SampleResult result = model.get(i);
                if (result != null) {
                    writer.write(JMeterResultDetails.csv(result));
                    writer.write("\n");
                }
            }
        }
    }

    static void jtlXml(JMeterSampleResultTableModel model, File file) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<testResults version=\"1.2\">\n");
            for (int i = 0; i < model.getRowCount(); i++) {
                SampleResult result = model.get(i);
                if (result != null) {
                    writeSample(writer, result);
                }
            }
            writer.write("</testResults>\n");
        }
    }

    static void jtlCsv(JMeterSampleResultTableModel model, File file) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write("timeStamp,elapsed,label,responseCode,responseMessage,threadName,success,bytes,sentBytes,grpThreads,allThreads,URL,Latency,IdleTime,Connect\n");
            for (int i = 0; i < model.getRowCount(); i++) {
                SampleResult result = model.get(i);
                if (result != null) {
                    writer.write(jtlCsvLine(result));
                    writer.write("\n");
                }
            }
        }
    }

    private static void writeSample(Writer writer, SampleResult result) throws IOException {
        writer.write("  <httpSample t=\"");
        writer.write(String.valueOf(result.getTime()));
        writer.write("\" lt=\"");
        writer.write(String.valueOf(result.getLatency()));
        writer.write("\" ct=\"");
        writer.write(String.valueOf(result.getConnectTime()));
        writer.write("\" ts=\"");
        writer.write(String.valueOf(result.getStartTime()));
        writer.write("\" s=\"");
        writer.write(String.valueOf(result.isSuccessful()));
        writer.write("\" lb=\"");
        writer.write(xml(result.getSampleLabel()));
        writer.write("\" rc=\"");
        writer.write(xml(result.getResponseCode()));
        writer.write("\" rm=\"");
        writer.write(xml(result.getResponseMessage()));
        writer.write("\" tn=\"");
        writer.write(xml(result.getThreadName()));
        writer.write("\" by=\"");
        writer.write(String.valueOf(result.getBytesAsLong()));
        writer.write("\" sby=\"");
        writer.write(String.valueOf(result.getSentBytes()));
        writer.write("\"/>\n");
    }

    private static String jtlCsvLine(SampleResult result) {
        return result.getStartTime()
                + "," + result.getTime()
                + "," + quote(result.getSampleLabel())
                + "," + quote(result.getResponseCode())
                + "," + quote(result.getResponseMessage())
                + "," + quote(result.getThreadName())
                + "," + result.isSuccessful()
                + "," + result.getBytesAsLong()
                + "," + result.getSentBytes()
                + "," + result.getGroupThreads()
                + "," + result.getAllThreads()
                + "," + quote(result.getUrlAsString())
                + "," + result.getLatency()
                + "," + result.getIdleTime()
                + "," + result.getConnectTime();
    }

    private static String quote(String value) {
        String text = value == null ? "" : value;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private static String xml(String value) {
        String text = value == null ? "" : value;
        return text.replace("&", "&amp;").replace("\"", "&quot;")
                .replace("<", "&lt;").replace(">", "&gt;");
    }
}
