workspace "Composable Domain Platform" "Authoritative architecture model for the Composable Domain Platform." {

    model {
        stakeholder = person "Stakeholder" "Shapes platform requirements and validates business outcomes."
        platform = softwareSystem "Composable Domain Platform" "A modular application platform for independently bounded business capabilities."

        core = element "Platform Core" "Gradle project" "Business-neutral Correlation ID and execution-context primitives." "Current,Core"
        eventHttpContract = element "Event-facing HTTP Contract" "OpenAPI contract" "Authoritative versioned Event-facing contract for current Event operations and accepted planned Event-registration workflow operations." "Current,Contract"
        httpInterface = element "HTTP Interface" "Gradle project" "Inbound Spring Web adapter generated from versioned HTTP contracts and mapped to public application contracts." "Current,Interface"
        platformApp = element "Platform Application" "Spring Boot application" "Executable composition root that wires HTTP, capability implementations, PostgreSQL configuration, and owned startup migrations." "Current,Runtime"
        eventApi = element "Event API" "Gradle project" "Public application-level contract for defining and retrieving Event state with explicit execution context." "Current,Event Module,API"
        eventImpl = element "Event Implementation" "Gradle project" "Private Event domain, application, and persistence-adapter implementation." "Current,Event Module,Implementation"
        eventPersistence = element "Event Persistence" "PostgreSQL schema" "Event-owned durable state defined by Flyway migrations and accessed only through the private Event persistence adapter." "Current,Event Module,Persistence"

        registrationApi = element "Registration API" "Gradle project" "Domain-neutral public application contract for registering and retrieving namespaced opaque registrant-to-target relations." "Current,Registration Module,API"
        registrationImpl = element "Registration Implementation" "Gradle project" "Private domain-neutral Registration domain, application, and persistence-adapter implementation." "Current,Registration Module,Implementation"
        registrationPersistence = element "Registration Persistence" "PostgreSQL schema" "Registration-owned durable namespaced registrant-to-target state with no Event-specific columns or cross-capability persistence coupling." "Current,Registration Module,Persistence"
        eventRegistrationComposition = element "Event-Registration Composition" "Gradle project" "Planned Event-specific workflow that verifies Event existence and translates Event workflow identities into Registration references through public APIs." "Planned,Composition"

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

        httpInterface -> eventRegistrationComposition "Will call Event-registration create and retrieval workflows"
        registrationApi -> core "Will carry execution context"
        registrationImpl -> registrationApi "Will implement and depend on"
        registrationImpl -> registrationPersistence "Will persist and retrieve Registration state"
        eventRegistrationComposition -> core "Will carry execution context"
        eventRegistrationComposition -> eventApi "Will resolve Event existence"
        eventRegistrationComposition -> registrationApi "Will create and retrieve domain-neutral Registration state"
        platformApp -> eventRegistrationComposition "Will wire the cross-capability workflow"
        platformApp -> registrationImpl "Will construct private Registration implementation"
        platformApp -> registrationPersistence "Will configure DataSource and apply Registration-owned Flyway migrations"
    }

    views {
        systemContext platform "CurrentSystemContext" {
            include stakeholder core eventHttpContract httpInterface platformApp eventApi eventImpl eventPersistence registrationApi registrationImpl registrationPersistence
            autolayout lr
        }

        custom "CurrentModuleMap" "Current module map" "Implemented runtime, contract, Gradle, and persistence boundaries for Event and the domain-neutral Registration capability." {
            include core eventHttpContract httpInterface platformApp eventApi eventImpl eventPersistence registrationApi registrationImpl registrationPersistence
            autolayout lr
        }

        custom "PlannedRegistrationComposition" "Planned Registration composition" "Implemented Registration capability with accepted planned Event-Registration composition." {
            include core eventHttpContract httpInterface platformApp eventApi eventImpl eventPersistence registrationApi registrationImpl registrationPersistence eventRegistrationComposition
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
