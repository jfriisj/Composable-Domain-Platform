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

include(":registration-api")
project(":registration-api").projectDir = file("modules/registration/api")

include(":registration-impl")
project(":registration-impl").projectDir = file("modules/registration/impl")

include(":event-registration-composition")
project(":event-registration-composition").projectDir = file("compositions/event-registration")

include(":http-interface")
project(":http-interface").projectDir = file("interfaces/http")

include(":platform-app")
project(":platform-app").projectDir = file("apps/platform")
