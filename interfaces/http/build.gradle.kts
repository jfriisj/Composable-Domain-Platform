plugins {
    id("composable-domain-platform.java-library-conventions")
    alias(libs.plugins.openapi.generator)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))
    implementation(project(":event-api"))
    implementation(platform(libs.spring.boot.dependencies))
}
