package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Collections.singleton;
import static java.util.UUID.randomUUID;
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
import java.util.stream.Stream;

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
    }

    @Test
    public void rejectsSecondCaseReceivingWithAppropriateEvent() {
        // WHEN receiving the same case again
        final Stream<Object> events = caseAggregate.receiveCase(aCase, clock.now());

        final CaseCreationFailedBecauseCaseAlreadyExisted rejectionEvent = collectSingleEvent(events, CaseCreationFailedBecauseCaseAlreadyExisted.class);

        assertThat(rejectionEvent, equalTo(new CaseCreationFailedBecauseCaseAlreadyExisted(aCase.getId(), aCase.getUrn())));
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

        //WHEN we try to create duplicate state
        final List<Object> events = caseAggregate.receiveCase(aCase, clock.now()).collect(Collectors.toList());
        assertThat(events, hasSize(1));

        //THEN it fails as the state was properly managed
        final CaseCreationFailedBecauseCaseAlreadyExisted rejectionEvent = collectSingleEvent(events.stream(), CaseCreationFailedBecauseCaseAlreadyExisted.class);
        assertThat(rejectionEvent, equalTo(new CaseCreationFailedBecauseCaseAlreadyExisted(aCase.getId(), aCase.getUrn())));

        //and WHEN we try to update plea
        final Stream<Object> pleaUpdateEvents = caseAggregate.updatePlea(
                randomUUID(),
                new UpdatePlea(
                        aCase.getId(),
                        offenceId,
                        PleaType.GUILTY),
                clock.now());

        //THEN plea is properly updated
        final PleaUpdated pleaUpdateEvent = collectSingleEvent(pleaUpdateEvents, PleaUpdated.class);
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
