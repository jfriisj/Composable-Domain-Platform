workspace "Composable Domain Platform" "Authoritative architecture model for the Composable Domain Platform." {

    model {
        stakeholder = person "Stakeholder" "Shapes platform requirements and validates business outcomes."
        platform = softwareSystem "Composable Domain Platform" "A modular application platform for independently bounded business capabilities."

        core = element "Platform Core" "Gradle project" "Business-neutral Correlation ID and execution-context primitives." "Current,Core"
        eventHttpContract = element "Event HTTP Contract Unit" "OpenAPI source contract" "Independently authoritative source for Event-owned externally addressable HTTP behavior." "Current,Contract"
        eventRegistrationHttpContract = element "Event-Registration HTTP Contract Unit" "OpenAPI source contract" "Independently authoritative source for participant Event-registration and organizer Event-registration view workflows owned by the non-module composition." "Current,Contract"
        eventApplicationHttpContract = element "Event Application HTTP Contract" "Aggregated OpenAPI contract" "Derived coherent Event-only application contract assembled statically from the selected Event contract unit." "Current,Contract"
        platformApplicationHttpContract = element "Platform Application HTTP Contract" "Aggregated OpenAPI contract" "Derived coherent full application contract assembled statically from selected Event and Event-Registration contract units." "Current,Contract"
        httpInterface = element "HTTP Interface" "Gradle project" "Inbound Spring Web adapter generated from versioned HTTP contracts and mapped to public application contracts." "Current,Interface"
        platformApp = element "Platform Application" "Spring Boot application" "Executable composition root that wires HTTP, capability implementations, PostgreSQL configuration, and owned startup migrations." "Current,Runtime"
        eventApi = element "Event API" "Gradle project" "Public application-level contract for defining, updating, publishing, withdrawing, and retrieving Event state with explicit execution context." "Current,Event Module,API"
        eventImpl = element "Event Implementation" "Gradle project" "Private Event domain, application, and persistence-adapter implementation." "Current,Event Module,Implementation"
        eventPersistence = element "Event Persistence" "PostgreSQL schema" "Event-owned durable state defined by Flyway migrations and accessed only through the private Event persistence adapter." "Current,Event Module,Persistence"

        registrationApi = element "Registration API" "Gradle project" "Domain-neutral public application contract for registering and retrieving namespaced opaque registrant-to-target relations." "Current,Registration Module,API"
        registrationImpl = element "Registration Implementation" "Gradle project" "Private domain-neutral Registration domain, application, and persistence-adapter implementation." "Current,Registration Module,Implementation"
        registrationPersistence = element "Registration Persistence" "PostgreSQL schema" "Registration-owned durable namespaced registrant-to-target state with no Event-specific columns or cross-capability persistence coupling." "Current,Registration Module,Persistence"
        eventRegistrationComposition = element "Event-Registration Composition" "Gradle project" "Event-specific workflow that verifies Event existence and lifecycle eligibility, translates Event workflow identities into Registration references, and exposes organizer Event-registration views through public APIs." "Current,Composition"
        eventManagementComposition = element "Event-Management Composition" "Gradle project" "Organizer Event-management workflow that delegates Event creation, modification, publication, and withdrawal to Event while checking owner authorization with Security." "Current,Composition"

        securityApi = element "Security API" "Gradle project" "Framework-neutral authenticated-actor Authentication boundary plus opaque resource-ownership Authorization decision selected by decision #99 and recorded by ADR-0014." "Current,Security Module,API"
        securityImpl = element "Security Implementation" "Gradle project" "Private Security implementation and adapters: Spring Security/stateless HTTP Basic, encoded verifier validation, principal-to-actor adaptation, and ownership authorization." "Current,Security Module,Implementation"
        eventRegistrationHttpInterface = element "Event-Registration HTTP Interface" "Gradle project" "Event-registration inbound adapter with independently generated workflow transport mapped to Event-Registration and Security public APIs." "Current,Interface"
        eventApp = element "Event Application" "Spring Boot application" "Event-only executable composition root selecting Event and Security without Registration or Event-Registration." "Current,Runtime"

        stakeholder -> platform "Uses and shapes"
        eventHttpContract -> httpInterface "Generates independently selectable Event transport"
        eventRegistrationHttpContract -> eventRegistrationHttpInterface "Generates independently selectable workflow transport"
        eventHttpContract -> eventApplicationHttpContract "Selected into static application aggregation"
        eventHttpContract -> platformApplicationHttpContract "Selected into static application aggregation"
        eventRegistrationHttpContract -> platformApplicationHttpContract "Selected into static application aggregation"
        httpInterface -> core "Establishes and propagates correlation context"
        httpInterface -> eventApi "Calls anonymous retrieve and discover contracts"
        httpInterface -> eventManagementComposition "Calls organizer Event-management workflows"
        httpInterface -> securityApi "Consumes authenticated-actor Authentication boundary"
        eventApi -> core "Carries execution context"
        eventImpl -> eventApi "Implements and depends on"
        eventImpl -> eventPersistence "Persists and retrieves Event state"
        eventManagementComposition -> core "Carries execution context"
        eventManagementComposition -> eventApi "Defines, updates, publishes, withdraws, and retrieves Event state"
        eventManagementComposition -> securityApi "Requests opaque resource-ownership Authorization decisions"
        platformApp -> httpInterface "Hosts and wires"
        platformApp -> eventImpl "Constructs private Event implementation"
        platformApp -> eventPersistence "Configures DataSource and applies Event-owned Flyway migrations"
        platformApp -> eventManagementComposition "Wires the organizer Event-management workflow"

        registrationApi -> core "Carries execution context"
        registrationImpl -> registrationApi "Implements and depends on"
        registrationImpl -> registrationPersistence "Persists and retrieves Registration state"
        eventRegistrationComposition -> core "Carries execution context"
        eventRegistrationComposition -> eventApi "Resolves Event existence, lifecycle eligibility, and organizer ownership"
        eventRegistrationComposition -> registrationApi "Creates and retrieves domain-neutral Registration state"
        platformApp -> eventRegistrationComposition "Wires the cross-capability workflow"
        platformApp -> registrationImpl "Constructs private Registration implementation"
        platformApp -> registrationPersistence "Configures DataSource and applies Registration-owned Flyway migrations"

        securityImpl -> securityApi "Implements the Security public boundary"
        eventRegistrationComposition -> securityApi "Requests opaque resource-ownership Authorization decisions"
        platformApp -> securityImpl "Selects and configures private Security implementation"
        platformApp -> securityApi "Wires Security public contracts to consumers"

        eventRegistrationHttpInterface -> core "Establishes and propagates correlation context"
        eventRegistrationHttpInterface -> eventRegistrationComposition "Calls participant and organizer Event-registration workflows"
        eventRegistrationHttpInterface -> securityApi "Consumes authenticated-actor Authentication boundary"
        platformApp -> eventRegistrationHttpInterface "Hosts and wires Event-registration HTTP adaptation"
        platformApp -> platformApplicationHttpContract "Declares selected external surface"
        eventApp -> eventApplicationHttpContract "Declares selected external surface"
        eventApp -> httpInterface "Hosts and wires Event HTTP adaptation"
        eventApp -> eventImpl "Constructs private Event implementation"
        eventApp -> eventPersistence "Configures DataSource and applies Event-owned Flyway migrations"
        eventApp -> eventManagementComposition "Wires the organizer Event-management workflow"
        eventApp -> securityImpl "Selects and configures private Security implementation"
        eventApp -> securityApi "Wires Security public contracts to consumers"

    }

    views {
        systemContext platform "CurrentSystemContext" {
            include stakeholder core eventHttpContract eventRegistrationHttpContract eventApplicationHttpContract platformApplicationHttpContract httpInterface eventRegistrationHttpInterface platformApp eventApp eventApi eventImpl eventPersistence registrationApi registrationImpl registrationPersistence eventRegistrationComposition eventManagementComposition securityApi securityImpl
            autolayout lr
        }

        custom "CurrentModuleMap" "Current module map" "Implemented runtime, contract, capability, composition, persistence, and selectable-application boundaries." {
            include core eventHttpContract eventRegistrationHttpContract eventApplicationHttpContract platformApplicationHttpContract httpInterface eventRegistrationHttpInterface platformApp eventApp eventApi eventImpl eventPersistence registrationApi registrationImpl registrationPersistence eventRegistrationComposition eventManagementComposition securityApi securityImpl
            autolayout lr
        }

        custom "RegistrationComposition" "Registration composition" "Implemented Registration capability and Event-Registration cross-capability composition." {
            include core eventHttpContract httpInterface eventRegistrationHttpInterface platformApp eventApi eventImpl eventPersistence registrationApi registrationImpl registrationPersistence eventRegistrationComposition securityApi securityImpl
            autolayout lr
        }

        custom "SecurityBoundary" "Security module boundary" "Current Security API/Implementation and high-level consumers established by #97/#99/ADR-0014 and implementation #102." {
            include eventRegistrationHttpInterface httpInterface platformApp eventApp eventRegistrationComposition eventManagementComposition registrationApi eventApi securityApi securityImpl
            autolayout lr
        }

        custom "SelectableComposition" "Selectable application composition" "Implemented ADR-0015 static Event-only composition alongside the complete Platform Application." {
            include core eventHttpContract eventRegistrationHttpContract eventApplicationHttpContract platformApplicationHttpContract httpInterface eventRegistrationHttpInterface platformApp eventApp eventApi eventImpl eventPersistence registrationApi registrationImpl registrationPersistence eventRegistrationComposition eventManagementComposition securityApi securityImpl
            autolayout lr
        }

        custom "ExternalContractComposition" "External contract composition" "ADR-0016 implementation: independently authoritative Event and Event-Registration contract units, static application aggregation, and independently selectable generated transport boundaries." {
            include eventHttpContract eventRegistrationHttpContract eventApplicationHttpContract platformApplicationHttpContract httpInterface eventRegistrationHttpInterface eventApp platformApp eventApi eventRegistrationComposition eventManagementComposition securityApi
            autolayout lr
        }

        styles {
            element "Person" {
                shape person
            }

            element "Event Module" {
                shape component
            }

            element "Registration Module" {
                shape component
            }

            element "Security Module" {
                shape component
            }

            element "Composition" {
                shape component
            }

            element "Interface" {
                shape hexagon
            }

            element "Runtime" {
                shape roundedbox
            }
        }
    }
}
