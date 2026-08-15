plugins {
    id("composable-domain-platform.java-library-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":security-api"))
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.boot.starter.security)
    compileOnly(libs.jakarta.servlet.api)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jakarta.servlet.api)
    testImplementation(libs.spring.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
