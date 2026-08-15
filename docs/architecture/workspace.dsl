workspace "Composable Domain Platform" "Authoritative architecture model for the Composable Domain Platform." {

    model {
        stakeholder = person "Stakeholder" "Shapes platform requirements and validates business outcomes."
        platform = softwareSystem "Composable Domain Platform" "A modular application platform for independently bounded business capabilities."

        core = element "Platform Core" "Gradle project" "Business-neutral Correlation ID and execution-context primitives." "Current,Core"
        eventHttpContract = element "Event-facing HTTP Contract" "OpenAPI contract" "Authoritative versioned Event-facing contract for current Event and Event-registration workflow operations." "Current,Contract"
        httpInterface = element "HTTP Interface" "Gradle project" "Inbound Spring Web adapter generated from versioned HTTP contracts and mapped to public application contracts." "Current,Interface"
        platformApp = element "Platform Application" "Spring Boot application" "Executable composition root that wires HTTP, capability implementations, PostgreSQL configuration, and owned startup migrations." "Current,Runtime"
        eventApi = element "Event API" "Gradle project" "Public application-level contract for defining and retrieving Event state with explicit execution context." "Current,Event Module,API"
        eventImpl = element "Event Implementation" "Gradle project" "Private Event domain, application, and persistence-adapter implementation." "Current,Event Module,Implementation"
        eventPersistence = element "Event Persistence" "PostgreSQL schema" "Event-owned durable state defined by Flyway migrations and accessed only through the private Event persistence adapter." "Current,Event Module,Persistence"

        registrationApi = element "Registration API" "Gradle project" "Domain-neutral public application contract for registering and retrieving namespaced opaque registrant-to-target relations." "Current,Registration Module,API"
        registrationImpl = element "Registration Implementation" "Gradle project" "Private domain-neutral Registration domain, application, and persistence-adapter implementation." "Current,Registration Module,Implementation"
        registrationPersistence = element "Registration Persistence" "PostgreSQL schema" "Registration-owned durable namespaced registrant-to-target state with no Event-specific columns or cross-capability persistence coupling." "Current,Registration Module,Persistence"
        eventRegistrationComposition = element "Event-Registration Composition" "Gradle project" "Event-specific workflow that verifies Event existence and translates Event workflow identities into Registration references through public APIs." "Current,Composition"

        securityApi = element "Security API" "Gradle project" "Framework-neutral authenticated-actor Authentication boundary plus opaque resource-ownership Authorization decision selected by decision #99 and recorded by ADR-0014." "Current,Security Module,API"
        securityImpl = element "Security Implementation" "Gradle project" "Private Security implementation and adapters: Spring Security/stateless HTTP Basic, encoded verifier validation, principal-to-actor adaptation, and ownership authorization." "Current,Security Module,Implementation"

        stakeholder -> platform "Uses and shapes"
        eventHttpContract -> httpInterface "Generates server transport interface and models"
        httpInterface -> core "Establishes and propagates correlation context"
        httpInterface -> eventApi "Calls define and retrieve contracts"
        eventApi -> core "Carries execution context"
        eventImpl -> eventApi "Implements and depends on"
        eventImpl -> eventPersistence "Persists and retrieves Event state"
        platformApp -> httpInterface "Hosts and wires"
        platformApp -> eventImpl "Constructs private Event implementation"
        platformApp -> eventPersistence "Configures DataSource and applies Event-owned Flyway migrations"

        httpInterface -> eventRegistrationComposition "Calls Event-registration create and retrieval workflows"
        registrationApi -> core "Carries execution context"
        registrationImpl -> registrationApi "Implements and depends on"
        registrationImpl -> registrationPersistence "Persists and retrieves Registration state"
        eventRegistrationComposition -> core "Carries execution context"
        eventRegistrationComposition -> eventApi "Resolves Event existence"
        eventRegistrationComposition -> registrationApi "Creates and retrieves domain-neutral Registration state"
        platformApp -> eventRegistrationComposition "Wires the cross-capability workflow"
        platformApp -> registrationImpl "Constructs private Registration implementation"
        platformApp -> registrationPersistence "Configures DataSource and applies Registration-owned Flyway migrations"

        securityImpl -> securityApi "Implements the Security public boundary"
        httpInterface -> securityApi "Consumes authenticated-actor Authentication boundary"
        eventRegistrationComposition -> securityApi "Requests opaque resource-ownership Authorization decisions"
        platformApp -> securityImpl "Selects and configures private Security implementation"
        platformApp -> securityApi "Wires Security public contracts to consumers"
    }

    views {
        systemContext platform "CurrentSystemContext" {
            include stakeholder core eventHttpContract httpInterface platformApp eventApi eventImpl eventPersistence registrationApi registrationImpl registrationPersistence eventRegistrationComposition securityApi securityImpl
            autolayout lr
        }

        custom "CurrentModuleMap" "Current module map" "Implemented runtime, contract, capability, composition, and persistence boundaries for Event and Registration." {
            include core eventHttpContract httpInterface platformApp eventApi eventImpl eventPersistence registrationApi registrationImpl registrationPersistence eventRegistrationComposition securityApi securityImpl
            autolayout lr
        }

        custom "RegistrationComposition" "Registration composition" "Implemented Registration capability and Event-Registration cross-capability composition." {
            include core eventHttpContract httpInterface platformApp eventApi eventImpl eventPersistence registrationApi registrationImpl registrationPersistence eventRegistrationComposition securityApi securityImpl
            autolayout lr
        }

        custom "SecurityBoundary" "Security module boundary" "Current Security API/Implementation and high-level consumers established by #97/#99/ADR-0014 and implementation #102." {
            include httpInterface platformApp eventRegistrationComposition registrationApi securityApi securityImpl
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
