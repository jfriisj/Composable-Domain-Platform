package composable.domain.platform.waitlist.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class WaitlistArchitectureTest {

    private static final String ROOT = "composable.domain.platform";
    private static final String WAITLIST_ROOT = ROOT + ".waitlist";
    private static final String CORE_EXECUTION_PACKAGE =
            ROOT + ".core.execution..";
    private static final String API_PACKAGE = WAITLIST_ROOT + ".api..";
    private static final String APPLICATION_PACKAGE =
            WAITLIST_ROOT + ".application..";
    private static final String DOMAIN_PACKAGE =
            WAITLIST_ROOT + ".domain..";
    private static final String PERSISTENCE_PACKAGE =
            WAITLIST_ROOT + ".persistence..";

    private static final JavaClasses PRODUCTION_CLASSES =
            new ClassFileImporter()
                    .withImportOption(
                            ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages(WAITLIST_ROOT);

    @Test
    void publicApiMayOnlyDependOnApiCoreExecutionAndJavaPlatform() {
        classes()
                .that().resideInAPackage(API_PACKAGE)
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        API_PACKAGE,
                        CORE_EXECUTION_PACKAGE,
                        "java..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void domainMayOnlyDependOnDomainAndJavaPlatform() {
        classes()
                .that().resideInAPackage(DOMAIN_PACKAGE)
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        DOMAIN_PACKAGE,
                        "java..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void applicationMayOnlyDependOnOwnedLayersCoreAndJavaPlatform() {
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
    void persistenceMayOnlyDependOnOwnedLayersAndDatabaseImplementation() {
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
    void jooqPersistenceAdapterMustNotBePublic() {
        classes()
                .that().resideInAPackage(PERSISTENCE_PACKAGE)
                .and().haveSimpleName(
                        "JooqWaitlistParticipationRepository")
                .should().notBePublic()
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void waitlistMustNotDependOnOtherCapabilitiesOrFrameworks() {
        noClasses()
                .that().resideInAPackage(WAITLIST_ROOT + "..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".event..",
                        ROOT + ".registration..",
                        ROOT + ".security..",
                        ROOT + ".http..",
                        ROOT + ".app..",
                        "jakarta..",
                        "org.springframework..",
                        "org.openapitools..")
                .check(PRODUCTION_CLASSES);
    }
}
