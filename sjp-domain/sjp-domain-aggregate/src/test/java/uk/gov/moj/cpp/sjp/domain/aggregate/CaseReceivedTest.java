package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Collections.singleton;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang.builder.EqualsBuilder.reflectionEquals;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest.AggregateTester.when;

import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.CaseCreationFailedBecauseCaseAlreadyExisted;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;

import org.junit.Test;

public class CaseReceivedTest extends CaseAggregateBaseTest {

    @Test
    public void testCreateCase_whenValidSjpCase_shouldTriggerExpectedCaseCreated() {
        final CaseReceived expectedCaseReceived = buildCaseReceived(aCase);
        assertThat(reflectionEquals(caseReceivedEvent, expectedCaseReceived, singleton("defendant")), is(true));
        assertThat(reflectionEquals(
                caseReceivedEvent.getDefendant(),
                expectedCaseReceived.getDefendant(),
                singleton("id")), is(true));
        assertThat(caseReceivedEvent.getDefendant().getId(), notNullValue());
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
    @Test
    @SuppressWarnings("deprecation")
    public void testApply_whenSjpCaseCreatedEvent() {
        //GIVEN legacy case was created (using SjpCaseCreated)
        final SjpCaseCreated sjpCaseCreated = buildSjpCaseCreated(CaseBuilder.aDefaultSjpCase().build());
        caseAggregate.apply(sjpCaseCreated);

        when(caseAggregate.receiveCase(aCase, clock.now()))
                .reason("should not create duplicate state")
                .thenExpect(new CaseCreationFailedBecauseCaseAlreadyExisted(aCase.getId(), aCase.getUrn()));

        final PleaType pleaType = PleaType.GUILTY;
        when(caseAggregate.updatePlea(
                randomUUID(),
                new UpdatePlea(caseId, offenceId, pleaType), clock.now()))
                .thenExpect(new PleaUpdated(caseId, offenceId, pleaType, PleaMethod.ONLINE, clock.now()));
    }

    /**
     * To ensure backward compatibility
     */
    @SuppressWarnings("deprecation")
    private SjpCaseCreated buildSjpCaseCreated(Case aCase) {
        return new SjpCaseCreated(aCase.getId(), aCase.getUrn(), aCase.getProsecutingAuthority(),
                aCase.getDefendant().getId(), aCase.getDefendant().getNumPreviousConvictions(), aCase.getCosts(),
                aCase.getPostingDate(), aCase.getDefendant().getOffences(), clock.now());
    }

}
