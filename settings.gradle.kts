pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "composable-domain-platform"

include(":core")
project(":core").projectDir = file("core")

include(":event-api")
project(":event-api").projectDir = file("modules/event/api")

include(":event-impl")
project(":event-impl").projectDir = file("modules/event/impl")

include(":http-interface")
project(":http-interface").projectDir = file("interfaces/http")

include(":platform-app")
project(":platform-app").projectDir = file("apps/platform")
