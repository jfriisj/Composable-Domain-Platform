package composable.domain.platform.registration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class RegistrationArchitectureTest {

    private static final String ROOT = "composable.domain.platform";
    private static final String REGISTRATION_ROOT = ROOT + ".registration";
    private static final String CORE_EXECUTION_PACKAGE = ROOT + ".core.execution..";
    private static final String API_PACKAGE = REGISTRATION_ROOT + ".api..";
    private static final String APPLICATION_PACKAGE = REGISTRATION_ROOT + ".application..";
    private static final String DOMAIN_PACKAGE = REGISTRATION_ROOT + ".domain..";
    private static final String PERSISTENCE_PACKAGE = REGISTRATION_ROOT + ".persistence..";

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(REGISTRATION_ROOT);

    @Test
    void public_api_may_only_depend_on_api_core_execution_and_java_platform() {
        classes()
                .that().resideInAPackage(API_PACKAGE)
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        API_PACKAGE,
                        CORE_EXECUTION_PACKAGE,
                        "java..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void domain_may_only_depend_on_domain_and_java_platform() {
        classes()
                .that().resideInAPackage(DOMAIN_PACKAGE)
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        DOMAIN_PACKAGE,
                        "java..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void application_may_only_depend_on_registration_layers_core_execution_and_java_platform() {
        classes()
                .that().resideInAPackage(APPLICATION_PACKAGE)
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        APPLICATION_PACKAGE,
                        DOMAIN_PACKAGE,
                        API_PACKAGE,
                        CORE_EXECUTION_PACKAGE,
                        "java..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void persistence_may_only_depend_on_application_domain_and_database_implementation() {
        classes()
                .that().resideInAPackage(PERSISTENCE_PACKAGE)
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        PERSISTENCE_PACKAGE,
                        APPLICATION_PACKAGE,
                        DOMAIN_PACKAGE,
                        "java..",
                        "javax.sql..",
                        "org.jooq..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void persistence_adapter_must_not_be_public() {
        classes()
                .that().resideInAPackage(PERSISTENCE_PACKAGE)
                .should().notBePublic()
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void registration_production_must_not_depend_on_non_owned_capabilities_or_frameworks() {
        noClasses()
                .that().resideInAPackage(REGISTRATION_ROOT + "..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".event..",
                        ROOT + ".person..",
                        ROOT + ".http..",
                        ROOT + ".app..",
                        "jakarta..",
                        "org.springframework..",
                        "org.openapitools..")
                .check(PRODUCTION_CLASSES);
    }
}
