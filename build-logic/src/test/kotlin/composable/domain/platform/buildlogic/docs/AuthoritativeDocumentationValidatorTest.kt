package composable.domain.platform.buildlogic.docs

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class AuthoritativeDocumentationValidatorTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `accepts valid registered documentation and an explicit budget exception`() {
        writeTemplate("scope.template.md", "## Purpose")
        writeModel(
            registration("Scope", "docs/scope.md", "scope.template.md", 4),
            exception("docs/scope.md", 5, "Temporary migration", "Remove after compaction"),
        )
        write("docs/scope.md", "# Scope\n\n## Purpose\n\nValue\n")

        assertTrue(validate().isEmpty())
    }

    @Test
    fun `enumerates registered glob matches`() {
        writeTemplate("adr.template.md", "## Status", "## Context")
        writeModel(
            registration("ADR", "docs/adr/[0-9][0-9][0-9][0-9]-*.md", "adr.template.md", 10)
        )
        write("docs/adr/0001-first.md", "# ADR-0001\n\n## Status\n\n## Context\n")
        write("docs/adr/0002-second.md", "# ADR-0002\n\n## Status\n\n## Context")

        val violations = validate()

        assertTrue(
            violations.any {
                it.path == "docs/adr/0002-second.md" && it.rule == "missing final newline"
            }
        )
    }

    @Test
    fun `rejects missing registered template with exact diagnostic`() {
        writeModel(registration("Scope", "docs/scope.md", "scope.template.md", 10))
        write("docs/scope.md", "# Scope\n\n## Purpose\n")

        val violations = validate()

        assertTrue(
            violations.any {
                it.path == "docs/templates/scope.template.md" &&
                    it.rule == "registered template is missing for docs/scope.md"
            }
        )
    }

    @Test
    fun `rejects required H2 headings out of order`() {
        writeTemplate("scope.template.md", "## Purpose", "## Current accepted product boundary")
        writeModel(registration("Scope", "docs/scope.md", "scope.template.md", 10))
        write(
            "docs/scope.md",
            "# Scope\n\n## Current accepted product boundary\n\n## Purpose\n",
        )

        val violations = validate()

        assertTrue(
            violations.any {
                it.path == "docs/scope.md" && it.rule.startsWith("H2 structure mismatch:")
            }
        )
    }

    @Test
    fun `rejects line budget overflow`() {
        writeTemplate("scope.template.md", "## Purpose")
        writeModel(registration("Scope", "docs/scope.md", "scope.template.md", 4))
        write("docs/scope.md", "# Scope\n\n## Purpose\n\nValue\n")

        val violations = validate()

        assertTrue(
            violations.any {
                it.path == "docs/scope.md" && it.rule == "line budget exceeded: 5 > 4"
            }
        )
    }

    @Test
    fun `rejects missing final newline`() {
        writeTemplate("scope.template.md", "## Purpose")
        writeModel(registration("Scope", "docs/scope.md", "scope.template.md", 10))
        write("docs/scope.md", "# Scope\n\n## Purpose")

        val violations = validate()

        assertTrue(
            violations.any {
                it.path == "docs/scope.md" && it.rule == "missing final newline"
            }
        )
    }

    @Test
    fun `rejects prohibited project status history H2`() {
        writeTemplate("project-status.template.md", "## Authority", "## Current state")
        writeModel(
            registration(
                "Project status",
                "docs/project-status.md",
                "project-status.template.md",
                10,
            )
        )
        write(
            "docs/project-status.md",
            "# Project Status\n\n## Authority\n\n## Completed\n\n## Current state\n",
        )

        val violations = validate()

        assertTrue(
            violations.any {
                it.path == "docs/project-status.md" &&
                    it.rule == "prohibited project-status H2: ## Completed"
            }
        )
    }

    private fun validate(): List<DocumentationViolation> =
        AuthoritativeDocumentationValidator(tempDir).validate()

    private fun writeModel(
        registration: String,
        exception: String? = null,
    ) {
        val exceptionRows = exception?.let { "$it\n" }.orEmpty()
        write(
            "docs/templates/README.md",
            """
            # Authoritative Documentation Model

            | Type | Path or pattern | Template | Responsibility | Must not own | Max lines |
            | --- | --- | --- | --- | --- | ---: |
            $registration

            ### Registered exceptions

            | Path | Max lines | Reason | Removal condition |
            | --- | ---: | --- | --- |
            $exceptionRows
            """.trimIndent() + "\n",
        )
    }

    private fun registration(type: String, path: String, template: String, maxLines: Int): String =
        "| $type | `$path` | `$template` | test responsibility | test exclusion | $maxLines |"

    private fun exception(path: String, maxLines: Int, reason: String, removal: String): String =
        "| `$path` | $maxLines | $reason | $removal |"

    private fun writeTemplate(name: String, vararg h2: String) {
        write(
            "docs/templates/$name",
            buildString {
                append("# Template\n\n")
                h2.forEach { heading -> append(heading).append("\n\n") }
            },
        )
    }

    private fun write(relative: String, content: String) {
        val path = tempDir.resolve(relative)
        Files.createDirectories(path.parent)
        Files.writeString(path, content, StandardCharsets.UTF_8)
    }
}
