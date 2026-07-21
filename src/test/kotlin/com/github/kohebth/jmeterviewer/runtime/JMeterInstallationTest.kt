package com.github.kohebth.jmeterviewer.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

class JMeterInstallationTest {
    @Test
    fun acceptsAnOfficialJMeter563Layout(@TempDir home: Path) {
        val files = createInstallation(home, JMeterInstallation.SUPPORTED_VERSION)
        val library = emptyJar(home.resolve("lib/commons-lang3.jar"))
        val extension = emptyJar(home.resolve("lib/ext/custom-plugin.jar"))
        val junit = emptyJar(home.resolve("lib/junit/junit.jar"))

        val installation = JMeterInstallation.validate(home)

        assertEquals(home.toAbsolutePath().normalize(), installation.home)
        assertEquals(JMeterInstallation.SUPPORTED_VERSION, installation.version)
        assertEquals(files.launcher, installation.launcherJar)
        assertEquals(files.core, installation.coreJar)
        assertEquals(
            listOf(files.launcher, library, files.core, extension, junit),
            installation.runtimeJars,
        )
    }

    @Test
    fun acceptsAVersionedCoreJar(@TempDir home: Path) {
        val coreName = "ApacheJMeter_core-${JMeterInstallation.SUPPORTED_VERSION}.jar"
        val files = createInstallation(
            home,
            JMeterInstallation.SUPPORTED_VERSION,
            coreName,
        )

        val installation = JMeterInstallation.validate(home)

        assertEquals(files.core, installation.coreJar)
    }

    @Test
    fun rejectsAnUnsupportedJMeterVersion(@TempDir home: Path) {
        createInstallation(home, "5.6.2")

        val failure = assertThrows<JMeterConfigurationException> {
            JMeterInstallation.validate(home)
        }

        assertTrue(failure.message.orEmpty().contains("5.6.2"))
        assertTrue(failure.message.orEmpty().contains(JMeterInstallation.SUPPORTED_VERSION))
    }

    @Test
    fun reportsEveryRequiredPathThatIsMissing(@TempDir home: Path) {
        val failure = assertThrows<JMeterConfigurationException> {
            JMeterInstallation.validate(home)
        }

        val message = failure.message.orEmpty()
        assertTrue(message.contains("bin/ApacheJMeter.jar"))
        assertTrue(message.contains("bin/jmeter.properties"))
        assertTrue(message.contains("bin/saveservice.properties"))
        assertTrue(message.contains("lib"))
        assertTrue(message.contains("lib/ext"))
    }

    @Test
    fun rejectsAFileInsteadOfAJMeterHome(@TempDir directory: Path) {
        val file = Files.createFile(directory.resolve("apache-jmeter"))

        val failure = assertThrows<JMeterConfigurationException> {
            JMeterInstallation.validate(file)
        }

        assertTrue(failure.message.orEmpty().contains("not a directory"))
    }

    private fun createInstallation(
        home: Path,
        version: String,
        coreName: String = "ApacheJMeter_core.jar",
    ): InstallationFiles {
        val bin = Files.createDirectories(home.resolve("bin"))
        Files.createDirectories(home.resolve("lib/ext"))
        Files.createDirectories(home.resolve("lib/junit"))
        Files.writeString(bin.resolve("jmeter.properties"), "# test\n")
        Files.writeString(bin.resolve("saveservice.properties"), "_version=5.0\n")
        val launcher = versionedJar(bin.resolve("ApacheJMeter.jar"), version)
        val core = emptyJar(home.resolve("lib/ext/$coreName"))
        return InstallationFiles(launcher, core)
    }

    private fun versionedJar(path: Path, version: String): Path {
        val manifest = Manifest().apply {
            mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
            mainAttributes[Attributes.Name.IMPLEMENTATION_VERSION] = version
        }
        Files.newOutputStream(path).use { output ->
            JarOutputStream(output, manifest).use { }
        }
        return path.toAbsolutePath().normalize()
    }

    private fun emptyJar(path: Path): Path {
        Files.newOutputStream(path).use { output ->
            JarOutputStream(output).use { }
        }
        return path.toAbsolutePath().normalize()
    }

    private data class InstallationFiles(
        val launcher: Path,
        val core: Path,
    )
}
