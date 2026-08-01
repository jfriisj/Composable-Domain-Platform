plugins {
    id("composable-domain-platform.java-library-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":event-api"))

    testImplementation(libs.archunit)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
