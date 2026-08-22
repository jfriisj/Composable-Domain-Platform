plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.swagger.parser.v3:swagger-parser:2.1.45")
    runtimeOnly(kotlin("stdlib"))

    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

val validateAuthoritativeDocumentation = tasks.register<JavaExec>("validateAuthoritativeDocumentation") {
    group = "verification"
    description = "Validates registered authoritative documentation structure and size."
    dependsOn(tasks.named("classes"))
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("composable.domain.platform.buildlogic.docs.AuthoritativeDocumentationValidatorKt")
    args(rootDir.parentFile.absolutePath)
    workingDir(rootDir.parentFile)
}

tasks.named("check") {
    dependsOn(validateAuthoritativeDocumentation)
}
