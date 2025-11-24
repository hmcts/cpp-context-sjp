package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Collections.singleton;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest.AggregateTester.when;

import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.event.CaseCreationFailedBecauseCaseAlreadyExisted;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;

import org.junit.jupiter.api.Test;

public class CaseReceivedTest extends CaseAggregateBaseTest {

    @Test
    public void testCreateCase_whenValidSjpCase_shouldTriggerExpectedCaseCreated() {
        final CaseReceived expectedCaseReceived = buildCaseReceived(aCase);
        assertThat(reflectionEquals(caseReceivedEvent, expectedCaseReceived, singleton("defendant")), is(true));
        assertThat(caseReceivedEvent.getDefendant().getId(), equalTo(defendantId));
    }

    @Test
    public void rejectsSecondCaseReceivingWithAppropriateEvent() {
        when(caseAggregate.receiveCase(aCase, clock.now()))
                .reason("reject when receiving the same case again")
                .thenExpect(new CaseCreationFailedBecauseCaseAlreadyExisted(aCase.getId(), aCase.getUrn()));
    }

    /**
     * To ensure backward compatibility
     */
    @SuppressWarnings("deprecation")
    private SjpCaseCreated buildSjpCaseCreated(final Case aCase) {
        return new SjpCaseCreated(aCase.getId(), aCase.getUrn(), aCase.getProsecutingAuthority(),
                aCase.getDefendant().getId(), aCase.getDefendant().getNumPreviousConvictions(), aCase.getCosts(),
                aCase.getPostingDate(), aCase.getDefendant().getOffences(), clock.now());
    }

}
