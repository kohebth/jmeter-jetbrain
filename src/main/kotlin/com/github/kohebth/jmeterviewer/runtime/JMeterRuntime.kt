package com.github.kohebth.jmeterviewer.runtime

import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.Enumeration
import javax.swing.plaf.ComponentUI

internal class JMeterRuntime private constructor(
    val installation: JMeterInstallation,
    private val bridgeJar: Path,
    internal val classLoader: JMeterRuntimeClassLoader,
    internal val contextClassLoader: JMeterContextClassLoader,
) : AutoCloseable {
    private var closed = false

    fun createWorkspace(): JMeterWorkspace = withContextClassLoader {
        ensureOpen()
        val workspaceClass = classLoader.loadClass(EMBEDDED_WORKSPACE_CLASS)
        verifyBridgeClass(workspaceClass)
        val delegate = invoke(workspaceClass.getMethod("create"), null)
            ?: throw JMeterRuntimeException("JMeter returned no embedded workspace")
        ReflectiveJMeterWorkspace(this, workspaceClass, delegate)
    }

    internal fun <T> withContextClassLoader(action: () -> T): T {
        ensureOpen()
        val thread = Thread.currentThread()
        val previous = thread.contextClassLoader
        thread.contextClassLoader = contextClassLoader
        return try {
            action()
        } finally {
            thread.contextClassLoader = previous
        }
    }

    internal fun invoke(method: Method, receiver: Any?, vararg arguments: Any?): Any? =
        try {
            method.invoke(receiver, *arguments)
        } catch (failure: InvocationTargetException) {
            throw failure.targetException
        } catch (failure: ReflectiveOperationException) {
            throw JMeterRuntimeException(
                "Unable to call ${method.declaringClass.name}.${method.name}",
                failure,
            )
        }

    private fun initialize() = withContextClassLoader {
        val utils = classLoader.loadClass(JMETER_UTILS_CLASS)
        val detectedVersion = invoke(utils.getMethod("getJMeterVersion"), null) as? String
        if (detectedVersion != JMeterInstallation.SUPPORTED_VERSION) {
            throw JMeterConfigurationException(
                "The JMeter compatibility bridge reports version $detectedVersion; " +
                    "expected ${JMeterInstallation.SUPPORTED_VERSION}.",
            )
        }

        invoke(
            utils.getMethod("setJMeterHome", String::class.java),
            null,
            installation.home.toString(),
        )
        invoke(
            utils.getMethod("loadJMeterProperties", String::class.java),
            null,
            installation.propertiesFile.toString(),
        )
        invoke(
            utils.getMethod("setProperty", String::class.java, String::class.java),
            null,
            "saveservice_properties",
            installation.saveServicePropertiesFile.fileName.toString(),
        )
        invoke(
            utils.getMethod("setProperty", String::class.java, String::class.java),
            null,
            "undo.history.size",
            "0",
        )
        invoke(utils.getMethod("initLocale"), null)

        val saveService = classLoader.loadClass(SAVE_SERVICE_CLASS)
        invoke(saveService.getMethod("loadProperties"), null)

        val jmeterPlugin = classLoader.loadClass(JMETER_PLUGIN_CLASS)
        val jmeter = instantiate(classLoader.loadClass(JMETER_CLASS))
        val pluginManager = classLoader.loadClass(PLUGIN_MANAGER_CLASS)
        invoke(
            pluginManager.getMethod("install", jmeterPlugin, Boolean::class.javaPrimitiveType),
            null,
            jmeter,
            true,
        )
    }

    private fun instantiate(type: Class<*>): Any = try {
        type.getDeclaredConstructor().newInstance()
    } catch (failure: InvocationTargetException) {
        throw failure.targetException
    } catch (failure: ReflectiveOperationException) {
        throw JMeterRuntimeException("Unable to create ${type.name}", failure)
    }

    private fun verifyBridgeClass(type: Class<*>) {
        val source = try {
            type.protectionDomain.codeSource?.location?.toURI()?.let(Path::of)
        } catch (failure: Exception) {
            throw JMeterRuntimeException(
                "Unable to identify the JMeter compatibility bridge source",
                failure,
            )
        }?.toAbsolutePath()?.normalize()
        if (source != bridgeJar) {
            throw JMeterRuntimeException(
                "JMeter embedded classes were loaded from $source instead of $bridgeJar",
            )
        }
    }

    private fun ensureOpen() {
        check(!closed) { "The JMeter runtime is closed" }
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        try {
            classLoader.close()
        } catch (failure: IOException) {
            throw JMeterRuntimeException("Unable to close the JMeter runtime classloader", failure)
        }
    }

    companion object {
        private const val JMETER_UTILS_CLASS = "org.apache.jmeter.util.JMeterUtils"
        private const val SAVE_SERVICE_CLASS = "org.apache.jmeter.save.SaveService"
        private const val JMETER_CLASS = "org.apache.jmeter.JMeter"
        private const val JMETER_PLUGIN_CLASS = "org.apache.jmeter.plugin.JMeterPlugin"
        private const val PLUGIN_MANAGER_CLASS = "org.apache.jmeter.plugin.PluginManager"
        private const val EMBEDDED_WORKSPACE_CLASS = "org.apache.jmeter.gui.EmbeddedJMeterWorkspace"

        fun open(
            installation: JMeterInstallation,
            bridgeJar: Path,
            hostClassLoader: ClassLoader = JMeterRuntime::class.java.classLoader,
        ): JMeterRuntime {
            val normalizedBridge = bridgeJar.toAbsolutePath().normalize()
            if (!Files.isRegularFile(normalizedBridge)) {
                throw JMeterRuntimeException(
                    "Missing JMeter compatibility bridge: $normalizedBridge",
                )
            }
            val classpath = buildList {
                add(normalizedBridge)
                addAll(installation.runtimeJars)
            }.distinct()
            val loader = JMeterRuntimeClassLoader(
                urls = classpath.map { it.toUri().toURL() }.toTypedArray(),
                hostClassLoader = hostClassLoader,
            )
            val contextLoader = JMeterContextClassLoader(
                runtimeClassLoader = loader,
                hostClassLoader = hostClassLoader,
            )
            val runtime = JMeterRuntime(
                installation,
                normalizedBridge,
                loader,
                contextLoader,
            )
            return try {
                runtime.initialize()
                runtime
            } catch (failure: Throwable) {
                try {
                    runtime.close()
                } catch (closeFailure: Throwable) {
                    failure.addSuppressed(closeFailure)
                }
                throw failure
            }
        }
    }
}

