pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "jmeter-jetbrains-plugin"

includeBuild("vendor/apache-jmeter-5.6.3") {
    dependencySubstitution {
        substitute(module("org.apache.jmeter:ApacheJMeter_config"))
            .using(project(":src:config"))
        substitute(module("org.apache.jmeter:ApacheJMeter_core"))
            .using(project(":src:core"))
        substitute(module("org.apache.jmeter:ApacheJMeter_components"))
            .using(project(":src:components"))
        substitute(module("org.apache.jmeter:ApacheJMeter_functions"))
            .using(project(":src:functions"))
        substitute(module("org.apache.jmeter:ApacheJMeter_bolt"))
            .using(project(":src:protocol:bolt"))
        substitute(module("org.apache.jmeter:ApacheJMeter_ftp"))
            .using(project(":src:protocol:ftp"))
        substitute(module("org.apache.jmeter:ApacheJMeter_http"))
            .using(project(":src:protocol:http"))
        substitute(module("org.apache.jmeter:ApacheJMeter_java"))
            .using(project(":src:protocol:java"))
        substitute(module("org.apache.jmeter:ApacheJMeter_jdbc"))
            .using(project(":src:protocol:jdbc"))
        substitute(module("org.apache.jmeter:ApacheJMeter_jms"))
            .using(project(":src:protocol:jms"))
        substitute(module("org.apache.jmeter:ApacheJMeter_junit"))
            .using(project(":src:protocol:junit"))
        substitute(module("org.apache.jmeter:ApacheJMeter_ldap"))
            .using(project(":src:protocol:ldap"))
        substitute(module("org.apache.jmeter:ApacheJMeter_mail"))
            .using(project(":src:protocol:mail"))
        substitute(module("org.apache.jmeter:ApacheJMeter_mongodb"))
            .using(project(":src:protocol:mongodb"))
        substitute(module("org.apache.jmeter:ApacheJMeter_native"))
            .using(project(":src:protocol:native"))
        substitute(module("org.apache.jmeter:ApacheJMeter_tcp"))
            .using(project(":src:protocol:tcp"))
    }
}

