workspace "Composable Domain Platform" "Authoritative architecture model for the Composable Domain Platform." {

    model {
        stakeholder = person "Stakeholder" "Shapes platform requirements and validates business outcomes."
        platform = softwareSystem "Composable Domain Platform" "A modular application platform for independently bounded business capabilities."

        core = element "Platform Core" "Gradle project" "Business-neutral Correlation ID and execution-context primitives." "Current,Core"
        eventHttpContract = element "Event HTTP Contract" "OpenAPI contract" "Authoritative versioned contract for defining and retrieving Event state over HTTP." "Current,Contract"
        httpInterface = element "HTTP Interface" "Gradle project" "Inbound Spring Web adapter generated from versioned HTTP contracts and mapped to public application contracts." "Current,Interface"
        platformApp = element "Platform Application" "Spring Boot application" "Executable composition root that wires HTTP, capability implementations, PostgreSQL configuration, and owned startup migrations." "Current,Runtime"
        eventApi = element "Event API" "Gradle project" "Public application-level contract for defining and retrieving Event state with explicit execution context." "Current,Event Module,API"
        eventImpl = element "Event Implementation" "Gradle project" "Private Event domain, application, and persistence-adapter implementation." "Current,Event Module,Implementation"
        eventPersistence = element "Event Persistence" "PostgreSQL schema" "Event-owned durable state defined by Flyway migrations and accessed only through the private Event persistence adapter." "Current,Event Module,Persistence"

        registrationHttpContract = element "Registration HTTP Contract" "OpenAPI contract" "Planned authoritative contract for registering participation and retrieving Registration state." "Planned,Contract"
        registrationApi = element "Registration API" "Gradle project" "Planned public application contract for registering and retrieving Registration state." "Planned,Registration Module,API"
        registrationImpl = element "Registration Implementation" "Gradle project" "Planned private Registration domain, application, and persistence-adapter implementation." "Planned,Registration Module,Implementation"
        registrationPersistence = element "Registration Persistence" "PostgreSQL schema" "Planned Registration-owned durable state with no direct Event persistence coupling." "Planned,Registration Module,Persistence"
        eventRegistrationComposition = element "Event-Registration Composition" "Gradle project" "Planned cross-capability workflow that verifies Event existence through Event API before invoking Registration API." "Planned,Composition"

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

        registrationHttpContract -> httpInterface "Will generate Registration transport interface and models"
        httpInterface -> eventRegistrationComposition "Will call the registration workflow"
        httpInterface -> registrationApi "Will call Registration retrieval"
        registrationApi -> core "Will carry execution context"
        registrationImpl -> registrationApi "Will implement and depend on"
        registrationImpl -> registrationPersistence "Will persist and retrieve Registration state"
        eventRegistrationComposition -> core "Will carry execution context"
        eventRegistrationComposition -> eventApi "Will resolve Event existence"
        eventRegistrationComposition -> registrationApi "Will register participation"
        platformApp -> eventRegistrationComposition "Will wire the cross-capability workflow"
        platformApp -> registrationImpl "Will construct private Registration implementation"
        platformApp -> registrationPersistence "Will configure DataSource and apply Registration-owned Flyway migrations"
    }

    views {
        systemContext platform "CurrentSystemContext" {
            include stakeholder core eventHttpContract httpInterface platformApp eventApi eventImpl eventPersistence
            autolayout lr
        }

        custom "CurrentModuleMap" "Current module map" "Implemented runtime, contract, Gradle, and persistence boundaries for the Event reference slice." {
            include core eventHttpContract httpInterface platformApp eventApi eventImpl eventPersistence
            autolayout lr
        }

        custom "PlannedRegistrationComposition" "Planned Registration composition" "Accepted planned Registration capability and cross-capability composition; not yet implemented." {
            include core eventHttpContract registrationHttpContract httpInterface platformApp eventApi eventImpl eventPersistence registrationApi registrationImpl registrationPersistence eventRegistrationComposition
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
