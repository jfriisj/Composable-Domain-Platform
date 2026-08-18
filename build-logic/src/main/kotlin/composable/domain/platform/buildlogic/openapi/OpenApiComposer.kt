package composable.domain.platform.buildlogic.openapi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.parser.core.models.ParseOptions
import io.swagger.v3.parser.core.models.SwaggerParseResult
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object OpenApiComposer {
    private const val SUPPORTED_OPENAPI_VERSION = "3.0.3"

    private val httpMethods =
        setOf("get", "put", "post", "delete", "options", "head", "patch", "trace")

    private val componentNamespaces =
        listOf(
            "schemas",
            "responses",
            "parameters",
            "headers",
            "securitySchemes",
            "requestBodies",
            "examples",
            "links",
            "callbacks",
        )

    private val allowedTopLevelFields =
        setOf("openapi", "info", "servers", "tags", "paths", "components")

    fun compose(
        sourceContracts: List<Path>,
        title: String,
        version: String,
    ): String {
        check(sourceContracts.isNotEmpty()) {
            "At least one authoritative OpenAPI source contract must be selected"
        }
        check(title.isNotBlank()) {
            "Application OpenAPI title must not be blank"
        }
        check(version.isNotBlank()) {
            "Application OpenAPI version must not be blank"
        }

        val documents = sourceContracts.map(::readValidatedDocument)
        val aggregate = mergeValidatedDocuments(documents, title, version)
        val serialized =
            Yaml.mapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(aggregate)

        validateSerializedAggregate(serialized)
        return serialized
    }

    fun inspect(path: Path): OpenApiSummary =
        summarize(readValidatedDocument(path))

    fun mergeValidatedDocuments(
        documents: List<ObjectNode>,
        title: String,
        version: String,
    ): ObjectNode {
        check(documents.isNotEmpty()) {
            "At least one validated OpenAPI document is required"
        }

        val mapper = Yaml.mapper()
        val aggregate = mapper.createObjectNode()
        aggregate.put("openapi", SUPPORTED_OPENAPI_VERSION)
        aggregate.putObject("info")
            .put("title", title)
            .put("version", version)

        val aggregateTags = mapper.createArrayNode()
        val aggregatePaths = mapper.createObjectNode()
        val aggregateComponents = mapper.createObjectNode()

        val tagNames = linkedSetOf<String>()
        val operationIds = linkedSetOf<String>()
        val topLevelExtensions = linkedSetOf<String>()

        var serversInitialized = false
        var selectedServers: JsonNode? = null

        documents.forEachIndexed { index, document ->
            validateDocumentShape(document, "selected source ${index + 1}")
            validateLocalReferences(document, "selected source ${index + 1}")

            val sourceServers = document.get("servers")
            if (!serversInitialized) {
                selectedServers = sourceServers?.deepCopy<JsonNode>()
                serversInitialized = true
            } else {
                check(selectedServers == sourceServers) {
                    "Selected OpenAPI sources define incompatible top-level servers"
                }
            }

            mergeTags(document, aggregateTags, tagNames)
            mergePaths(document, aggregatePaths, operationIds)
            mergeComponents(document, aggregateComponents)
            mergeTopLevelExtensions(document, aggregate, topLevelExtensions)
        }

        selectedServers?.let { servers ->
            aggregate.set<JsonNode>("servers", servers)
        }

        if (!aggregateTags.isEmpty) {
            aggregate.set<ArrayNode>("tags", aggregateTags)
        }
        aggregate.set<ObjectNode>("paths", aggregatePaths)
        if (!aggregateComponents.isEmpty) {
            aggregate.set<ObjectNode>("components", aggregateComponents)
        }

        validateLocalReferences(aggregate, "aggregated application contract")
        return aggregate
    }

    fun validateSerializedAggregate(serialized: String) {
        val root = Yaml.mapper().readTree(serialized)
        check(root is ObjectNode) {
            "Serialized aggregate must be an OpenAPI object"
        }
        validateDocumentShape(root, "serialized aggregate")
        validateLocalReferences(root, "serialized aggregate")

        val result =
            OpenAPIParser().readContents(
                serialized,
                null,
                parseOptions(),
            )
        requireSuccessfulParse(result, "serialized aggregate")
    }

    private fun readValidatedDocument(path: Path): ObjectNode {
        check(Files.isRegularFile(path)) {
            "OpenAPI source does not exist: $path"
        }

        val root =
            Files.newBufferedReader(path, StandardCharsets.UTF_8).use { reader ->
                Yaml.mapper().readTree(reader)
            }
        check(root is ObjectNode) {
            "OpenAPI source must contain an object: $path"
        }
        validateDocumentShape(root, path.toString())
        validateLocalReferences(root, path.toString())

        val result =
            OpenAPIParser().readLocation(
                path.toAbsolutePath().normalize().toString(),
                null,
                parseOptions(),
            )
        requireSuccessfulParse(result, path.toString())
        return root
    }

    private fun requireSuccessfulParse(
        result: SwaggerParseResult,
        origin: String,
    ) {
        val messages =
            result.messages
                ?.filter { message -> message.isNotBlank() }
                .orEmpty()

        check(result.openAPI != null) {
            "OpenAPI parser did not produce a document for $origin"
        }
        check(messages.isEmpty()) {
            "OpenAPI validation failed for $origin: ${messages.joinToString(" | ")}"
        }
    }

    private fun parseOptions(): ParseOptions =
        ParseOptions().apply {
            setResolve(true)
        }

    private fun validateDocumentShape(
        document: ObjectNode,
        origin: String,
    ) {
        val openApiVersion = document.path("openapi").asText()
        check(openApiVersion == SUPPORTED_OPENAPI_VERSION) {
            "Unsupported or incompatible OpenAPI version in $origin: " +
                "$openApiVersion; required $SUPPORTED_OPENAPI_VERSION"
        }

        val info = document.get("info")
        check(info is ObjectNode) {
            "OpenAPI info object is required in $origin"
        }
        check(info.path("title").asText().isNotBlank()) {
            "OpenAPI info.title is required in $origin"
        }
        check(info.path("version").asText().isNotBlank()) {
            "OpenAPI info.version is required in $origin"
        }
        check(document.get("paths") is ObjectNode) {
            "OpenAPI paths object is required in $origin"
        }

        val unexpectedFields =
            document.fieldNames()
                .asSequence()
                .filterNot { field ->
                    field in allowedTopLevelFields || field.startsWith("x-")
                }
                .toList()
        check(unexpectedFields.isEmpty()) {
            "Unsupported top-level OpenAPI fields in $origin: " +
                unexpectedFields.sorted().joinToString()
        }

        document.get("servers")?.let { servers ->
            check(servers is ArrayNode) {
                "OpenAPI servers must be an array in $origin"
            }
        }
        document.get("tags")?.let { tags ->
            check(tags is ArrayNode) {
                "OpenAPI tags must be an array in $origin"
            }
        }
        document.get("components")?.let { components ->
            check(components is ObjectNode) {
                "OpenAPI components must be an object in $origin"
            }
            val unsupportedComponents =
                components.fieldNames()
                    .asSequence()
                    .filterNot { namespace ->
                        namespace in componentNamespaces || namespace.startsWith("x-")
                    }
                    .toList()
            check(unsupportedComponents.isEmpty()) {
                "Unsupported OpenAPI component namespaces in $origin: " +
                    unsupportedComponents.sorted().joinToString()
            }
        }
    }

    private fun mergeTags(
        source: ObjectNode,
        target: ArrayNode,
        tagNames: MutableSet<String>,
    ) {
        val sourceTags = source.get("tags") ?: return
        check(sourceTags is ArrayNode)

        sourceTags.forEach { tag ->
            check(tag is ObjectNode) {
                "OpenAPI tag entries must be objects"
            }
            val tagName = tag.path("name").asText()
            check(tagName.isNotBlank()) {
                "OpenAPI tag name must not be blank"
            }
            check(tagNames.add(tagName)) {
                "Duplicate OpenAPI tag across selected sources: $tagName"
            }
            target.add(tag.deepCopy<JsonNode>())
        }
    }

    private fun mergePaths(
        source: ObjectNode,
        target: ObjectNode,
        operationIds: MutableSet<String>,
    ) {
        val sourcePaths = source.get("paths") as ObjectNode
        sourcePaths.fields().forEachRemaining { entry ->
            val path = entry.key
            val pathItem = entry.value
            check(path.startsWith("/")) {
                "OpenAPI path must start with '/': $path"
            }
            check(pathItem is ObjectNode) {
                "OpenAPI path item must be an object: $path"
            }
            check(!target.has(path)) {
                "Duplicate OpenAPI path across selected sources: $path"
            }
            collectOperationIds(path, pathItem, operationIds)
            target.set<JsonNode>(path, pathItem.deepCopy<JsonNode>())
        }
    }

    private fun collectOperationIds(
        path: String,
        pathItem: ObjectNode,
        operationIds: MutableSet<String>,
    ) {
        httpMethods.forEach { method ->
            val operation = pathItem.get(method) ?: return@forEach
            check(operation is ObjectNode) {
                "OpenAPI operation must be an object: $method $path"
            }
            val operationIdNode = operation.get("operationId") ?: return@forEach
            check(operationIdNode.isTextual) {
                "OpenAPI operationId must be text: $method $path"
            }
            val operationId = operationIdNode.asText()
            check(operationId.isNotBlank()) {
                "OpenAPI operationId must not be blank: $method $path"
            }
            check(operationIds.add(operationId)) {
                "Duplicate OpenAPI operationId across selected sources: $operationId"
            }
        }
    }

    private fun mergeComponents(
        source: ObjectNode,
        target: ObjectNode,
    ) {
        val sourceComponents = source.get("components") as? ObjectNode ?: return

        componentNamespaces.forEach { namespace ->
            val sourceNamespace = sourceComponents.get(namespace) ?: return@forEach
            check(sourceNamespace is ObjectNode) {
                "OpenAPI components/$namespace must be an object"
            }
            val targetNamespace =
                (target.get(namespace) as? ObjectNode)
                    ?: Yaml.mapper().createObjectNode().also { created ->
                        target.set<ObjectNode>(namespace, created)
                    }

            sourceNamespace.fields().forEachRemaining { entry ->
                check(!targetNamespace.has(entry.key)) {
                    "Duplicate OpenAPI component across selected sources: " +
                        "$namespace/${entry.key}"
                }
                targetNamespace.set<JsonNode>(
                    entry.key,
                    entry.value.deepCopy<JsonNode>(),
                )
            }
        }

        sourceComponents.fields().forEachRemaining { entry ->
            if (!entry.key.startsWith("x-")) {
                return@forEachRemaining
            }
            check(!target.has(entry.key)) {
                "Duplicate OpenAPI component extension across selected sources: ${entry.key}"
            }
            target.set<JsonNode>(entry.key, entry.value.deepCopy<JsonNode>())
        }
    }

    private fun mergeTopLevelExtensions(
        source: ObjectNode,
        target: ObjectNode,
        extensionNames: MutableSet<String>,
    ) {
        source.fields().forEachRemaining { entry ->
            if (!entry.key.startsWith("x-")) {
                return@forEachRemaining
            }
            check(extensionNames.add(entry.key)) {
                "Duplicate top-level OpenAPI extension across selected sources: ${entry.key}"
            }
            target.set<JsonNode>(entry.key, entry.value.deepCopy<JsonNode>())
        }
    }

    private fun validateLocalReferences(
        root: ObjectNode,
        origin: String,
    ) {
        visitReferences(root, root, origin)
    }

    private fun visitReferences(
        root: ObjectNode,
        node: JsonNode,
        origin: String,
    ) {
        when {
            node.isObject -> {
                node.fields().forEachRemaining { entry ->
                    if (entry.key == "\$ref") {
                        check(entry.value.isTextual) {
                            "OpenAPI \$ref must be textual in $origin"
                        }
                        val reference = entry.value.asText()
                        check(reference.startsWith("#/")) {
                            "Only local OpenAPI references are accepted in $origin: $reference"
                        }
                        val target = root.at(reference.substring(1))
                        check(!target.isMissingNode) {
                            "Unresolved OpenAPI reference in $origin: $reference"
                        }
                    } else {
                        visitReferences(root, entry.value, origin)
                    }
                }
            }
            node.isArray -> node.forEach { child -> visitReferences(root, child, origin) }
        }
    }

    private fun summarize(document: ObjectNode): OpenApiSummary {
        val operationIds = linkedSetOf<String>()
        val paths = document.get("paths") as ObjectNode
        paths.fields().forEachRemaining { entry ->
            val pathItem = entry.value as ObjectNode
            httpMethods.forEach { method ->
                val operation = pathItem.get(method) as? ObjectNode ?: return@forEach
                val operationId = operation.get("operationId")
                if (operationId != null && operationId.isTextual) {
                    operationId.asText()
                        .takeIf(String::isNotBlank)
                        ?.let(operationIds::add)
                }
            }
        }

        val components = document.get("components") as? ObjectNode
        val securitySchemes =
            (components?.get("securitySchemes") as? ObjectNode)
                ?.fieldNames()
                ?.asSequence()
                ?.toSet()
                .orEmpty()
        val componentNames =
            componentNamespaces
                .flatMap { namespace ->
                    (components?.get(namespace) as? ObjectNode)
                        ?.fieldNames()
                        ?.asSequence()
                        ?.toList()
                        .orEmpty()
                }
                .toSet()

        return OpenApiSummary(
            operationIds = operationIds,
            securitySchemes = securitySchemes,
            componentNames = componentNames,
        )
    }
}

data class OpenApiSummary(
    val operationIds: Set<String>,
    val securitySchemes: Set<String>,
    val componentNames: Set<String>,
)
