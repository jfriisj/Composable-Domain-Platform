package composable.domain.platform.app;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class PlatformArchitectureTest {

    private static final String ROOT = "composable.domain.platform";
    private static final String APP_PACKAGE = ROOT + ".app..";
    private static final String CORE_PACKAGE = ROOT + ".core..";
    private static final String EVENT_API_PACKAGE = ROOT + ".event.api..";
    private static final String EVENT_APPLICATION_PACKAGE = ROOT + ".event.application..";
    private static final String EVENT_DOMAIN_PACKAGE = ROOT + ".event.domain..";
    private static final String EVENT_PERSISTENCE_PACKAGE = ROOT + ".event.persistence..";
    private static final String REGISTRATION_API_PACKAGE = ROOT + ".registration.api..";
    private static final String REGISTRATION_APPLICATION_PACKAGE =
            ROOT + ".registration.application..";
    private static final String REGISTRATION_DOMAIN_PACKAGE =
            ROOT + ".registration.domain..";
    private static final String REGISTRATION_PERSISTENCE_PACKAGE =
            ROOT + ".registration.persistence..";
    private static final String EVENT_REGISTRATION_COMPOSITION_PACKAGE =
            ROOT + ".composition.eventregistration..";
    private static final String HTTP_PACKAGE = ROOT + ".http..";

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void core_may_only_depend_on_core_and_java_platform() {
        classes()
                .that().resideInAPackage(CORE_PACKAGE)
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        CORE_PACKAGE,
                        "java..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void event_api_may_only_depend_on_event_api_core_and_java_platform() {
        classes()
                .that().resideInAPackage(EVENT_API_PACKAGE)
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        EVENT_API_PACKAGE,
                        CORE_PACKAGE,
                        "java..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void http_interface_must_not_depend_on_capability_implementations_or_database_infrastructure() {
        noClasses()
                .that().resideInAPackage(HTTP_PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage(
                        EVENT_APPLICATION_PACKAGE,
                        EVENT_DOMAIN_PACKAGE,
                        EVENT_PERSISTENCE_PACKAGE,
                        REGISTRATION_APPLICATION_PACKAGE,
                        REGISTRATION_DOMAIN_PACKAGE,
                        REGISTRATION_PERSISTENCE_PACKAGE,
                        "org.flywaydb..",
                        "org.jooq..",
                        "org.postgresql..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void capability_domain_and_application_must_not_depend_on_http_or_runtime_frameworks() {
        noClasses()
                .that().resideInAnyPackage(
                        EVENT_DOMAIN_PACKAGE,
                        EVENT_APPLICATION_PACKAGE,
                        REGISTRATION_DOMAIN_PACKAGE,
                        REGISTRATION_APPLICATION_PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage(
                        HTTP_PACKAGE,
                        APP_PACKAGE,
                        "jakarta..",
                        "org.springframework..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void spring_security_must_remain_confined_to_application_runtime() {
        noClasses()
                .that().resideInAnyPackage(
                        CORE_PACKAGE,
                        EVENT_API_PACKAGE,
                        EVENT_APPLICATION_PACKAGE,
                        EVENT_DOMAIN_PACKAGE,
                        EVENT_PERSISTENCE_PACKAGE,
                        REGISTRATION_API_PACKAGE,
                        REGISTRATION_APPLICATION_PACKAGE,
                        REGISTRATION_DOMAIN_PACKAGE,
                        REGISTRATION_PERSISTENCE_PACKAGE,
                        EVENT_REGISTRATION_COMPOSITION_PACKAGE,
                        HTTP_PACKAGE)
                .should().dependOnClassesThat().resideInAPackage(
                        "org.springframework.security..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void application_runtime_may_only_depend_on_current_composition_packages() {
        classes()
                .that().resideInAPackage(APP_PACKAGE)
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        APP_PACKAGE,
                        CORE_PACKAGE,
                        EVENT_API_PACKAGE,
                        EVENT_APPLICATION_PACKAGE,
                        EVENT_PERSISTENCE_PACKAGE,
                        REGISTRATION_API_PACKAGE,
                        REGISTRATION_APPLICATION_PACKAGE,
                        REGISTRATION_PERSISTENCE_PACKAGE,
                        EVENT_REGISTRATION_COMPOSITION_PACKAGE,
                        HTTP_PACKAGE,
                        "java..",
                        "javax.sql..",
                        "org.flywaydb..",
                        "org.postgresql..",
                        "org.springframework..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void application_runtime_must_not_depend_on_capability_domains() {
        noClasses()
                .that().resideInAPackage(APP_PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage(
                        EVENT_DOMAIN_PACKAGE,
                        REGISTRATION_DOMAIN_PACKAGE)
                .check(PRODUCTION_CLASSES);
    }
}
