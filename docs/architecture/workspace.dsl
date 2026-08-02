workspace "Composable Domain Platform" "Authoritative architecture model for the Composable Domain Platform." {

    model {
        stakeholder = person "Stakeholder" "Shapes platform requirements and validates business outcomes."
        platform = softwareSystem "Composable Domain Platform" "A modular application platform for independently bounded business capabilities."

        eventApi = element "Event API" "Gradle project" "Public application-level contract for defining and retrieving Event state." "Current,Event Module,API"
        eventImpl = element "Event Implementation" "Gradle project" "Private Event domain, application, and persistence-adapter implementation." "Current,Event Module,Implementation"
        eventPersistence = element "Event Persistence" "PostgreSQL schema" "Event-owned durable state defined by Flyway migrations and accessed only through the private Event persistence adapter." "Current,Event Module,Persistence"

        stakeholder -> platform "Uses and shapes"
        eventImpl -> eventApi "Implements and depends on"
        eventImpl -> eventPersistence "Persists and retrieves Event state"
    }

    views {
        systemContext platform "CurrentSystemContext" {
            include *
            autolayout lr
        }

        custom "CurrentModuleMap" "Current module map" "Implemented Gradle and persistence boundaries for the Event reference module." {
            include eventApi eventImpl eventPersistence
            autolayout lr
        }

        styles {
            element "Person" {
                shape person
            }

            element "Event Module" {
                shape component
            }
        }
    }
}
