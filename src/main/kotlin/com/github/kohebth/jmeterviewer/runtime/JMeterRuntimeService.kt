package com.github.kohebth.jmeterviewer.runtime

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.PluginId

@Service(Service.Level.APP)
class JMeterRuntimeService {
    @Volatile
    private var initialized = false

    @Synchronized
    fun ensureInitialized() {
        if (initialized) {
            return
        }

        val descriptor = checkNotNull(PluginManagerCore.getPlugin(PLUGIN_ID)) {
            "Unable to locate the installed JMeter Viewer plugin"
        }
        JMeterRuntime.initialize(
            home = descriptor.pluginPath.resolve("jmeter-home"),
            pluginLibraryDirectory = descriptor.pluginPath.resolve("lib"),
            pluginClassLoader = javaClass.classLoader,
        )
        initialized = true
    }

    private companion object {
        val PLUGIN_ID: PluginId = PluginId.getId("com.github.kohebth.jmeterviewer")
    }
}
