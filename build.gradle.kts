plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.mgmt) apply false
}

group = "com.hvacsys"
version = "0.0.1-SNAPSHOT"
description = "scada-server"

subprojects {
    pluginManager.withPlugin("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(25)
            }
        }
    }

    pluginManager.withPlugin("io.spring.dependency-management") {
        extensions.configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>{
            imports {
                mavenBom(libs.spring.boot.bom.get().toString())
                mavenBom(libs.spring.modulith.bom.get().toString())
            }
        }
    }

    repositories {
        mavenCentral()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

}

