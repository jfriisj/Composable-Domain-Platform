package composable.domain.platform.buildlogic.openapi

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Verification task has no output")
abstract class VerifyOpenApiApplicationContractTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFile: RegularFileProperty

    @get:Input
    abstract val requiredOperationIds: SetProperty<String>

    @get:Input
    abstract val forbiddenOperationIds: SetProperty<String>

    @get:Input
    abstract val requiredSecuritySchemes: SetProperty<String>

    @get:Input
    abstract val forbiddenSecuritySchemes: SetProperty<String>

    @get:Input
    abstract val forbiddenComponentNames: SetProperty<String>

    @TaskAction
    fun verifyContract() {
        val summary = OpenApiComposer.inspect(inputFile.get().asFile.toPath())

        val missingOperations = requiredOperationIds.get() - summary.operationIds
        check(missingOperations.isEmpty()) {
            "Application OpenAPI contract is missing required operationIds: " +
                missingOperations.sorted().joinToString()
        }

        val unexpectedOperations = forbiddenOperationIds.get() intersect summary.operationIds
        check(unexpectedOperations.isEmpty()) {
            "Application OpenAPI contract contains forbidden operationIds: " +
                unexpectedOperations.sorted().joinToString()
        }

        val missingSecurity = requiredSecuritySchemes.get() - summary.securitySchemes
        check(missingSecurity.isEmpty()) {
            "Application OpenAPI contract is missing required security schemes: " +
                missingSecurity.sorted().joinToString()
        }

        val unexpectedSecurity =
            forbiddenSecuritySchemes.get() intersect summary.securitySchemes
        check(unexpectedSecurity.isEmpty()) {
            "Application OpenAPI contract contains forbidden security schemes: " +
                unexpectedSecurity.sorted().joinToString()
        }

        val unexpectedComponents =
            forbiddenComponentNames.get() intersect summary.componentNames
        check(unexpectedComponents.isEmpty()) {
            "Application OpenAPI contract contains forbidden components: " +
                unexpectedComponents.sorted().joinToString()
        }
    }
}
