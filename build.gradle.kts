plugins {
    base
}

tasks.named("check") {
    dependsOn(
        gradle.includedBuild("build-logic").task(":check"),
        ":core:check",
        ":event-api:check",
        ":event-impl:check",
        ":registration-api:check",
        ":registration-impl:check",
        ":security-api:check",
        ":security-impl:check",
        ":event-registration-composition:check",
        ":http-interface:check",
        ":event-registration-http-interface:check",
        ":platform-app:check",
        ":event-app:check",
    )
}
