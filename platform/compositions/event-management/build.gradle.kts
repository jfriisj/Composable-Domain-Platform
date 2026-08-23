plugins {
    id("composable-domain-platform.java-library-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":core"))
    implementation(project(":event-api"))
    implementation(project(":security-api"))

    testImplementation(libs.archunit)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