/**
 * Loads JMeter from its isolated classpath. Swing asks a component's defining loader for its
 * look-and-feel delegate, so the only host classes exposed here are ComponentUI implementations.
 */
internal class JMeterRuntimeClassLoader(
    urls: Array<URL>,
    private val hostClassLoader: ClassLoader,
) : URLClassLoader(urls, ClassLoader.getPlatformClassLoader()) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> =
        synchronized(getClassLoadingLock(name)) {
            try {
                super.loadClass(name, resolve)
            } catch (runtimeFailure: ClassNotFoundException) {
                val hostClass = try {
                    hostClassLoader.loadClass(name)
                } catch (_: ClassNotFoundException) {
                    throw runtimeFailure
                }
                if (!ComponentUI::class.java.isAssignableFrom(hostClass)) {
                    throw runtimeFailure
                }
                hostClass
            }
        }
}

/**
 * Supplies host classes needed by APIs that use the thread-context loader. Resources remain
 * runtime-first and are never merged, which prevents JMeter service loading from accidentally
 * discovering providers bundled by the host IDE.
 */
internal class JMeterContextClassLoader(
    private val runtimeClassLoader: ClassLoader,
    private val hostClassLoader: ClassLoader,
) : ClassLoader(ClassLoader.getPlatformClassLoader()) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> =
        synchronized(getClassLoadingLock(name)) {
            try {
                runtimeClassLoader.loadClass(name)
            } catch (_: ClassNotFoundException) {
                hostClassLoader.loadClass(name)
            }
        }

    override fun getResource(name: String): URL? =
        runtimeClassLoader.getResource(name) ?: hostClassLoader.getResource(name)

    override fun getResources(name: String): Enumeration<URL> {
        val runtimeResources = runtimeClassLoader.getResources(name).toList()
        return if (runtimeResources.isNotEmpty()) {
            Collections.enumeration(runtimeResources)
        } else {
            hostClassLoader.getResources(name)
        }
    }
}

internal class JMeterRuntimeException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
