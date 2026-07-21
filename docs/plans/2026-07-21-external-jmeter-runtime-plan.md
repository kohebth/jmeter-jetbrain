# Plan: External JMeter Runtime

**Date:** 2026-07-21
**Goal:** Let users select an official Apache JMeter 5.6.3 installation while preserving the embedded native editor and reducing the distributable plugin from 67.2 MB to at most 5 MB.
**Approach:** Load the selected JMeter installation in an isolated classloader and bundle only the patched 5.6.3 core jar required for the embedding contract.
**Complexity:** Complex

---

## Context Discovered

- `build.gradle.kts` currently places every JMeter module and its transitive dependencies on the plugin runtime classpath; the resulting ZIP is 67,190,142 bytes.
- `JMeterRuntimeService` currently assumes a bundled `jmeter-home`, while `JMeterRuntime` and `JMeterWorkspaceService` directly reference `org.apache.jmeter.*` classes. Those references force JMeter into IntelliJ's plugin classloader and permit logging/XML conflicts such as the `MenuFactory` initialization failure.
- The native editor depends on a small source fork: `EmbeddedJMeterWorkspace` plus changes in `MainFrame`, `GuiPackage`, and `MenuFactory`. Stock JMeter cannot provide the existing embedded lifecycle by itself.
- The complete patched `ApacheJMeter_core-5.6.3.jar` is 2,041,106 bytes. Keeping it outside the plugin `lib` directory allows it to override the stock core only inside the isolated JMeter runtime.
- There is no persistent JMeter-home setting or Settings configurable today. The editor already has a recoverable load-error panel where a configuration action can be added.
- The vendored JMeter source remains useful for building and testing the compatibility bridge. This change targets the installed plugin size, not repository size.

---

## Approaches Considered

| # | Approach | Pros | Cons | Effort |
|---|----------|------|------|--------|
| 1 | External official JMeter 5.6.3 plus full patched-core bridge ✓ | Uses the standard JMeter distribution, preserves the current native UI contract, isolates dependency conflicts, and should produce a 2–3 MB plugin | Version-locked until another bridge is built and verified | High |
| 2 | External official JMeter plus a jar containing only changed class files | Smallest artifact while still accepting stock JMeter | Fragile around inner classes, resources, and binary linkage; easy to create a partially patched core | High |
| 3 | Require users to install a custom patched JMeter distribution | Almost no JMeter payload in the plugin | Burdens users with a nonstandard JMeter build and complicates upgrades and support | Medium |

**Why option 1:** The full patched core is small compared with the current dependency bundle and is the safest binary unit for the four coordinated JMeter GUI changes. An isolated loader with the JDK platform loader as parent also prevents IntelliJ or another plugin from supplying incompatible SLF4J, Log4j, or XML implementations.

---

## Execution Steps

### Phase 1: Configuration and Installation Contract

| # | Step | Expected Output | Depends On |
|---|------|-----------------|------------|
| 1 | Add a JMeter-installation model and validator for the selected root, requiring 5.6.3 and checking `bin/ApacheJMeter.jar`, `bin/jmeter.properties`, `bin/saveservice.properties`, `lib`, and `lib/ext` | Platform-neutral validation with actionable failures and unit tests for missing, malformed, and unsupported installations | — |
| 2 | Add application-level persistent settings and a `Settings > Tools > JMeter` configurable with a directory chooser | A saved JMeter home that can be configured before runtime initialization | Step 1 |
| 3 | Extend the editor error state with a `Configure JMeter` action while retaining `Retry` and `Open Text` | Users can recover from an unset or invalid path without leaving the JMX editor | Step 2 |

### Phase 2: Isolated Runtime Boundary

| # | Step | Expected Output | Depends On |
|---|------|-----------------|------------|
| 4 | Build a closeable runtime classloader whose ordered URLs are the bundled bridge first, then the selected installation's `bin/ApacheJMeter.jar`, `lib/*.jar`, `lib/ext/*.jar`, and `lib/junit/*.jar`; use the JDK platform classloader as parent | JMeter, its logging stack, XML providers, modules, and user plugins are isolated from IntelliJ's plugin classloader | Step 1 |
| 5 | Replace direct JMeter initialization with reflection under the runtime context classloader, including JMeter home, properties, locale, save-service initialization, and version verification | `src/main` contains no direct `org.apache.jmeter.*` references and initialization errors identify the selected installation | Step 4 |
| 6 | Introduce a plugin-owned workspace interface and reflective adapter for `create`, component access, document load/snapshot, dirty state, saved baseline, dialog parent, and close | Only JDK/Swing values cross the classloader boundary; the existing editor synchronization lifecycle remains intact | Step 5 |
| 7 | Make `JMeterRuntimeService` own the initialized loader/adapter lifecycle and consume persistent settings | First-time configuration can be retried immediately; changing an already active runtime reports that an IDE restart is required rather than mixing installations | Steps 2, 5, 6 |

