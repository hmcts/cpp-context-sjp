package uk.gov.moj.cpp.sjp.domain.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.aDefaultSjpCase;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.util.DefaultTestData;
import uk.gov.moj.cpp.sjp.event.CaseCreationFailedBecauseCaseAlreadyExisted;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.CaseStarted;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class CaseStartedTest {

    private final Clock clock = new StoppedClock(new UtcClock().now());
    private CaseAggregate caseAggregate;

    @Before
    public void setUp() {
        caseAggregate = new CaseAggregate();
    }

    @Test
    public void shouldCreateCaseReceivedEvent() {
        Case aCase = aDefaultSjpCase().build();
        ZonedDateTime createdOn = clock.now();
        final Stream<Object> events = caseAggregate.receiveCase(aCase, createdOn);

        CaseReceived caseReceivedEvent = (CaseReceived) events.findFirst().get();
        assertThat("Sets defendant", caseReceivedEvent.getDefendant(), notNullValue());
        assertThat("Sets case id", caseReceivedEvent.getCaseId(), equalTo(aCase.getId()));
        assertThat("Sets urn", caseReceivedEvent.getUrn(), equalTo(aCase.getUrn()));
        assertThat("Sets enterprise id", caseReceivedEvent.getEnterpriseId(), equalTo(aCase.getEnterpriseId()));
        assertThat("Sets prosecuting authority", caseReceivedEvent.getProsecutingAuthority(), equalTo(aCase.getProsecutingAuthority()));
        assertThat("Sets costs", caseReceivedEvent.getCosts(), equalTo(aCase.getCosts()));
        assertThat("Sets posting date", caseReceivedEvent.getPostingDate(), equalTo(aCase.getPostingDate()));
        assertThat("Sets created on date", caseReceivedEvent.getCreatedOn(), equalTo(createdOn));
    }

    @Test
    public void shouldCreateCaseCreationFailedEventWhenCaseAlreadyReceived() {
        Case aCase = aDefaultSjpCase().build();
        caseAggregate.receiveCase(aCase, clock.now());

        final Stream<Object> events = caseAggregate.receiveCase(aCase, clock.now());

        final CaseCreationFailedBecauseCaseAlreadyExisted caseCreationFailedEvent = (CaseCreationFailedBecauseCaseAlreadyExisted) events.findFirst().get();
        assertThat("Sets case id", caseCreationFailedEvent.getCaseId(), equalTo(aCase.getId()));
        assertThat("Sets urn", caseCreationFailedEvent.getUrn(), equalTo(aCase.getUrn()));
    }
}
