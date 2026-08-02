plugins {
    base
}

tasks.named("check") {
    dependsOn(
        gradle.includedBuild("build-logic").task(":check"),
        ":core:check",
        ":event-api:check",
        ":event-impl:check",
    )
}
