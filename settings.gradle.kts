pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "composable-domain-platform"

include(":core")
project(":core").projectDir = file("platform/core")

include(":event-api")
project(":event-api").projectDir = file("platform/modules/event/api")

include(":event-impl")
project(":event-impl").projectDir = file("platform/modules/event/impl")

include(":registration-api")
project(":registration-api").projectDir = file("platform/modules/registration/api")

include(":registration-impl")
project(":registration-impl").projectDir = file("platform/modules/registration/impl")

include(":event-registration-composition")
project(":event-registration-composition").projectDir = file("platform/compositions/event-registration")

include(":http-interface")
project(":http-interface").projectDir = file("platform/interfaces/http")

include(":platform-app")
project(":platform-app").projectDir = file("platform/apps/platform")
