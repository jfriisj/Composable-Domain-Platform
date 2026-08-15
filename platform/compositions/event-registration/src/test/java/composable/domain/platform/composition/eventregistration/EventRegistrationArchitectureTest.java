package composable.domain.platform.composition.eventregistration;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class EventRegistrationArchitectureTest {

    private static final String COMPOSITION =
            "composable.domain.platform.composition.eventregistration..";

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("composable.domain.platform.composition.eventregistration");

    @Test
    void composition_may_only_depend_on_public_capability_contracts_core_and_java_platform() {
        classes()
                .that().resideInAPackage(COMPOSITION)
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        COMPOSITION,
                        "composable.domain.platform.core.execution..",
                        "composable.domain.platform.event.api..",
                        "composable.domain.platform.registration.api..",
                        "composable.domain.platform.security.api..",
                        "java..")
                .check(PRODUCTION_CLASSES);
    }
}
