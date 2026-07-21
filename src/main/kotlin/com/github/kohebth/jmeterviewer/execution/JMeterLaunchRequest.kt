package com.github.kohebth.jmeterviewer.execution

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

internal data class JMeterLaunchRequest(
    val commandLine: GeneralCommandLine,
    val ownerFile: VirtualFile,
    val sessionId: String,
    val restrictedPlan: Path,
    val logFile: Path,
    val bridge: JMeterResultBridge,
)
