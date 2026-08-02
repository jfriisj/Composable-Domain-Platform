plugins {
    id("composable-domain-platform.java-application-conventions")
    alias(libs.plugins.spring.boot)
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("composable.domain.platform.app.PlatformApplication")
}

dependencies {
    implementation(project(":event-api"))
    implementation(project(":event-impl"))
    implementation(project(":http-interface"))
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.archunit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
