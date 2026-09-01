package composable.domain.platform.waitlist.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.waitlist.api.WaitlistEventReference;
import composable.domain.platform.waitlist.api.WaitlistParticipantReference;
import org.junit.jupiter.api.Test;

class WaitlistServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("waitlist-service-test"));

    @Test
    void repeatedJoinReturnsSameDurableParticipation() {
        InMemoryWaitlistParticipationRepository repository =
                new InMemoryWaitlistParticipationRepository();
        JoinWaitlistService join = new JoinWaitlistService(repository);
        FindWaitlistParticipationService find =
                new FindWaitlistParticipationService(repository);

        WaitlistParticipantReference participant =
                new WaitlistParticipantReference("participant-a");
        WaitlistEventReference event =
                new WaitlistEventReference("event-a");

        var first = join.join(CONTEXT, participant, event);
        var repeated = join.join(CONTEXT, participant, event);

        assertEquals(first, repeated);
        assertEquals(1, repository.size());
        assertEquals(
                first,
                find.findByParticipantAndEvent(
                                CONTEXT,
                                participant,
                                event)
                        .orElseThrow());
        assertFalse(first.waitlistParticipationId().isBlank());
    }
}
