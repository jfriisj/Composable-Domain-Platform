plugins {
    id("composable-domain-platform.java-application-conventions")
    id("composable-domain-platform.openapi-application-contract")
    alias(libs.plugins.spring.boot)
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("composable.domain.platform.app.PlatformApplication")
}

openApiApplicationContract {
    sourceContracts.set(
        listOf(
            "platform/contracts/http/v1/event.yaml",
            "platform/contracts/http/v1/event-registration.yaml",
        )
    )
    title.set("Composable Domain Platform API")
    version.set("1.0.0")
    outputFile.set(layout.buildDirectory.file("generated/openapi/application.yaml"))
    requiredOperationIds.set(
        setOf(
            "discoverEvents",
            "defineEvent",
            "findEventById",
            "publishEvent",
            "updateEvent",
            "createEventRegistration",
            "findEventRegistrationById",
            "cancelEventRegistration",
        )
    )
    requiredSecuritySchemes.set(setOf("ParticipantBasicAuth", "PlatformActorBasicAuth"))
}

dependencies {
    implementation(project(":event-api"))
    implementation(project(":event-impl"))
    implementation(project(":registration-api"))
    implementation(project(":registration-impl"))
    implementation(project(":security-api"))
    implementation(project(":security-impl"))
    implementation(project(":event-management-composition"))
    implementation(project(":event-registration-composition"))
    implementation(project(":http-interface"))
    implementation(project(":event-registration-http-interface"))
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.archunit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.spring.boot.starter.security)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
