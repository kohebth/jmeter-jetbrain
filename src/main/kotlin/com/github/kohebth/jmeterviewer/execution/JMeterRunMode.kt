package com.github.kohebth.jmeterviewer.execution

internal enum class JMeterRunMode(
    val actionCommand: String,
    private val displayName: String,
) {
    AS_IS("run_tg", "Start"),
    IGNORE_TIMERS("run_tg_no_timers", "Start no pauses"),
    VALIDATE("validate_tg", "Validate"),
    ;

    override fun toString(): String = displayName

    companion object {
        fun fromActionCommand(actionCommand: String): JMeterRunMode? =
            values().firstOrNull { it.actionCommand == actionCommand }
    }
}
