package com.github.duync.jmeterviewer;

final class JMeterRunProfile {
    private final String remoteHosts;
    private final String resultFile;
    private final String logLevel;
    private final Advanced advanced;

    JMeterRunProfile(String remoteHosts, String resultFile, String logLevel, Advanced advanced) {
        this.remoteHosts = clean(remoteHosts);
        this.resultFile = clean(resultFile);
        this.logLevel = clean(logLevel);
        this.advanced = advanced == null ? Advanced.empty() : advanced;
    }

    static JMeterRunProfile empty() {
        return new JMeterRunProfile("", "", "", Advanced.empty());
    }

    String remoteHosts() {
        return remoteHosts;
    }

    String resultFile() {
        return resultFile;
    }

    String logLevel() {
        return logLevel;
    }

    Advanced advanced() {
        return advanced;
    }

    boolean isEmpty() {
        return remoteHosts.isEmpty()
                && resultFile.isEmpty()
                && logLevel.isEmpty()
                && advanced.isEmpty();
    }

    private static String clean(String value) {
        return value == null ? "" : value;
    }

    static final class Advanced {
        private final String userPropertiesFile;
        private final String jmeterProperties;
        private final String systemProperties;

        Advanced(String userPropertiesFile, String jmeterProperties, String systemProperties) {
            this.userPropertiesFile = clean(userPropertiesFile);
            this.jmeterProperties = clean(jmeterProperties);
            this.systemProperties = clean(systemProperties);
        }

        static Advanced empty() {
            return new Advanced("", "", "");
        }

        String userPropertiesFile() {
            return userPropertiesFile;
        }

        String jmeterProperties() {
            return jmeterProperties;
        }

        String systemProperties() {
            return systemProperties;
        }

        boolean isEmpty() {
            return userPropertiesFile.isEmpty()
                    && jmeterProperties.isEmpty()
                    && systemProperties.isEmpty();
        }
    }
}
