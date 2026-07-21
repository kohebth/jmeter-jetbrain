package com.github.kohebth.jmeterviewer.runtime

import org.apache.jmeter.save.SaveService
import org.apache.jmeter.util.JMeterUtils
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import java.util.concurrent.atomic.AtomicBoolean

internal object JMeterRuntime {
    private val ready = AtomicBoolean(false)

    @Synchronized
    fun initialize(
        home: Path,
        pluginLibraryDirectory: Path? = null,
        pluginClassLoader: ClassLoader = JMeterRuntime::class.java.classLoader,
    ) {
        if (ready.get()) {
            return
        }

        val normalizedHome = home.toAbsolutePath().normalize()
        val properties = normalizedHome.resolve("bin/jmeter.properties")
        val saveServiceProperties = normalizedHome.resolve("bin/saveservice.properties")
        require(Files.isRegularFile(properties)) {
            "Missing bundled JMeter properties: " + properties
        }
        require(Files.isRegularFile(saveServiceProperties)) {
            "Missing bundled JMeter save-service properties: " + saveServiceProperties
        }

        JMeterUtils.setJMeterHome(normalizedHome.toString())
        JMeterUtils.loadJMeterProperties(properties.toString())
        JMeterUtils.setProperty("saveservice_properties", "saveservice.properties")

        if (pluginLibraryDirectory != null) {
            val moduleSearchPath = bundledModuleSearchPath(pluginLibraryDirectory)
            require(moduleSearchPath.isNotEmpty()) {
                "No bundled ApacheJMeter modules found in $pluginLibraryDirectory"
            }
            JMeterUtils.setProperty("search_paths", moduleSearchPath)
        }

        withContextClassLoader(pluginClassLoader) {
            JMeterUtils.initLocale()
            SaveService.loadProperties()
        }

        ready.set(true)
    }

    internal fun bundledModuleSearchPath(pluginLibraryDirectory: Path): String {
        if (!Files.isDirectory(pluginLibraryDirectory)) {
            return ""
        }

        return Files.list(pluginLibraryDirectory).use { paths ->
            paths
                .filter(Files::isRegularFile)
                .filter { path ->
                    val name = path.fileName.toString()
                    name.startsWith("ApacheJMeter_") && name.endsWith(".jar")
                }
                .map { it.toAbsolutePath().normalize().toString() }
                .sorted()
                .collect(Collectors.joining(";"))
        }
    }

    internal inline fun <T> withContextClassLoader(
        classLoader: ClassLoader,
        action: () -> T,
    ): T {
        val thread = Thread.currentThread()
        val previous = thread.contextClassLoader
        thread.contextClassLoader = classLoader
        return try {
            action()
        } finally {
            thread.contextClassLoader = previous
        }
    }
}
