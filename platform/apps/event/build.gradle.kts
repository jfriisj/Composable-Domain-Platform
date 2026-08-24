plugins {
    id("composable-domain-platform.java-application-conventions")
    id("composable-domain-platform.openapi-application-contract")
    alias(libs.plugins.spring.boot)
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("composable.domain.platform.app.event.EventApplication")
}

openApiApplicationContract {
    sourceContracts.set(
        listOf(
            "platform/contracts/http/v1/event.yaml",
        )
    )
    title.set("Composable Domain Platform Event Application API")
    version.set("1.0.0")
    outputFile.set(layout.buildDirectory.file("generated/openapi/application.yaml"))
    requiredOperationIds.set(
        setOf(
            "discoverEvents",
            "defineEvent",
            "updateEvent",
            "findEventById",
            "publishEvent",
            "withdrawEvent",
        )
    )
    forbiddenOperationIds.set(
        setOf(
            "createEventRegistration",
            "findEventRegistrationById",
            "cancelEventRegistration",
            "findOrganizerEventRegistrations",
        )
    )
    requiredSecuritySchemes.set(setOf("PlatformActorBasicAuth"))
    forbiddenSecuritySchemes.set(setOf("ParticipantBasicAuth", "OrganizerBasicAuth"))
    forbiddenComponentNames.set(
        setOf(
            "CreateEventRegistrationRequest",
            "EventRegistrationResponse",
            "EventRegistrationLifecycle",
            "EventRegistrationErrorResponse",
            "EventRegistrationCorrelationId",
            "EventRegistrationEventId",
            "RegistrationId",
        )
    )
}

dependencies {
    implementation(project(":event-api"))
    implementation(project(":event-impl"))
    implementation(project(":event-management-composition"))
    implementation(project(":security-api"))
    implementation(project(":security-impl"))
    implementation(project(":http-interface"))
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql)
    implementation(libs.spring.boot.starter.security)
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
