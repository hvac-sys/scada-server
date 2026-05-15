plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.mgmt)
}

dependencies {
    implementation(project(":api"))
    implementation(project(":alarm"))
    implementation(project(":core"))
    implementation(project(":historian"))
    implementation(project(":security"))
    implementation(project(":acquisition"))
    implementation(project(":websocket"))

    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation(libs.spring.modulith.core)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.spring.modulith.test)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}