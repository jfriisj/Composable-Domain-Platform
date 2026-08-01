package composable.domain.platform.event.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class EventArchitectureTest {

    private static final String EVENT_ROOT = "composable.domain.platform.event";
    private static final String APPLICATION_PACKAGE = EVENT_ROOT + ".application..";
    private static final String DOMAIN_PACKAGE = EVENT_ROOT + ".domain..";
    private static final String API_PACKAGE = EVENT_ROOT + ".api..";

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(EVENT_ROOT);

    @Test
    void domain_must_not_depend_on_application_implementation() {
        noClasses()
                .that().resideInAPackage(DOMAIN_PACKAGE)
                .should().dependOnClassesThat().resideInAPackage(APPLICATION_PACKAGE)
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void domain_must_not_depend_on_public_api() {
        noClasses()
                .that().resideInAPackage(DOMAIN_PACKAGE)
                .should().dependOnClassesThat().resideInAPackage(API_PACKAGE)
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void application_implementation_may_only_depend_on_current_allowed_packages() {
        classes()
                .that().resideInAPackage(APPLICATION_PACKAGE)
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        APPLICATION_PACKAGE,
                        DOMAIN_PACKAGE,
                        API_PACKAGE,
                        "java..")
                .check(PRODUCTION_CLASSES);
    }
}
