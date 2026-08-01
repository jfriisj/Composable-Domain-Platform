workspace "Composable Domain Platform" "Authoritative architecture model for the Composable Domain Platform." {

    model {
        stakeholder = person "Stakeholder" "Shapes platform requirements and validates business outcomes."
        platform = softwareSystem "Composable Domain Platform" "A modular application platform for independently bounded business capabilities."

        eventApi = element "Event API" "Gradle project" "Public application-level contract for defining an Event and returning its state." "Current,Event Module,API"
        eventImpl = element "Event Implementation" "Gradle project" "Private Event domain and application implementation." "Current,Event Module,Implementation"

        stakeholder -> platform "Uses and shapes"
        eventImpl -> eventApi "Implements and depends on"
    }

    views {
        systemContext platform "CurrentSystemContext" {
            include *
            autolayout lr
        }

        custom "CurrentModuleMap" "Current module map" "Implemented Gradle boundary for the Event reference module." {
            include eventApi eventImpl
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
