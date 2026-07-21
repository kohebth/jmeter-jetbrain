# JMeter Viewer for JetBrains IDEs

JetBrains plugin that opens local `.jmx` files with Apache JMeter's native
editable tree, element forms, structural actions, and `Copy Code` support.
The ordinary XML editor remains available beside the visual editor.

The plugin uses an Apache JMeter 5.6.3 installation selected by the user. This
keeps the plugin small and lets JMeter discover compatible components installed
in that installation's `lib/ext` directory.

## Configure JMeter

1. Install the official Apache JMeter 5.6.3 binary distribution.
2. In the IDE, open **Settings/Preferences > Tools > JMeter**.
3. Select the JMeter root directory. It must contain
   `bin/ApacheJMeter.jar`, `bin/jmeter.properties`, and `lib/ext`.
4. Open a local `.jmx` file and select its **JMeter** editor tab.

An invalid or unsupported directory is rejected in Settings. If no installation
is configured, the editor offers a **Configure JMeter** action. Changing or
clearing the configured home after JMeter's native editor has loaded requires an
IDE restart because JMeter keeps process-global GUI state.

## Editor ownership

JMeter supplies its standard tree, forms, context menus, validation, and JMX
serialization. JetBrains supplies editor tabs, file navigation, Save/Save All,
external-change handling, and the application look and feel. JMeter's standalone
toolbar, menu bar, logger, runner controls, and file actions are not exposed.

JMeter's GUI state is process-global, so the plugin deliberately owns one shared
native workspace. Before another visual JMX tab becomes active, the current
model is flushed into the IDE document and saved. A save failure cancels the
switch. If XML and visual state both changed, the editor offers `Reload
external`, `Overwrite visual`, or `Cancel`.

The editor supports local physical JMX files and loads the standard modules and
compatible third-party plugins from the selected JMeter 5.6.3 installation.
Execution/results, recorder, templates, and report generation are intentionally
not exposed in the JetBrains UI.

## Runtime isolation

JMeter and its dependencies are loaded from the selected installation in an
isolated class loader. The distributable contains only the JetBrains plugin jar
and a small patched JMeter core compatibility bridge. That bridge fixes native
menu discovery when an external component contributes an item without a default
menu-order entry; it also avoids leaking JMeter's logging, XML, and Kotlin
dependencies into the IntelliJ Platform class loader.

Apache JMeter 5.6.3 source is vendored under `vendor/apache-jmeter-5.6.3` so the
compatibility patch remains reviewable and reproducible.

## Build and verify

The repository includes JDK 17 wrappers for the normal verification paths:

```bash
./scripts/test-jdk17.sh
./scripts/verify-jdk17.sh
./scripts/plugin-verifier-jdk17.sh
```

`test-jdk17.sh` accepts Gradle test filters, for example
`./scripts/test-jdk17.sh --tests '*JMeterInstallationTest'`. Override the
default local JDK path with `JMETER_VIEWER_JAVA_HOME=/path/to/jdk-17`.

`verify-jdk17.sh` runs the complete test suite, builds the distributable, checks
that no JMeter installation or conflicting runtime libraries were bundled, and
enforces a 5 MiB archive-size ceiling. `plugin-verifier-jdk17.sh` runs the
IntelliJ Plugin Verifier against the supported 2022.1.4 baseline.

The plugin targets JVM 11 and IntelliJ Platform build 221 (PyCharm Community
2022.1.4). The distributable is written to `build/distributions/`.

Launch the development IDE:

```bash
JAVA_HOME=/home/duync/toolchains/jdk-17.0.4.1 \
PATH=/home/duync/toolchains/jdk-17.0.4.1/bin:$PATH \
./gradlew runIde
```

`runIde` requires a graphical environment. Run `./gradlew
prepareJMeterTestHome` if you want a generated JMeter 5.6.3 installation for
local development, then select `build/test-jmeter-home` in the sandbox IDE's
JMeter settings.