### Phase 3: Build and Packaging Boundary

| # | Step | Expected Output | Depends On |
|---|------|-----------------|------------|
| 8 | Remove JMeter modules from the plugin runtime classpath; create a non-transitive bridge configuration built from the vendored patched core and copy it into a non-`lib` bridge directory | The plugin classloader sees only plugin code; the isolated loader receives one patched core jar | Phase 2 |
| 9 | Replace the bundled `jmeter-home` preparation with deterministic test-only JMeter installation fixtures assembled from separate Gradle configurations | Tests exercise the production external-home layout without shipping that fixture in the plugin | Step 8 |
| 10 | Strengthen `verifyPluginRuntime` to reject JMeter/runtime dependency jars in plugin `lib`, require exactly one bridge jar in its bridge directory, and enforce a 5 MB archive ceiling | Automated protection against dependency and size regressions | Steps 8, 9 |

### Phase 4: Compatibility Verification and Documentation

| # | Step | Expected Output | Depends On |
|---|------|-----------------|------------|
| 11 | Rewrite runtime, embedding-contract, stream-loading, and menu-discovery tests to operate through the isolated reflective boundary; add classloader-isolation and settings/path tests | Coverage proves native menus initialize from external modules without linking JMeter into the plugin loader | Phases 1–3 |
| 12 | Update the README with the supported version, installation selection, validation rules, restart behavior, external-plugin discovery, and build/test instructions | Users and contributors can configure and diagnose the external runtime | Step 11 |
| 13 | Run unit tests, `verifyPluginRuntime`, a full plugin build, plugin verification, archive inspection, and a clean-worktree review | Evidence that the native editor lifecycle remains valid and the artifact meets its packaging target | Steps 10–12 |

---

## Risks & Assumptions

| # | Type | Description | Mitigation |
|---|------|-------------|------------|
| 1 | Risk | A bridge compiled for 5.6.3 could link incorrectly against another JMeter version | Reject installations that are not exactly 5.6.3 and include the detected/expected versions in the error |
| 2 | Risk | URL or resource ordering could load the stock core before the patched bridge | Put the bridge URL first, use a parent without JMeter classes, and assert the code source of `EmbeddedJMeterWorkspace` and `MainFrame` in tests |
| 3 | Risk | ServiceLoader, menu scanning, or third-party plugins might use the wrong context loader | Wrap every initialization and workspace operation with the isolated loader as TCCL and test module/menu discovery from `lib/ext` |
| 4 | Risk | JMeter's process-global Swing state could survive a path change | Keep one runtime per IDE process, clean it on application-service disposal, and require restart after changing an initialized runtime |
| 5 | Risk | Test-only JMeter dependencies could accidentally return to the plugin ZIP | Use detached/non-runtime configurations and inspect the final ZIP by path and filename |
| 6 | Assumption | The official 5.6.3 binary layout contains all libraries needed by the native GUI | Validate the required layout and exercise an equivalent complete test fixture before workspace creation |
| 7 | Assumption | A 5 MB ceiling leaves enough room for the plugin jar, patched core, licenses, and metadata | Measure the distribution after packaging; retain the ceiling as a verification check |

---

## Success Criteria

- [ ] A user can select an official Apache JMeter 5.6.3 root under `Settings > Tools > JMeter`, and the selection persists across IDE restarts.
- [ ] Missing, invalid, or unsupported installations produce an actionable editor error with `Configure JMeter`, `Retry`, and `Open Text` paths.
- [ ] A valid external installation opens a JMX file in the same embedded native tree/form editor and preserves load, edit, save, conflict, tab-switch, dialog-parent, and disposal behavior.
- [ ] Standard JMeter modules and compatible user plugins in the selected installation's `lib/ext` participate in menu discovery.
- [ ] JMeter, SLF4J/Log4j, and XML implementation classes are loaded by the isolated runtime rather than IntelliJ's plugin classloader.
- [ ] `src/main` has no direct `org.apache.jmeter.*` imports or signatures.
- [ ] The plugin ZIP contains no JMeter distribution or transitive runtime libraries in `lib`, contains exactly one patched-core bridge outside `lib`, and is no larger than 5 MB.
- [ ] Unit tests, runtime/package verification, the full build, and IntelliJ plugin verification pass.
- [ ] README documentation explains setup, the 5.6.3 compatibility requirement, restart behavior, and troubleshooting.

---

## First Action

Add failing installation-validation and classpath-layout tests that define a supported external JMeter 5.6.3 home before implementing settings or runtime loading.
