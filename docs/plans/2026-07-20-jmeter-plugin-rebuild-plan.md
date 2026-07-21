# Plan: Rebuild the JMeter JetBrains Plugin

**Date:** 2026-07-20
**Goal:** Build a fresh Kotlin-based JetBrains plugin that embeds JMeter's native editable tree and element forms for `.jmx` files, then validate it interactively after each runnable checkpoint.
**Approach:** Vendor Apache JMeter 5.6.3, add a minimal embedded-GUI fork, and keep JMX as the source of truth through JMeter's own load/save actions.
**Complexity:** Complex

**Implementation status:** Checkpoints 1 and 2 are implemented. Automated
runtime, streaming JMX, editor synchronization, registration, native catalog,
full test, distribution, packaging, and JetBrains compatibility checks pass.
The remaining review gate is an interactive two-file smoke test in a graphical
IDE.

---

## Context Discovered

- The repository is intentionally empty except for Git and workspace metadata; the removed plugin will not be restored.
- Apache JMeter 5.6.3 provides an experimental Kotlin DSL and native `Copy Code` action, but existing JMX files are still loaded by `SaveService.loadTree`.
- JMeter's GUI relies on the process-global `GuiPackage` and a `MainFrame`; the previous multi-editor experiment failed when a second editor replaced that singleton.
- JDK 17 and PyCharm Community 2022.1.4 are available locally; the root plugin build will emit JVM 11-compatible bytecode for that sandbox.

---

## Approaches Considered

| # | Approach | Pros | Cons | Effort |
|---|----------|------|------|--------|
| 1 | Minimal JMeter UI fork ✓ | Native UI/actions, no custom JMX model, controlled embedding API | Maintains a small upstream patch | High |
| 2 | Thin host around unmodified jars | Smallest source footprint | Requires brittle `MainFrame` content extraction and cannot improve embedding lifecycle | Medium |
| 3 | Kotlin DSL round-trip | Makes generated Kotlin visible in the data path | Still requires JMX loading, adds compilation/evaluation and security risks | High |

**Why option 1:** It preserves JMeter's native editor and `Copy Code` behavior while giving the plugin an explicit embedded mode and avoiding reflection against private singleton state.

---

## Execution Steps

### Checkpoint 1: Reproducible Build Foundation

| # | Step | Expected Output | Depends On |
|---|------|-----------------|------------|
| 1 | Extract the official Apache JMeter 5.6.3 source archive into `vendor/apache-jmeter-5.6.3`, retaining upstream license and notice files; do not restore the old plugin. | Tracked upstream source available for the fork | — |
| 2 | Create a fresh Gradle wrapper and Kotlin-based IntelliJ Platform plugin targeting PyCharm Community 2022.1.4, using JDK 17 to build and JVM 11 bytecode. | Minimal plugin project with deterministic tool versions | Step 1 |
| 3 | Add the vendored JMeter build as a Gradle composite build with explicit dependency substitutions for the required JMeter modules. | Plugin compiles against the local fork rather than Maven's stock core | Step 2 |
| 4 | Bundle the required JMeter `bin` configuration as plugin runtime data and initialize JMeter from the installed plugin path. Exclude duplicate XML parsers and conflicting SLF4J bindings. | JMeter runtime initializes safely inside the IDE | Step 3 |
| 5 | Add the minimal plugin descriptor and a no-op application service, then run unit tests, `build`, and a `runIde` smoke launch. | PyCharm sandbox starts with the plugin enabled | Step 4 |
| 6 | Stop and report the exact run command and observed result for user confirmation. | Checkpoint 1 review gate | Step 5 |

### Checkpoint 2: Native Editable JMX Editor

| # | Step | Expected Output | Depends On |
|---|------|-----------------|------------|
| 7 | Patch the vendored JMeter core with an embedded workspace mode that exposes the native tree/form split without the standalone menu, logger, or runner UI while retaining a hidden `MainFrame` action controller. | Supported embeddable JMeter component without reflection | Checkpoint 1 approval |
| 8 | Add one application-level host service that owns the sole `GuiPackage`, native controller, active project/JMX path, and workspace component across all IDE windows. | Exactly one JMeter GUI state in the IDE process | Step 7 |
| 9 | Register `.jmx` and a `FileEditorProvider`; attach the shared workspace to the selected visual editor and load through `SaveService.loadTree` plus JMeter's native tree insertion path. | Opening a JMX shows its real tree and selected-element form | Step 8 |
| 10 | Preserve native selection, forms, context menus, structural edit actions, and `Copy Code`. Keep JMX as the only persisted source; generated Kotlin remains export-only. | Native editing behavior with Kotlin DSL copy support | Step 9 |
| 11 | Route IntelliJ Save/Ctrl+S through JMeter's native update/save action. Before activating another JMX visual tab, flush and auto-save the current plan, then load the selected file into the shared model. | Edits persist and switching tabs cannot replace unsaved singleton state | Step 10 |
| 12 | Add editor-visible load/save errors and safe disposal. A failed load must keep the original file and leave the host recoverable for another file. | Actionable failures without data loss | Step 11 |
| 13 | Run automated tests and a two-file sandbox smoke test, then stop for user validation of rendering, editing, saving, tab switching, and `Copy Code`. | Checkpoint 2 review gate | Step 12 |

---

## Risks & Assumptions

| # | Type | Description | Mitigation |
|---|------|-------------|------------|
| 1 | Risk | JMeter and IntelliJ may provide conflicting XML/logging libraries. | Exclude duplicate parser artifacts and logging bindings; verify startup and classloading in the sandbox. |
| 2 | Risk | JMeter's GUI assumes a top-level `MainFrame`. | Keep a hidden controller instance and add only a supported embedded workspace API in the fork. |
| 3 | Risk | JMeter has global GUI state shared by every open IDE project. | Own one application-level host and swap one active model; never create a `GuiPackage` per project or editor tab. |
| 4 | Risk | Auto-save on tab switching could overwrite invalid external edits. | Flush only the active native model, use JMeter's native save path, report failures, and do not switch models after a failed save. |
| 5 | Assumption | First delivery needs bundled JMeter elements only. | Defer external `lib/ext` plugin discovery and custom classpaths. |
| 6 | Assumption | Test execution and results are outside the first editor milestone. | Hide runner UI and add execution only after the native editor is validated. |

---

## Success Criteria

- [ ] Checkpoint 1: `build` and tests pass, and the PyCharm 2022.1.4 sandbox starts with the fresh plugin enabled.
- [ ] Opening a JMX displays JMeter's native tree and native selected-element form inside the IDE.
- [ ] Selecting a node updates the form; field and structural edits survive Save and reload.
- [ ] The native `Copy Code` action copies Kotlin DSL for the selected subtree.
- [ ] Switching between two JMX visual tabs auto-saves the first and loads the correct second plan.
- [ ] A malformed JMX or failed save produces an editor error without damaging the source file or global host.
- [ ] No old plugin implementation is restored, and no custom JMX parser or Kotlin evaluator is introduced.

---

## Next Validation

Launch the PyCharm sandbox in a graphical environment and validate rendering,
field and structural edits, Save/Save All, raw XML switching, two-file visual
tab switching, conflict choices, and native `Copy Code` clipboard output.
