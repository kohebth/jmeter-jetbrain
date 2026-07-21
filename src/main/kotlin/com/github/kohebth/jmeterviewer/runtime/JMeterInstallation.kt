package com.github.kohebth.jmeterviewer.runtime

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.stream.Collectors

internal data class JMeterInstallation private constructor(
    val home: Path,
    val version: String,
    val launcherJar: Path,
    val coreJar: Path,
    val runtimeJars: List<Path>,
) {
    val propertiesFile: Path = home.resolve("bin/jmeter.properties")
    val saveServicePropertiesFile: Path = home.resolve("bin/saveservice.properties")

    companion object {
        const val SUPPORTED_VERSION: String = "5.6.3"
        private val SUPPORTED_CORE_JAR_NAMES = setOf(
            "ApacheJMeter_core.jar",
            "ApacheJMeter_core-$SUPPORTED_VERSION.jar",
        )

        fun validate(candidate: Path): JMeterInstallation {
            val home = candidate.toAbsolutePath().normalize()
            if (!Files.isDirectory(home)) {
                throw JMeterConfigurationException(
                    "The configured JMeter home is not a directory: $home",
                )
            }

            val launcher = home.resolve("bin/ApacheJMeter.jar")
            val properties = home.resolve("bin/jmeter.properties")
            val saveServiceProperties = home.resolve("bin/saveservice.properties")
            val libraryDirectory = home.resolve("lib")
            val extensionDirectory = libraryDirectory.resolve("ext")

            val missing = buildList {
                if (!Files.isRegularFile(launcher)) add("bin/ApacheJMeter.jar")
                if (!Files.isRegularFile(properties)) add("bin/jmeter.properties")
                if (!Files.isRegularFile(saveServiceProperties)) add("bin/saveservice.properties")
                if (!Files.isDirectory(libraryDirectory)) add("lib")
                if (!Files.isDirectory(extensionDirectory)) add("lib/ext")
            }
            if (missing.isNotEmpty()) {
                throw JMeterConfigurationException(
                    "The selected directory is not a complete JMeter installation. " +
                        "Missing: ${missing.joinToString()} (home: $home)",
                )
            }

            val detectedVersion = readImplementationVersion(launcher)
            if (detectedVersion != SUPPORTED_VERSION) {
                throw JMeterConfigurationException(
                    "Unsupported Apache JMeter version $detectedVersion at $home. " +
                        "This plugin requires Apache JMeter $SUPPORTED_VERSION.",
                )
            }

            val extensionJars = jarFiles(extensionDirectory)
            val core = extensionJars.firstOrNull { path ->
                val name = path.fileName.toString()
                SUPPORTED_CORE_JAR_NAMES.any { expected ->
                    name.equals(expected, ignoreCase = true)
                }
            }
            if (core == null) {
                val detectedCores = extensionJars
                    .map { it.fileName.toString() }
                    .filter { it.startsWith("ApacheJMeter_core", ignoreCase = true) }
                val detail = if (detectedCores.isEmpty()) {
                    "No ApacheJMeter_core jar was found"
                } else {
                    "Found ${detectedCores.joinToString()}"
                }
                throw JMeterConfigurationException(
                    "Missing lib/ext/ApacheJMeter_core.jar in $home. $detail.",
                )
            }

            val normalizedLauncher = launcher.toAbsolutePath().normalize()
            val runtimeJars = buildList {
                add(normalizedLauncher)
                addAll(jarFiles(libraryDirectory))
                addAll(jarFiles(extensionDirectory))
                addAll(jarFiles(libraryDirectory.resolve("junit")))
            }

            return JMeterInstallation(
                home = home,
                version = detectedVersion,
                launcherJar = normalizedLauncher,
                coreJar = core.toAbsolutePath().normalize(),
                runtimeJars = runtimeJars,
            )
        }

        private fun readImplementationVersion(launcher: Path): String {
            val version = try {
                JarFile(launcher.toFile()).use { jar ->
                    jar.manifest?.mainAttributes?.getValue(Attributes.Name.IMPLEMENTATION_VERSION)
                }
            } catch (failure: IOException) {
                throw JMeterConfigurationException(
                    "Unable to read JMeter version from $launcher: ${failure.message}",
                    failure,
                )
            }
            return version?.trim()?.takeIf(String::isNotEmpty)
                ?: throw JMeterConfigurationException(
                    "Unable to detect the JMeter version from $launcher.",
                )
        }

        private fun jarFiles(directory: Path): List<Path> {
            if (!Files.isDirectory(directory)) {
                return emptyList()
            }
            return Files.list(directory).use { paths ->
                paths
                    .filter(Files::isRegularFile)
                    .filter { it.fileName.toString().lowercase(Locale.ROOT).endsWith(".jar") }
                    .map { it.toAbsolutePath().normalize() }
                    .sorted(compareBy { it.fileName.toString().lowercase(Locale.ROOT) })
                    .collect(Collectors.toList())
            }
        }
    }
}

internal class JMeterConfigurationException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)
