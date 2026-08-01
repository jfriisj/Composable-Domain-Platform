workspace "Composable Domain Platform" "Authoritative architecture model for the Composable Domain Platform." {

    model {
        stakeholder = person "Stakeholder" "Shapes platform requirements and validates business outcomes."
        platform = softwareSystem "Composable Domain Platform" "A modular application platform for independently bounded business capabilities."

        stakeholder -> platform "Uses and shapes"
    }

    views {
        systemContext platform "CurrentSystemContext" {
            include *
            autolayout lr
        }

        styles {
            element "Person" {
                shape person
            }
        }
    }
}
