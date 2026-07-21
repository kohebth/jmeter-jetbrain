package com.github.kohebth.jmeterviewer.ide

import com.github.kohebth.jmeterviewer.runtime.JMeterConfigurationException
import com.github.kohebth.jmeterviewer.runtime.JMeterInstallation
import com.github.kohebth.jmeterviewer.runtime.JMeterRuntimeService
import com.github.kohebth.jmeterviewer.runtime.JMeterSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import java.nio.file.InvalidPathException
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.JPanel

class JMeterSettingsConfigurable : SearchableConfigurable {
    private var homeField: TextFieldWithBrowseButton? = null
    private var panel: JPanel? = null

    override fun getId(): String = ID

    override fun getDisplayName(): String = "JMeter"

    override fun createComponent(): JComponent {
        panel?.let { return it }
        val field = TextFieldWithBrowseButton().apply {
            addBrowseFolderListener(
                "Select Apache JMeter Home",
                "Select the root of an Apache JMeter ${JMeterInstallation.SUPPORTED_VERSION} installation.",
                null,
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
            )
        }
        homeField = field
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("JMeter home:", field, 1, false)
            .addComponent(
                JBLabel(
                    "<html>Requires the official Apache JMeter " +
                        "${JMeterInstallation.SUPPORTED_VERSION} binary distribution.</html>",
                ),
            )
            .addComponentFillVertically(JPanel(), 0)
            .panel
            .also { panel = it }
    }

    override fun isModified(): Boolean =
        homeField?.text?.trim().orEmpty() != settings().jmeterHome

    @Throws(ConfigurationException::class)
    override fun apply() {
        val configuredHome = homeField?.text?.trim().orEmpty()
        if (configuredHome.isNotEmpty()) {
            try {
                JMeterInstallation.validate(Path.of(configuredHome))
            } catch (failure: InvalidPathException) {
                throw ConfigurationException("Invalid JMeter home: ${failure.input}")
            } catch (failure: JMeterConfigurationException) {
                throw ConfigurationException(failure.message.orEmpty())
            }
        }

        val restartRequired = ApplicationManager.getApplication()
            .getService(JMeterRuntimeService::class.java)
            .requiresRestart(configuredHome)
        settings().jmeterHome = configuredHome
        if (restartRequired) {
            Messages.showInfoMessage(
                "Restart the IDE before opening another JMeter test plan so the new " +
                    "JMeter installation can be loaded safely.",
                "JMeter Restart Required",
            )
        }
    }

    override fun reset() {
        homeField?.text = settings().jmeterHome
    }

    override fun disposeUIResources() {
        homeField = null
        panel = null
    }

    private fun settings(): JMeterSettings =
        ApplicationManager.getApplication().getService(JMeterSettings::class.java)

    companion object {
        const val ID: String = "jmeter.viewer.settings"
    }
}
