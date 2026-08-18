import composable.domain.platform.buildlogic.openapi.ComposeOpenApiTask
import composable.domain.platform.buildlogic.openapi.OpenApiApplicationContractExtension
import composable.domain.platform.buildlogic.openapi.VerifyOpenApiApplicationContractTask

val openApiApplicationContract =
    extensions.create<OpenApiApplicationContractExtension>("openApiApplicationContract")

openApiApplicationContract.outputFile.convention(
    layout.buildDirectory.file("generated/openapi/application.yaml")
)
openApiApplicationContract.requiredOperationIds.convention(emptySet())
openApiApplicationContract.forbiddenOperationIds.convention(emptySet())
openApiApplicationContract.requiredSecuritySchemes.convention(emptySet())
openApiApplicationContract.forbiddenSecuritySchemes.convention(emptySet())
openApiApplicationContract.forbiddenComponentNames.convention(emptySet())

val composeApplicationOpenApi =
    tasks.register<ComposeOpenApiTask>("composeApplicationOpenApi") {
        group = "build"
        description =
            "Composes the application OpenAPI contract from explicitly selected source contracts."

        sourceContractPaths.set(openApiApplicationContract.sourceContracts)
        sourceContractFiles.from(
            openApiApplicationContract.sourceContracts.map { paths ->
                paths.map { path -> rootProject.layout.projectDirectory.file(path) }
            }
        )
        contractTitle.set(openApiApplicationContract.title)
        contractVersion.set(openApiApplicationContract.version)
        rootDirectory.set(rootProject.layout.projectDirectory)
        outputFile.set(openApiApplicationContract.outputFile)
    }

val verifyApplicationOpenApi =
    tasks.register<VerifyOpenApiApplicationContractTask>("verifyApplicationOpenApi") {
        group = "verification"
        description =
            "Verifies the selected application OpenAPI surface and exclusions."

        dependsOn(composeApplicationOpenApi)
        inputFile.set(openApiApplicationContract.outputFile)
        requiredOperationIds.set(openApiApplicationContract.requiredOperationIds)
        forbiddenOperationIds.set(openApiApplicationContract.forbiddenOperationIds)
        requiredSecuritySchemes.set(openApiApplicationContract.requiredSecuritySchemes)
        forbiddenSecuritySchemes.set(openApiApplicationContract.forbiddenSecuritySchemes)
        forbiddenComponentNames.set(openApiApplicationContract.forbiddenComponentNames)
    }

tasks.matching { task -> task.name == "check" }.configureEach {
    dependsOn(verifyApplicationOpenApi)
}
