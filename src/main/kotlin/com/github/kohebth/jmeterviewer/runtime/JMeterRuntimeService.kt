package com.github.kohebth.jmeterviewer.runtime

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.PluginId
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.Locale
import java.util.stream.Collectors

@Service(Service.Level.APP)
class JMeterRuntimeService : Disposable {
    @Volatile
    private var runtime: JMeterRuntime? = null
    private var workspace: JMeterWorkspace? = null

    @Synchronized
    internal fun createWorkspace(): JMeterWorkspace {
        val installation = configuredInstallation()
        runtime?.let { active ->
            if (active.installation.home != installation.home) {
                throw JMeterConfigurationException(
                    "The JMeter home changed from ${active.installation.home} to ${installation.home}. " +
                        "Restart the IDE before loading another JMeter installation.",
                )
            }
            return workspace ?: active.createWorkspace().also { workspace = it }
        }

        val opened = JMeterRuntime.open(installation, locateBridge())
        return try {
            opened.createWorkspace().also {
                runtime = opened
                workspace = it
            }
        } catch (failure: Throwable) {
            try {
                opened.close()
            } catch (closeFailure: Throwable) {
                failure.addSuppressed(closeFailure)
            }
            throw failure
        }
    }

    internal fun requiresRestart(configuredHome: String): Boolean {
        val activeHome = runtime?.installation?.home ?: return false
        if (configuredHome.isBlank()) {
            return true
        }
        val requestedHome = try {
            Path.of(configuredHome).toAbsolutePath().normalize()
        } catch (_: InvalidPathException) {
            return false
        }
        return requestedHome != activeHome
    }

    internal fun configuredInstallation(): JMeterInstallation {
        val configuredHome = ApplicationManager.getApplication()
            .getService(JMeterSettings::class.java)
            .jmeterHome
        if (configuredHome.isBlank()) {
            throw JMeterConfigurationException(
                "Apache JMeter is not configured. Open Settings > Tools > JMeter and select " +
                    "an Apache JMeter ${JMeterInstallation.SUPPORTED_VERSION} installation.",
            )
        }
        val path = try {
            Path.of(configuredHome)
        } catch (failure: InvalidPathException) {
            throw JMeterConfigurationException(
                "The configured JMeter home is invalid: $configuredHome",
                failure,
            )
        }
        return JMeterInstallation.validate(path)
    }

    private fun locateBridge(): Path {
        val descriptor = checkNotNull(PluginManagerCore.getPlugin(PLUGIN_ID)) {
            "Unable to locate the installed JMeter Viewer plugin"
        }
        val bridgeDirectory = descriptor.pluginPath.resolve(BRIDGE_DIRECTORY)
        if (!Files.isDirectory(bridgeDirectory)) {
            throw JMeterRuntimeException(
                "Missing JMeter compatibility bridge directory: $bridgeDirectory",
            )
        }
        val bridges = Files.list(bridgeDirectory).use { paths ->
            paths
                .filter(Files::isRegularFile)
                .filter {
                    val name = it.fileName.toString().lowercase(Locale.ROOT)
                    name.startsWith("apachejmeter_core-") && name.endsWith(".jar")
                }
                .map { it.toAbsolutePath().normalize() }
                .sorted()
                .collect(Collectors.toList())
        }
        if (bridges.size != 1) {
            throw JMeterRuntimeException(
                "Expected one JMeter compatibility bridge in $bridgeDirectory, found ${bridges.size}",
            )
        }
        return bridges.single()
    }

    @Synchronized
    override fun dispose() {
        var failure: Throwable? = null
        try {
            workspace?.close()
        } catch (closeFailure: Throwable) {
            failure = closeFailure
        } finally {
            workspace = null
        }
        try {
            runtime?.close()
        } catch (closeFailure: Throwable) {
            if (failure == null) {
                failure = closeFailure
            } else {
                failure.addSuppressed(closeFailure)
            }
        }
        runtime = null
        failure?.let { throw it }
    }

    private companion object {
        const val BRIDGE_DIRECTORY = "jmeter-bridge"
        val PLUGIN_ID: PluginId = PluginId.getId("com.github.kohebth.jmeterviewer")
    }
}
