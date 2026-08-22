package composable.domain.platform.buildlogic.docs

import java.nio.charset.CharacterCodingException
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

internal data class DocumentationViolation(val path: String, val rule: String) {
    override fun toString(): String = "$path: $rule"
}

internal data class DocumentationRegistration(
    val pathPattern: String,
    val template: String,
    val maxLines: Int,
)

internal data class DocumentationException(
    val path: String,
    val maxLines: Int,
    val reason: String,
    val removalCondition: String,
)

internal class AuthoritativeDocumentationValidator(private val repositoryRoot: Path) {
    private val modelPath = repositoryRoot.resolve("docs/templates/README.md")

    fun validate(): List<DocumentationViolation> {
        val model =
            try {
                readUtf8(modelPath)
            } catch (failure: Exception) {
                return listOf(
                    DocumentationViolation(
                        "docs/templates/README.md",
                        "cannot read documentation model: ${failure.message}",
                    )
                )
            }

        val registrations =
            try {
                parseRegistrations(model)
            } catch (failure: IllegalArgumentException) {
                return listOf(
                    DocumentationViolation(
                        "docs/templates/README.md",
                        "invalid registration table: ${failure.message}",
                    )
                )
            }

        val exceptions =
            try {
                parseExceptions(model)
            } catch (failure: IllegalArgumentException) {
                return listOf(
                    DocumentationViolation(
                        "docs/templates/README.md",
                        "invalid registered exceptions: ${failure.message}",
                    )
                )
            }

        val violations = mutableListOf<DocumentationViolation>()
        val matchedDocuments = mutableSetOf<String>()

        registrations.forEach { registration ->
            val documents = matchedPaths(registration.pathPattern)
            if (!containsGlob(registration.pathPattern) && documents.isEmpty()) {
                violations +=
                    DocumentationViolation(
                        registration.pathPattern,
                        "registered document is missing",
                    )
                return@forEach
            }

            val templatePath = repositoryRoot.resolve("docs/templates").resolve(registration.template)
            if (!Files.isRegularFile(templatePath)) {
                violations +=
                    DocumentationViolation(
                        repositoryRelative(templatePath),
                        "registered template is missing for ${registration.pathPattern}",
                    )
                return@forEach
            }

            val expectedHeadings =
                try {
                    requiredTemplateHeadings(templatePath)
                } catch (failure: IllegalArgumentException) {
                    violations +=
                        DocumentationViolation(
                            repositoryRelative(templatePath),
                            "invalid template: ${failure.message}",
                        )
                    return@forEach
                }

            documents.forEach { document ->
                val relative = repositoryRelative(document)
                matchedDocuments += relative
                violations +=
                    validateDocument(
                        document,
                        relative,
                        expectedHeadings,
                        effectiveLineLimit(relative, registration.maxLines, exceptions),
                    )
            }
        }

        exceptions.values.forEach { exception ->
            if (exception.path !in matchedDocuments) {
                violations +=
                    DocumentationViolation(
                        exception.path,
                        "registered exception does not target a registered document",
                    )
            }
        }

        return violations.sortedWith(compareBy(DocumentationViolation::path, DocumentationViolation::rule))
    }

    private fun validateDocument(
        document: Path,
        relative: String,
        expectedHeadings: List<String>,
        maxLines: Int,
    ): List<DocumentationViolation> {
        val violations = mutableListOf<DocumentationViolation>()
        val bytes = Files.readAllBytes(document)

        if (bytes.isEmpty() || bytes.last() != '\n'.code.toByte()) {
            violations += DocumentationViolation(relative, "missing final newline")
        }

        val lineCount = physicalLineCount(bytes)
        if (lineCount > maxLines) {
            violations +=
                DocumentationViolation(
                    relative,
                    "line budget exceeded: $lineCount > $maxLines",
                )
        }

        val content =
            try {
                StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
            } catch (failure: CharacterCodingException) {
                violations += DocumentationViolation(relative, "content is not valid UTF-8")
                return violations
            }

        val lines = content.split('\n')
        val h1 = lines.filter { it.startsWith("# ") }
        if (h1.size != 1) {
            violations += DocumentationViolation(relative, "expected exactly one H1, found ${h1.size}")
        }

        val h2 = lines.filter { it.startsWith("## ") }
        if (h2 != expectedHeadings) {
            violations +=
                DocumentationViolation(
                    relative,
                    "H2 structure mismatch: expected ${expectedHeadings.joinToString(" | ")}; actual ${h2.joinToString(" | ")}",
                )
        }

        if (relative == "docs/project-status.md") {
            h2.filter { it.removePrefix("## ") in PROHIBITED_PROJECT_STATUS_H2 }
                .forEach { heading ->
                    violations +=
                        DocumentationViolation(
                            relative,
                            "prohibited project-status H2: $heading",
                        )
                }
        }

        return violations
    }

    private fun requiredTemplateHeadings(templatePath: Path): List<String> {
        val lines = readUtf8(templatePath).split('\n')
        val h1 = lines.filter { it.startsWith("# ") }
        require(h1.size == 1) { "expected exactly one H1, found ${h1.size}" }

        val h2 = lines.filter { it.startsWith("## ") }
        require(h2.isNotEmpty()) { "expected at least one H2" }
        require(h2.size == h2.distinct().size) { "duplicate required H2 heading" }
        return h2
    }

    private fun effectiveLineLimit(
        relative: String,
        registeredLimit: Int,
        exceptions: Map<String, DocumentationException>,
    ): Int = exceptions[relative]?.maxLines ?: registeredLimit

