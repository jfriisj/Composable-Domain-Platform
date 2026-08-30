package composable.domain.platform.waitlist.application;

import composable.domain.platform.waitlist.domain.WaitlistParticipation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class InMemoryWaitlistParticipationRepository
        implements WaitlistParticipationRepository {

    private final Map<String, WaitlistParticipation> byId = new HashMap<>();
    private final Map<ParticipationPair, WaitlistParticipation> byPair =
            new HashMap<>();

    @Override
    public synchronized boolean addIfAbsent(
            WaitlistParticipation participation) {
        ParticipationPair pair = ParticipationPair.from(participation);

        if (byId.containsKey(participation.id())
                || byPair.containsKey(pair)) {
            return false;
        }

        byId.put(participation.id(), participation);
        byPair.put(pair, participation);
        return true;
    }

    @Override
    public synchronized Optional<WaitlistParticipation>
            findByParticipantAndEvent(
                    String participantReference,
                    String eventReference) {
        return Optional.ofNullable(
                byPair.get(new ParticipationPair(
                        participantReference,
                        eventReference)));
    }

    int size() {
        return byPair.size();
    }

    private record ParticipationPair(
            String participantReference,
            String eventReference) {

        static ParticipationPair from(
                WaitlistParticipation participation) {
            return new ParticipationPair(
                    participation.participantReference(),
                    participation.eventReference());
        }
    }
}
