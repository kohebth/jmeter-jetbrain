package com.github.kohebth.jmeterviewer.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class JMeterJavaEnvironmentTest {
    @Test
    fun fallsBackToTheIdeRuntimeWhenJavaHomeIsMissing(@TempDir directory: Path) {
        val runtime = javaHome(directory.resolve("ide-runtime"), isWindows = false)

        val environment = JMeterJavaEnvironment.resolve(
            isWindows = false,
            parentEnvironment = mapOf("PATH" to "/usr/local/bin"),
            runtimeJavaHome = runtime.toString(),
        )

        assertEquals(runtime.toString(), environment["JAVA_HOME"])
        assertEquals(
            "${runtime.resolve("bin")}${File.pathSeparator}/usr/local/bin",
            environment["PATH"],
        )
        assertFalse(environment.containsKey("JM_LAUNCH"))
    }

    @Test
    fun keepsAValidInheritedJavaHome(@TempDir directory: Path) {
        val inherited = javaHome(directory.resolve("inherited"), isWindows = false)
        val runtime = javaHome(directory.resolve("ide-runtime"), isWindows = false)

        val environment = JMeterJavaEnvironment.resolve(
            isWindows = false,
            parentEnvironment = mapOf("JAVA_HOME" to inherited.toString()),
            runtimeJavaHome = runtime.toString(),
        )

        assertEquals(inherited.toString(), environment["JAVA_HOME"])
    }

    @Test
    fun suppliesTheWindowsLauncherAndPreservesThePathKey(@TempDir directory: Path) {
        val runtime = javaHome(directory.resolve("ide-runtime"), isWindows = true)

        val environment = JMeterJavaEnvironment.resolve(
            isWindows = true,
            parentEnvironment = mapOf("Path" to "C:\\Windows\\System32"),
            runtimeJavaHome = runtime.toString(),
        )

        assertEquals(runtime.toString(), environment["JAVA_HOME"])
        assertEquals(
            "${runtime.resolve("bin")};C:\\Windows\\System32",
            environment["Path"],
        )
        assertEquals(runtime.resolve("bin/java.exe").toString(), environment["JM_LAUNCH"])
    }

    @Test
    fun ignoresAnInvalidInheritedJavaHome(@TempDir directory: Path) {
        val runtime = javaHome(directory.resolve("ide-runtime"), isWindows = false)

        val environment = JMeterJavaEnvironment.resolve(
            isWindows = false,
            parentEnvironment = mapOf("JAVA_HOME" to directory.resolve("missing").toString()),
            runtimeJavaHome = runtime.toString(),
        )

        assertEquals(runtime.toString(), environment["JAVA_HOME"])
    }

    private fun javaHome(home: Path, isWindows: Boolean): Path {
        val executable = Files.createDirectories(home.resolve("bin"))
            .resolve(if (isWindows) "java.exe" else "java")
        Files.writeString(executable, "test")
        return home.toAbsolutePath().normalize()
    }
}
