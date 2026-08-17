plugins {
    id("composable-domain-platform.java-application-conventions")
    alias(libs.plugins.spring.boot)
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("composable.domain.platform.app.event.EventApplication")
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
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.junit.platform.launcher)
}

val forbiddenProjectPaths = setOf(
    ":registration-api",
    ":registration-impl",
    ":security-api",
    ":security-impl",
    ":event-registration-composition",
    ":event-registration-http-interface",
)

val verifySelectableComposition by tasks.registering {
    group = "verification"
    description =
        "Verifies that the Event-only compile/runtime graph excludes unrelated projects."

    doLast {
        listOf("compileClasspath", "runtimeClasspath").forEach { configurationName ->
            val configuration = configurations.getByName(configurationName)
            val presentForbiddenProjects = configuration.incoming.resolutionResult.allComponents
                .mapNotNull { component ->
                    (component.id as? org.gradle.api.artifacts.component.ProjectComponentIdentifier)
                        ?.projectPath
                }
                .filter(forbiddenProjectPaths::contains)
                .sorted()

            check(presentForbiddenProjects.isEmpty()) {
                "$configurationName contains forbidden Event-only project dependencies: " +
                    presentForbiddenProjects.joinToString()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(verifySelectableComposition)
}

tasks.test {
    useJUnitPlatform()
}
