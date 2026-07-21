package com.github.kohebth.jmeterviewer.runtime

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(
    name = "JMeterViewerSettings",
    storages = [Storage("jmeter-viewer.xml")],
)
class JMeterSettings : PersistentStateComponent<JMeterSettings.SettingsState> {
    private var settingsState = SettingsState()

    var jmeterHome: String
        get() = settingsState.jmeterHome
        set(value) {
            settingsState.jmeterHome = value.trim()
        }

    override fun getState(): SettingsState = settingsState

    override fun loadState(state: SettingsState) {
        settingsState = state.copy(jmeterHome = state.jmeterHome.trim())
    }

    data class SettingsState(
        var jmeterHome: String = "",
    )
}
