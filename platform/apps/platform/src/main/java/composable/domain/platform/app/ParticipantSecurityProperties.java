package composable.domain.platform.app;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.security")
public final class ParticipantSecurityProperties {

    private List<Participant> participants = List.of();

    public List<Participant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Participant> participants) {
        this.participants =
                participants == null ? List.of() : List.copyOf(participants);
    }

    public static final class Participant {

        private String principal;
        private String passwordVerifier;

        public String getPrincipal() {
            return principal;
        }

        public void setPrincipal(String principal) {
            this.principal = principal;
        }

        public String getPasswordVerifier() {
            return passwordVerifier;
        }

        public void setPasswordVerifier(String passwordVerifier) {
            this.passwordVerifier = passwordVerifier;
        }
    }
}
