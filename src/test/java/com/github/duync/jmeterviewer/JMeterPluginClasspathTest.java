package com.github.duync.jmeterviewer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JMeterPluginClasspathTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void clearClasspath() {
        JMeterPluginClasspath.clear();
    }

    @Test
    void addsAndRemovesWholeJMeterHome() throws Exception {
        Path home = tempDir.resolve("apache-jmeter");
        Path bin = Files.createDirectories(home.resolve("bin"));
        Path lib = Files.createDirectories(home.resolve("lib"));
        Path ext = Files.createDirectories(lib.resolve("ext"));
        Files.createFile(bin.resolve("jmeter.properties"));
        Files.createFile(lib.resolve("ApacheJMeter_core.jar"));
        Files.createFile(ext.resolve("plugin.jar"));

        JMeterPluginClasspath.addPath(home.toFile());

        java.util.Set<String> paths = paths();
        assertTrue(paths.contains(home.toFile().getCanonicalPath()));
        assertTrue(paths.contains(bin.toFile().getCanonicalPath()));
        assertTrue(paths.contains(lib.resolve("ApacheJMeter_core.jar").toFile().getCanonicalPath()));
        assertTrue(paths.contains(ext.resolve("plugin.jar").toFile().getCanonicalPath()));

        JMeterPluginClasspath.remove(home.toFile());

        String homePath = home.toFile().getCanonicalPath();
        assertFalse(paths().stream().anyMatch(path -> path.startsWith(homePath)));
    }

    private java.util.Set<String> paths() {
        return JMeterPluginClasspath.paths().stream()
                .map(this::canonicalPath)
                .collect(Collectors.toSet());
    }

    private String canonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (Exception exception) {
            return file.getAbsolutePath();
        }
    }
}
