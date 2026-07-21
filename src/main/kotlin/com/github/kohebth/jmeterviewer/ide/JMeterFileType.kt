package com.github.kohebth.jmeterviewer.ide

import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile
import java.nio.charset.StandardCharsets
import javax.swing.Icon
import javax.swing.ImageIcon

object JMeterFileType : LanguageFileType(XMLLanguage.INSTANCE) {
    private val jmeterIcon = ImageIcon(
        checkNotNull(JMeterFileType::class.java.getResource("/icons/jmeter-feather.gif")) {
            "Missing bundled JMeter feather icon"
        },
    )

    override fun getName(): String = "JMeter Test Plan"

    override fun getDescription(): String = "Apache JMeter test plan"

    override fun getDefaultExtension(): String = "jmx"

    override fun getIcon(): Icon = jmeterIcon

    override fun getCharset(file: VirtualFile, content: ByteArray): String =
        StandardCharsets.UTF_8.name()
}
