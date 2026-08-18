package composable.domain.platform.buildlogic.openapi

import com.fasterxml.jackson.databind.node.ObjectNode
import io.swagger.v3.core.util.Yaml
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class OpenApiComposerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `composes selected valid contracts deterministically`() {
        val event = write("event.yaml", validDocument("/events", "findEvents", "Event"))
        val registration =
            write(
                "registration.yaml",
                validDocument(
                    "/registrations",
                    "createRegistration",
                    "EventRegistration",
                ),
            )

        val first =
            OpenApiComposer.compose(
                listOf(event, registration),
                "Application API",
                "1.0.0",
            )
        val second =
            OpenApiComposer.compose(
                listOf(event, registration),
                "Application API",
                "1.0.0",
            )

        assertEquals(first, second)
        val root = Yaml.mapper().readTree(first)
        assertTrue(root.path("paths").has("/events"))
        assertTrue(root.path("paths").has("/registrations"))
    }

    @Test
    fun `rejects duplicate path keys even when operations differ`() {
        val first = tree(validDocument("/shared", "firstOperation", "First"))
        val second =
            tree(
                validDocument(
                    "/shared",
                    "secondOperation",
                    "Second",
                    method = "post",
                )
            )

        val failure =
            assertThrows(IllegalStateException::class.java) {
                OpenApiComposer.mergeValidatedDocuments(
                    listOf(first, second),
                    "Application API",
                    "1.0.0",
                )
            }

        assertTrue(failure.message.orEmpty().contains("Duplicate OpenAPI path"))
    }

    @Test
    fun `rejects duplicate operationIds`() {
        val first = tree(validDocument("/first", "sameOperation", "First"))
        val second = tree(validDocument("/second", "sameOperation", "Second"))

        val failure =
            assertThrows(IllegalStateException::class.java) {
                OpenApiComposer.mergeValidatedDocuments(
                    listOf(first, second),
                    "Application API",
                    "1.0.0",
                )
            }

        assertTrue(failure.message.orEmpty().contains("Duplicate OpenAPI operationId"))
    }

    @Test
    fun `rejects duplicate component names in every governed namespace`() {
        val namespaces =
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

        namespaces.forEach { namespace ->
            val first = componentDocument(namespace, "Shared")
            val second = componentDocument(namespace, "Shared")

            val failure =
                assertThrows(IllegalStateException::class.java) {
                    OpenApiComposer.mergeValidatedDocuments(
                        listOf(first, second),
                        "Application API",
                        "1.0.0",
                    )
                }

            assertTrue(failure.message.orEmpty().contains("Duplicate OpenAPI component"))
            assertTrue(failure.message.orEmpty().contains("$namespace/Shared"))
        }
    }

    @Test
    fun `rejects unsupported or incompatible OpenAPI versions`() {
        val first = tree(validDocument("/first", "first", "First"))
        val second =
            tree(
                validDocument("/second", "second", "Second")
                    .replace("openapi: 3.0.3", "openapi: 3.1.0")
            )

        val failure =
            assertThrows(IllegalStateException::class.java) {
                OpenApiComposer.mergeValidatedDocuments(
                    listOf(first, second),
                    "Application API",
                    "1.0.0",
                )
            }

        assertTrue(
            failure.message
                .orEmpty()
                .contains("Unsupported or incompatible OpenAPI version")
        )
    }

    @Test
    fun `rejects unresolved references`() {
        val source =
            write(
                "missing-ref.yaml",
                """
                openapi: 3.0.3
                info:
                  title: Missing ref
                  version: 1.0.0
                paths:
                  /missing:
                    get:
                      operationId: missing
                      responses:
                        '200':
                          description: ok
                          content:
                            application/json:
                              schema:
                                ${'$'}ref: '#/components/schemas/Missing'
                components:
                  schemas:
                    Present:
                      type: string
                """.trimIndent() + "\n",
            )

        assertThrows(IllegalStateException::class.java) {
            OpenApiComposer.compose(listOf(source), "Application API", "1.0.0")
        }
    }

    @Test
    fun `rejects malformed or invalid source documents`() {
        val source =
            write(
                "invalid.yaml",
                """
                openapi: 3.0.3
                info:
                  title: Invalid
                  version: 1.0.0
                paths:
                  not-an-openapi-path:
                    value: true
                """.trimIndent() + "\n",
            )

        assertThrows(IllegalStateException::class.java) {
            OpenApiComposer.compose(listOf(source), "Application API", "1.0.0")
        }
    }

    @Test
    fun `rejects an invalid serialized aggregate`() {
        assertThrows(IllegalStateException::class.java) {
            OpenApiComposer.validateSerializedAggregate(
                """
                openapi: 3.0.3
                info:
                  title: Invalid aggregate
                  version: 1.0.0
                paths: []
                """.trimIndent() + "\n"
            )
        }
    }

    private fun write(name: String, content: String): Path {
        val path = tempDir.resolve(name)
        Files.writeString(path, content, StandardCharsets.UTF_8)
        return path
    }

    private fun tree(content: String): ObjectNode =
        Yaml.mapper().readTree(content) as ObjectNode

    private fun validDocument(
        path: String,
        operationId: String,
        tag: String,
        method: String = "get",
    ): String =
        """
        openapi: 3.0.3
        info:
          title: $tag API
          version: 1.0.0
        servers:
          - url: /
        tags:
          - name: $tag
        paths:
          $path:
            $method:
              tags:
                - $tag
              operationId: $operationId
              responses:
                '200':
                  description: ok
        """.trimIndent() + "\n"

    private fun componentDocument(namespace: String, name: String): ObjectNode =
        tree(
            """
            openapi: 3.0.3
            info:
              title: Component API
              version: 1.0.0
            paths: {}
            components:
              $namespace:
                $name: {}
            """.trimIndent() + "\n"
        )
}
