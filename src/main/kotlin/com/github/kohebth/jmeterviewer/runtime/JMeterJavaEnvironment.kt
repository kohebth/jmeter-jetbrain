package com.github.kohebth.jmeterviewer.runtime

import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path

internal object JMeterJavaEnvironment {
    fun resolve(
        isWindows: Boolean,
        parentEnvironment: Map<String, String> = System.getenv(),
        runtimeJavaHome: String? = System.getProperty("java.home"),
    ): Map<String, String> {
        val javaHome = sequenceOf(
            parentEnvironment.valueOf("JAVA_HOME"),
            parentEnvironment.valueOf("JRE_HOME"),
            runtimeJavaHome,
        )
            .mapNotNull(::normalizedPath)
            .distinct()
            .firstOrNull { Files.isRegularFile(javaExecutable(it, isWindows)) }
            ?: throw JMeterConfigurationException(
                "Unable to locate Java for JMeter. Set JAVA_HOME to a Java installation " +
                    "that contains bin/${javaExecutableName(isWindows)}.",
            )

        val javaBin = javaHome.resolve("bin").toString()
        val inheritedPath = parentEnvironment.entries.firstOrNull { (name) ->
            name.equals("PATH", ignoreCase = true)
        }
        val pathSeparator = if (isWindows) ";" else java.io.File.pathSeparator
        val path = listOfNotNull(
            javaBin,
            inheritedPath?.value?.takeIf(String::isNotBlank),
        ).joinToString(pathSeparator)

        return buildMap {
            put("JAVA_HOME", javaHome.toString())
            put(inheritedPath?.key ?: "PATH", path)
            if (isWindows) {
                put("JM_LAUNCH", javaExecutable(javaHome, true).toString())
            }
        }
    }

    private fun Map<String, String>.valueOf(name: String): String? =
        entries.firstOrNull { (key) -> key.equals(name, ignoreCase = true) }?.value

    private fun normalizedPath(value: String?): Path? {
        val candidate = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return try {
            Path.of(candidate).toAbsolutePath().normalize()
        } catch (_: InvalidPathException) {
            null
        }
    }

    private fun javaExecutable(home: Path, isWindows: Boolean): Path =
        home.resolve("bin").resolve(javaExecutableName(isWindows))

    private fun javaExecutableName(isWindows: Boolean): String =
        if (isWindows) "java.exe" else "java"
}
