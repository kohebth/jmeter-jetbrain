import org.jetbrains.intellij.tasks.PrepareSandboxTask
import org.jetbrains.intellij.tasks.RunPluginVerifierTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.zip.ZipFile

plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.github.kohebth"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

configurations.configureEach {
    exclude(group = "xml-apis", module = "xml-apis")
    exclude(group = "xerces", module = "xercesImpl")
    exclude(group = "org.apache.logging.log4j", module = "log4j-slf4j-impl")
    exclude(group = "org.slf4j", module = "slf4j-api")
    exclude(group = "org.slf4j", module = "slf4j-simple")
    exclude(group = "ch.qos.logback")
}

configurations.named("runtimeClasspath") {
    // IntelliJ supplies these libraries. Bundling another copy breaks binary
    // compatibility with the platform, especially on the 2022.1 baseline.
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "org.jetbrains.kotlinx")
}

val jmeterVersion = "5.6.3"

dependencies {
    implementation("org.apache.jmeter:ApacheJMeter_config:$jmeterVersion")
    implementation("org.apache.jmeter:ApacheJMeter_core:$jmeterVersion")
    implementation("org.apache.jmeter:ApacheJMeter_components:$jmeterVersion")
    implementation("org.apache.jmeter:ApacheJMeter_functions:$jmeterVersion")
    implementation("org.apache.jmeter:ApacheJMeter_bolt:$jmeterVersion")
    implementation("org.apache.jmeter:ApacheJMeter_ftp:$jmeterVersion")
    implementation("org.apache.jmeter:ApacheJMeter_http:$jmeterVersion")
    implementation("org.apache.jmeter:ApacheJMeter_java:$jmeterVersion")
    implementation("org.apache.jmeter:ApacheJMeter_jdbc:$jmeterVersion")
    implementation("org.apache.jmeter:ApacheJMeter_jms:$jmeterVersion")
    implementation("org.apache.jmeter:ApacheJMeter_junit:$jmeterVersion")
    implementation("org.apache.jmeter:ApacheJMeter_ldap:$jmeterVersion")
    implementation("org.apache.jmeter:ApacheJMeter_mail:$jmeterVersion")
    implementation("org.apache.jmeter:ApacheJMeter_mongodb:$jmeterVersion")
    implementation("org.apache.jmeter:ApacheJMeter_native:$jmeterVersion")
    implementation("org.apache.jmeter:ApacheJMeter_tcp:$jmeterVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

kotlin {
    jvmToolchain(17)
}

intellij {
    pluginName.set("jmeter-jetbrains-plugin")
    version.set("2022.1.4")
    type.set("PC")
    downloadSources.set(false)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "11"
    // Build against the Kotlin API shipped by the 2022.1 IDE baseline. In
    // particular, do not emit the newer kotlin.enums.EnumEntries ABI.
    kotlinOptions.languageVersion = "1.6"
    kotlinOptions.apiVersion = "1.6"
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=all"
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named<RunPluginVerifierTask>("runPluginVerifier") {
    ideVersions.set(listOf("PC-2022.1.4"))
}

tasks.patchPluginXml {
    sinceBuild.set("221")
    untilBuild.set(provider { null })
}

tasks.withType<PrepareSandboxTask>().configureEach {
    intoChild(pluginName.map { it + "/jmeter-home/bin" })
        .from(layout.projectDirectory.dir("vendor/apache-jmeter-5.6.3/bin")) {
            include(
                "jmeter.properties",
                "saveservice.properties",
                "upgrade.properties",
                "user.properties",
                "system.properties",
                "log4j2.xml"
            )
        }
}

tasks.jar {
    from(layout.projectDirectory.file("LICENSE")) {
        into("META-INF")
        rename { "LICENSE-plugin" }
    }
    from(layout.projectDirectory.file("NOTICE")) {
        into("META-INF")
        rename { "NOTICE-plugin" }
    }
    from(layout.projectDirectory.file("vendor/apache-jmeter-5.6.3/LICENSE")) {
        into("META-INF")
        rename { "LICENSE-apache-jmeter" }
    }
    from(layout.projectDirectory.file("vendor/apache-jmeter-5.6.3/NOTICE")) {
        into("META-INF")
        rename { "NOTICE-apache-jmeter" }
    }
}

val verifyPluginRuntime by tasks.registering {
    group = "verification"
    description = "Checks that platform-owned Kotlin libraries are not bundled in the plugin."
    dependsOn(tasks.buildPlugin)

    doLast {
        val archive = tasks.buildPlugin.get().archiveFile.get().asFile
        ZipFile(archive).use { zip ->
            val forbidden = zip.entries().asSequence()
                .map { it.name.substringAfterLast('/') }
                .filter {
                    it.startsWith("kotlin-stdlib") ||
                        it.startsWith("kotlinx-coroutines")
                }
                .toList()
            check(forbidden.isEmpty()) {
                "Plugin bundles IntelliJ-owned Kotlin libraries: ${forbidden.joinToString()}"
            }
        }
    }
}

tasks.named("buildSearchableOptions") {
    enabled = false
}

tasks.named("jarSearchableOptions") {
    enabled = false
}
