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
    implementation(project(":event-api"))
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
    testImplementation(libs.spring.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

openApiGenerate {
    generatorName.set("spring")
    library.set("spring-boot")
    inputSpec.set(rootProject.file("platform/contracts/http/v1/event.yaml").absolutePath)
    outputDir.set(generatedOpenApi)
    apiPackage.set("composable.domain.platform.http.generated.api")
    invokerPackage.set("composable.domain.platform.http.generated")
    modelPackage.set("composable.domain.platform.http.generated.model")
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

sourceSets.named("main") {
    java.srcDir(generatedOpenApi.map { it.dir("src/main/java") })
}

tasks.named("compileJava") {
    dependsOn(tasks.named("openApiGenerate"))
}

tasks.test {
    useJUnitPlatform()
}
