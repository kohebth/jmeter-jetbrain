package com.github.kohebth.jmeterviewer.ide

import com.intellij.icons.AllIcons
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile
import java.nio.charset.StandardCharsets
import javax.swing.Icon

object JMeterFileType : LanguageFileType(XMLLanguage.INSTANCE) {
    override fun getName(): String = "JMeter Test Plan"

    override fun getDescription(): String = "Apache JMeter test plan"

    override fun getDefaultExtension(): String = "jmx"

    override fun getIcon(): Icon = AllIcons.FileTypes.Xml

    override fun getCharset(file: VirtualFile, content: ByteArray): String =
        StandardCharsets.UTF_8.name()
}
