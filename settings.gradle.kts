pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "composable-domain-platform"

include(":event-api")
project(":event-api").projectDir = file("modules/event/api")

include(":event-impl")
project(":event-impl").projectDir = file("modules/event/impl")
