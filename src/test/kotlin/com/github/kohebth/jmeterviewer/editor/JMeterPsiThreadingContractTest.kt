package com.github.kohebth.jmeterviewer.editor

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class JMeterPsiThreadingContractTest {
    @Test
    fun createsAndReadsTemporaryPsiUnderExplicitReadActions() {
        val source = Files.readString(
            Path.of(
                "src/main/kotlin/com/github/kohebth/jmeterviewer/editor/" +
                    "JMeterTextAreaAdapters.kt",
            ),
        )

        assertTrue(source.contains("ReadAction.compute<com.intellij.psi.PsiFile"))
        assertTrue(source.contains("ReadAction.compute<String"))
        assertTrue(source.contains("ReadAction.compute<com.intellij.openapi.editor.Document?"))
    }
}
