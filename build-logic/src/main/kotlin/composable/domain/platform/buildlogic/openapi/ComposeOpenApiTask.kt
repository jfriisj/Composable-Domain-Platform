package composable.domain.platform.buildlogic.openapi

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ComposeOpenApiTask : DefaultTask() {
    @get:Input
    abstract val sourceContractPaths: ListProperty<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceContractFiles: ConfigurableFileCollection

    @get:Input
    abstract val contractTitle: Property<String>

    @get:Input
    abstract val contractVersion: Property<String>

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun compose() {
        val paths = sourceContractPaths.get()
        check(paths.isNotEmpty()) {
            "At least one authoritative OpenAPI source contract must be selected"
        }
        check(paths.distinct().size == paths.size) {
            "OpenAPI source contract selection contains duplicates: $paths"
        }

        val sourceFiles =
            paths.map { path ->
                rootDirectory.file(path).get().asFile.toPath()
            }
        val serialized =
            OpenApiComposer.compose(
                sourceContracts = sourceFiles,
                title = contractTitle.get(),
                version = contractVersion.get(),
            )

        val output = outputFile.get().asFile.toPath()
        Files.createDirectories(output.parent)
        Files.writeString(output, serialized, StandardCharsets.UTF_8)
    }
}
