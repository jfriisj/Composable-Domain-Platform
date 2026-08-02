plugins {
    id("composable-domain-platform.java-application-conventions")
    alias(libs.plugins.spring.boot)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":event-api"))
    implementation(project(":event-impl"))
    implementation(project(":http-interface"))
    implementation(platform(libs.spring.boot.dependencies))
}
