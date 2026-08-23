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

include(":security-api")
project(":security-api").projectDir = file("platform/modules/security/api")

include(":security-impl")
project(":security-impl").projectDir = file("platform/modules/security/impl")

include(":event-registration-composition")
project(":event-registration-composition").projectDir = file("platform/compositions/event-registration")

include(":event-management-composition")
project(":event-management-composition").projectDir = file("platform/compositions/event-management")


include(":http-interface")
project(":http-interface").projectDir = file("platform/interfaces/http")

include(":event-registration-http-interface")
project(":event-registration-http-interface").projectDir =
    file("platform/interfaces/event-registration-http")

include(":platform-app")
project(":platform-app").projectDir = file("platform/apps/platform")

include(":event-app")
project(":event-app").projectDir = file("platform/apps/event")
