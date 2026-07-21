# JMeter Viewer for JetBrains IDEs

JetBrains plugin that opens local `.jmx` files with Apache JMeter's native
editable tree, element forms, structural actions, and `Copy Code` support.
The ordinary XML editor remains available beside the visual editor.

Apache JMeter 5.6.3 is vendored under `vendor/apache-jmeter-5.6.3` so the
embedded GUI integration can be maintained as a small, explicit fork.

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

The first release supports local physical JMX files and the JMeter modules
bundled with the plugin. External JMeter plugins, execution/results, recorder,
templates, and report generation are intentionally deferred.

## Build and verify

Use JDK 17 to run the build:

```bash
JAVA_HOME=/home/duync/toolchains/jdk-17.0.4.1 \
PATH=/home/duync/toolchains/jdk-17.0.4.1/bin:$PATH \
./gradlew test verifyPluginRuntime
```

The plugin targets JVM 11 and IntelliJ Platform build 221 (PyCharm Community
2022.1.4). The distributable is written to `build/distributions/`.

Launch the development IDE:

```bash
JAVA_HOME=/home/duync/toolchains/jdk-17.0.4.1 \
PATH=/home/duync/toolchains/jdk-17.0.4.1/bin:$PATH \
./gradlew runIde
```

`runIde` requires a graphical environment.
