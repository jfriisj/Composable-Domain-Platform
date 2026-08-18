package composable.domain.platform.buildlogic.openapi

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

abstract class OpenApiApplicationContractExtension {
    abstract val sourceContracts: ListProperty<String>
    abstract val title: Property<String>
    abstract val version: Property<String>
    abstract val outputFile: RegularFileProperty
    abstract val requiredOperationIds: SetProperty<String>
    abstract val forbiddenOperationIds: SetProperty<String>
    abstract val requiredSecuritySchemes: SetProperty<String>
    abstract val forbiddenSecuritySchemes: SetProperty<String>
    abstract val forbiddenComponentNames: SetProperty<String>
}
