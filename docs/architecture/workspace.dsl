workspace "Composable Domain Platform" "Authoritative architecture model for the Composable Domain Platform." {

    model {
        stakeholder = person "Stakeholder" "Shapes platform requirements and validates business outcomes."
        platform = softwareSystem "Composable Domain Platform" "A modular application platform for independently bounded business capabilities."

        core = element "Platform Core" "Gradle project" "Business-neutral Correlation ID and execution-context primitives." "Current,Core"
        eventHttpContract = element "Event HTTP Contract" "OpenAPI contract" "Authoritative versioned contract for defining and retrieving Event state over HTTP." "Current,Contract"
        httpInterface = element "HTTP Interface" "Gradle project" "Inbound Spring Web adapter generated from the Event HTTP contract and mapped to Event public application contracts." "Current,Interface"
        platformApp = element "Platform Application" "Spring Boot application" "Executable composition root that wires HTTP, Event implementation, PostgreSQL configuration, and Event startup migration." "Current,Runtime"
        eventApi = element "Event API" "Gradle project" "Public application-level contract for defining and retrieving Event state with explicit execution context." "Current,Event Module,API"
        eventImpl = element "Event Implementation" "Gradle project" "Private Event domain, application, and persistence-adapter implementation." "Current,Event Module,Implementation"
        eventPersistence = element "Event Persistence" "PostgreSQL schema" "Event-owned durable state defined by Flyway migrations and accessed only through the private Event persistence adapter." "Current,Event Module,Persistence"

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
    }

    views {
        systemContext platform "CurrentSystemContext" {
            include *
            autolayout lr
        }

        custom "CurrentModuleMap" "Current module map" "Implemented runtime, contract, Gradle, and persistence boundaries for the Event reference slice." {
            include core eventHttpContract httpInterface platformApp eventApi eventImpl eventPersistence
            autolayout lr
        }

        styles {
            element "Person" {
                shape person
            }

            element "Event Module" {
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
