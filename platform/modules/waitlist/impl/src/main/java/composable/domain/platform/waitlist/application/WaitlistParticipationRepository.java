package composable.domain.platform.waitlist.application;

import composable.domain.platform.waitlist.domain.WaitlistParticipation;
import java.util.Optional;

public interface WaitlistParticipationRepository {

    boolean addIfAbsent(WaitlistParticipation participation);

    Optional<WaitlistParticipation> findByParticipantAndEvent(
            String participantReference,
            String eventReference);
}
