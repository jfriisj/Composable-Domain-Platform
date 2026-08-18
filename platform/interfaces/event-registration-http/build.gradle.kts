plugins {
    id("composable-domain-platform.java-library-conventions")
    alias(libs.plugins.openapi.generator)
}

repositories {
    mavenCentral()
}

val generatedOpenApi = layout.buildDirectory.dir("generated/openapi")

dependencies {
    implementation(project(":core"))
    implementation(project(":event-registration-composition"))
    implementation(project(":security-api"))
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.jackson.annotations)
    implementation(libs.jakarta.annotation.api)
    compileOnly(libs.jakarta.servlet.api)
    implementation(libs.jakarta.validation.api)
    implementation(libs.spring.context)
    implementation(libs.spring.web)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jakarta.servlet.api)
    testRuntimeOnly(libs.junit.platform.launcher)
}

openApiGenerate {
    generatorName.set("spring")
    library.set("spring-boot")
    inputSpec.set(
        rootProject.file(
            "platform/contracts/http/v1/event-registration.yaml"
        ).absolutePath
    )
    outputDir.set(generatedOpenApi)
    apiPackage.set(
        "composable.domain.platform.http.eventregistration.generated.api"
    )
    invokerPackage.set(
        "composable.domain.platform.http.eventregistration.generated"
    )
    modelPackage.set(
        "composable.domain.platform.http.eventregistration.generated.model"
    )
    validateSpec.set(true)
    generateApiTests.set(false)
    generateModelTests.set(false)
    generateApiDocumentation.set(false)
    generateModelDocumentation.set(false)
    configOptions.set(
        mapOf(
            "annotationLibrary" to "none",
            "documentationProvider" to "none",
            "interfaceOnly" to "true",
            "openApiNullable" to "false",
            "requestMappingMode" to "api_interface",
            "skipDefaultInterface" to "true",
            "useBeanValidation" to "true",
            "useJackson3" to "true",
            "useSpringBoot4" to "true",
            "useSwaggerUI" to "false",
            "useTags" to "true",
        )
    )
}

tasks.named("openApiGenerate") {
    doFirst {
        project.delete(generatedOpenApi.get().asFile)
    }
}

sourceSets.named("main") {
    java.srcDir(generatedOpenApi.map { it.dir("src/main/java") })
}

tasks.named("compileJava") {
    dependsOn(tasks.named("openApiGenerate"))
}

val verifyGeneratedTransportBoundary by tasks.registering {
    group = "verification"
    description =
        "Verifies that Event-Registration generation contains no Event transport types."

    dependsOn(tasks.named("openApiGenerate"))
    inputs.dir(generatedOpenApi)

    doLast {
        val javaRoot = generatedOpenApi.get().dir("src/main/java").asFile
        val registrationApi =
            javaRoot.resolve(
                "composable/domain/platform/http/eventregistration/generated/api/" +
                    "EventRegistrationApi.java"
            )
        check(registrationApi.isFile) {
            "EventRegistrationApi.java was not generated in the workflow transport package"
        }

        val forbiddenNames =
            setOf(
                "EventApi.java",
                "DefineEventRequest.java",
                "EventResponse.java",
            )
        val forbiddenFiles =
            javaRoot.walkTopDown()
                .filter { file -> file.isFile && file.name in forbiddenNames }
                .map { file -> file.relativeTo(javaRoot).path }
                .sorted()
                .toList()

        check(forbiddenFiles.isEmpty()) {
            "Event-Registration generation contains Event transport types: " +
                forbiddenFiles.joinToString()
        }
    }
}

tasks.named("check") {
    dependsOn(verifyGeneratedTransportBoundary)
}

tasks.test {
    useJUnitPlatform()
}