    private fun matchedPaths(pattern: String): List<Path> {
        if (!containsGlob(pattern)) {
            val path = repositoryRoot.resolve(pattern)
            return if (Files.isRegularFile(path)) listOf(path) else emptyList()
        }

        val searchRoot = repositoryRoot.resolve(staticDirectoryPrefix(pattern))
        if (!Files.isDirectory(searchRoot)) {
            return emptyList()
        }

        val matcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")
        return Files.walk(searchRoot).use { paths ->
            paths
                .filter { Files.isRegularFile(it) }
                .filter { matcher.matches(repositoryRoot.relativize(it)) }
                .sorted()
                .toList()
        }
    }

    private fun parseRegistrations(content: String): List<DocumentationRegistration> {
        val rows = tableRows(content, REGISTRATION_HEADER)
        require(rows.isNotEmpty()) { "no registered document rows found" }

        return rows.map { cells ->
            require(cells.size == 6) { "registration row must have 6 columns" }
            val path = unquote(cells[1])
            val template = unquote(cells[2])
            val maxLines = cells[5].toIntOrNull()
                ?: throw IllegalArgumentException("invalid max lines for $path: ${cells[5]}")
            require(path.isNotBlank()) { "registered path must not be blank" }
            require(template.isNotBlank()) { "registered template must not be blank for $path" }
            require(maxLines > 0) { "max lines must be positive for $path" }
            DocumentationRegistration(path, template, maxLines)
        }
    }

    private fun parseExceptions(content: String): Map<String, DocumentationException> {
        val rows = tableRows(content, EXCEPTION_HEADER, required = false)
        val exceptions = linkedMapOf<String, DocumentationException>()

        rows.forEach { cells ->
            require(cells.size == 4) { "exception row must have 4 columns" }
            val path = unquote(cells[0])
            val maxLines = cells[1].toIntOrNull()
                ?: throw IllegalArgumentException("invalid exception max lines for $path: ${cells[1]}")
            val reason = cells[2]
            val removal = cells[3]
            require(path.isNotBlank()) { "exception path must not be blank" }
            require(!containsGlob(path)) { "exception path must be exact: $path" }
            require(maxLines > 0) { "exception max lines must be positive for $path" }
            require(reason.isNotBlank()) { "exception reason must not be blank for $path" }
            require(removal.isNotBlank()) { "exception removal condition must not be blank for $path" }
            require(exceptions.put(path, DocumentationException(path, maxLines, reason, removal)) == null) {
                "duplicate exception for $path"
            }
        }

        return exceptions
    }

    private fun tableRows(
        content: String,
        header: List<String>,
        required: Boolean = true,
    ): List<List<String>> {
        val lines = content.split('\n')
        val headerIndex = lines.indexOfFirst { markdownCells(it) == header }
        if (headerIndex < 0) {
            if (required) {
                throw IllegalArgumentException("missing table header: ${header.joinToString(" | ")}")
            }
            return emptyList()
        }

        require(headerIndex + 1 < lines.size && isSeparatorRow(markdownCells(lines[headerIndex + 1]))) {
            "missing separator row after ${header.joinToString(" | ")}"
        }

        return lines
            .drop(headerIndex + 2)
            .takeWhile { it.trimStart().startsWith("|") }
            .map(::markdownCells)
            .filter { it.isNotEmpty() }
    }

    private fun markdownCells(line: String): List<String> {
        val trimmed = line.trim()
        if (!trimmed.startsWith("|") || !trimmed.endsWith("|")) {
            return emptyList()
        }
        return trimmed.removePrefix("|").removeSuffix("|").split('|').map(String::trim)
    }

    private fun isSeparatorRow(cells: List<String>): Boolean =
        cells.isNotEmpty() && cells.all { cell -> cell.matches(Regex(":?-{3,}:?")) }

    private fun unquote(value: String): String =
        value.removePrefix("`").removeSuffix("`")

    private fun repositoryRelative(path: Path): String =
        repositoryRoot.relativize(path).toString().replace('\\', '/')

    private fun readUtf8(path: Path): String = Files.readString(path, StandardCharsets.UTF_8)

    private fun physicalLineCount(bytes: ByteArray): Int {
        if (bytes.isEmpty()) {
            return 0
        }
        val newlineCount = bytes.count { it == '\n'.code.toByte() }
        return newlineCount + if (bytes.last() == '\n'.code.toByte()) 0 else 1
    }

    private fun containsGlob(pattern: String): Boolean = pattern.any { it == '*' || it == '?' || it == '[' }

    private fun staticDirectoryPrefix(pattern: String): String {
        val wildcardIndex = pattern.indexOfFirst { it == '*' || it == '?' || it == '[' }
        val prefix = if (wildcardIndex < 0) pattern else pattern.substring(0, wildcardIndex)
        val slash = prefix.lastIndexOf('/')
        return if (slash < 0) "." else prefix.substring(0, slash)
    }

    companion object {
        private val REGISTRATION_HEADER =
            listOf("Type", "Path or pattern", "Template", "Responsibility", "Must not own", "Max lines")
        private val EXCEPTION_HEADER = listOf("Path", "Max lines", "Reason", "Removal condition")
        private val PROHIBITED_PROJECT_STATUS_H2 =
            setOf("Completed", "History", "Changelog", "Previous phases")
    }
}

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: authoritative-documentation-validator <repository-root>")
        exitProcess(2)
    }

    val violations = AuthoritativeDocumentationValidator(Path.of(args.single())).validate()
    if (violations.isNotEmpty()) {
        violations.forEach { violation -> System.err.println("FAIL: $violation") }
        exitProcess(1)
    }

    println("PASS: authoritative documentation validation")
}
