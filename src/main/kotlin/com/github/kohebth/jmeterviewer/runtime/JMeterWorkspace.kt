package com.github.kohebth.jmeterviewer.runtime

import java.awt.Component
import java.io.InputStream
import java.nio.file.Path
import javax.swing.JComponent

internal interface JMeterWorkspace : AutoCloseable {
    val component: JComponent

    fun setDialogParent(parent: Component?)

    fun load(input: InputStream, sourcePath: Path)

    fun snapshot(): ByteArray

    val isDirty: Boolean

    fun markSaved()
}

internal class ReflectiveJMeterWorkspace(
    private val runtime: JMeterRuntime,
    workspaceClass: Class<*>,
    private val delegate: Any,
) : JMeterWorkspace {
    private var closed = false
    private val getComponent = workspaceClass.getMethod("getComponent")
    private val setDialogParent = workspaceClass.getMethod("setDialogParent", Component::class.java)
    private val load = workspaceClass.getMethod("load", InputStream::class.java, Path::class.java)
    private val snapshot = workspaceClass.getMethod("snapshot")
    private val getDirty = workspaceClass.getMethod("isDirty")
    private val markSaved = workspaceClass.getMethod("markSaved")
    private val close = workspaceClass.getMethod("close")

    override val component: JComponent
        get() = call(getComponent) as? JComponent
            ?: throw JMeterRuntimeException("JMeter returned an incompatible editor component")

    override fun setDialogParent(parent: Component?) {
        call(setDialogParent, parent)
    }

    override fun load(input: InputStream, sourcePath: Path) {
        call(load, input, sourcePath)
    }

    override fun snapshot(): ByteArray = call(snapshot) as? ByteArray
        ?: throw JMeterRuntimeException("JMeter returned an incompatible JMX snapshot")

    override val isDirty: Boolean
        get() = call(getDirty) as? Boolean
            ?: throw JMeterRuntimeException("JMeter returned an incompatible dirty state")

    override fun markSaved() {
        call(markSaved)
    }

    @Synchronized
    override fun close() {
        if (closed) {
            return
        }
        closed = true
        runtime.withContextClassLoader {
            runtime.invoke(close, delegate)
        }
    }

    private fun call(method: java.lang.reflect.Method, vararg arguments: Any?): Any? {
        check(!closed) { "The JMeter workspace is closed" }
        return runtime.withContextClassLoader {
            runtime.invoke(method, delegate, *arguments)
        }
    }
}
