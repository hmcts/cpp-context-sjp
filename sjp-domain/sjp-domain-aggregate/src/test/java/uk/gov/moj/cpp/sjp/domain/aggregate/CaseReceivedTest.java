package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Collections.singleton;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.builder.EqualsBuilder.reflectionEquals;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.CaseCreationFailedBecauseCaseAlreadyExisted;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

public class CaseReceivedTest extends CaseAggregateBaseTest {

    @Before
    @Override
    public void setUp() {
        super.setUp();
        caseAggregate = new CaseAggregate();
    }

    @Test
    public void testCreateCase_whenValidSjpCase_shouldTriggerExpectedCaseCreated() {
        final List<Object> events = caseAggregate.receiveCase(aCase, clock.now()).collect(Collectors.toList());

        final CaseReceived expectedCaseReceived = buildCaseReceived(aCase);
        assertThat(reflectionEquals(events.get(0), expectedCaseReceived, singleton("defendant")), is(true));
        assertThat(reflectionEquals(
                ((CaseReceived) events.get(0)).getDefendant(),
                expectedCaseReceived.getDefendant(),
                singleton("id")), is(true));
    }

    @Test
    public void rejectsSecondCaseReceivingWithAppropriateEvent() {
        // GIVEN the case was received
        caseAggregate.receiveCase(aCase, clock.now());

        // WHEN receiving the same case again
        final List<Object> events = caseAggregate.receiveCase(aCase, clock.now()).collect(Collectors.toList());

        assertThat(events, hasSize(1));

        final CaseCreationFailedBecauseCaseAlreadyExisted rejectionEvent = (CaseCreationFailedBecauseCaseAlreadyExisted) events.get(0);

        assertThat(rejectionEvent.getCaseId(), equalTo(aCase.getId()));
        assertThat(rejectionEvent.getUrn(), equalTo(aCase.getUrn()));
    }

    /**
     * For ensure backward compatibility
     */
    @Test
    public void testApply_whenSjpCaseCreatedEvent() {
        //GIVEN legacy case was created (using SjpCaseCreated)
        SjpCaseCreated sjpCaseCreated = buildSjpCaseCreated(CaseBuilder.aDefaultSjpCase().build());
        caseAggregate.apply(sjpCaseCreated);

        //WHEN we try to create duplicate state
        final List<Object> events = caseAggregate.receiveCase(aCase, clock.now()).collect(Collectors.toList());
        assertThat(events, hasSize(1));

        //THEN it fails as the state was properly managed
        final CaseCreationFailedBecauseCaseAlreadyExisted rejectionEvent = (CaseCreationFailedBecauseCaseAlreadyExisted) events.get(0);
        assertThat("Case id does not match", rejectionEvent.getCaseId(), equalTo(sjpCaseCreated.getId()));
        assertThat("Case urn does not match", rejectionEvent.getUrn(), equalTo(sjpCaseCreated.getUrn()));

        //and WHEN we try to update plea
        final List<Object> pleaUpdateEvents = caseAggregate.updatePlea(randomUUID(),
                new UpdatePlea(aCase.getId(), aCase.getDefendant().getOffences().get(0).getId(),
                        PleaType.GUILTY),
                clock.now()).collect(toList());

        //THEN plea is properly updated
        assertThat(pleaUpdateEvents, hasSize(1));
        final PleaUpdated pleaUpdateEvent = (PleaUpdated) pleaUpdateEvents.get(0);
        assertThat(pleaUpdateEvent.getPlea(), equalTo(PleaType.GUILTY));
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
