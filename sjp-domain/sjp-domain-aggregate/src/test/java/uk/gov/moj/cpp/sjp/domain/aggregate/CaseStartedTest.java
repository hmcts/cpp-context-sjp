package uk.gov.moj.cpp.sjp.domain.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.aDefaultSjpCase;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.event.CaseCreationFailedBecauseCaseAlreadyExisted;
import uk.gov.moj.cpp.sjp.event.CaseReceived;

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
        final Case aCase = aDefaultSjpCase().build();
        final ZonedDateTime createdOn = clock.now();
        final Stream<Object> events = caseAggregate.receiveCase(aCase, createdOn);

        final CaseReceived caseReceivedEvent = (CaseReceived) events.findFirst().get();
        assertThat("Sets defendant", caseReceivedEvent.getDefendant(), notNullValue());
        assertThat("Sets defendant id", caseReceivedEvent.getDefendant().getId(), equalTo(aCase.getDefendant().getId()));
        assertThat("Sets defendant language needs", caseReceivedEvent.getDefendant().getLanguageNeeds(), equalTo(aCase.getDefendant().getLanguageNeeds()));
        assertThat("Sets defendant number of previous convictions", caseReceivedEvent.getDefendant().getNumPreviousConvictions(), equalTo(aCase.getDefendant().getNumPreviousConvictions()));
        assertThat("Sets defendant date of birth", caseReceivedEvent.getDefendant().getDateOfBirth(), equalTo(aCase.getDefendant().getDateOfBirth()));
        assertThat("Sets defendant driver number", caseReceivedEvent.getDefendant().getDriverNumber(), equalTo(aCase.getDefendant().getDriverNumber()));
        assertThat("Sets defendant title", caseReceivedEvent.getDefendant().getTitle(), equalTo(aCase.getDefendant().getTitle()));
        assertThat("Sets defendant first name", caseReceivedEvent.getDefendant().getFirstName(), equalTo(aCase.getDefendant().getFirstName()));
        assertThat("Sets defendant last name", caseReceivedEvent.getDefendant().getLastName(), equalTo(aCase.getDefendant().getLastName()));
        assertThat("Sets defendant gender", caseReceivedEvent.getDefendant().getGender(), equalTo(aCase.getDefendant().getGender()));
        assertThat("Sets defendant national insurance number", caseReceivedEvent.getDefendant().getNationalInsuranceNumber(), equalTo(aCase.getDefendant().getNationalInsuranceNumber()));
        assertThat("Sets defendant offences", caseReceivedEvent.getDefendant().getOffences(), equalTo(aCase.getDefendant().getOffences()));
        assertThat("Sets defendant address", caseReceivedEvent.getDefendant().getAddress(), equalTo(aCase.getDefendant().getAddress()));
        assertThat("Sets defendant contact details", caseReceivedEvent.getDefendant().getContactDetails(), equalTo(aCase.getDefendant().getContactDetails()));
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
        final Case aCase = aDefaultSjpCase().build();
        caseAggregate.receiveCase(aCase, clock.now());

        final Stream<Object> events = caseAggregate.receiveCase(aCase, clock.now());

        final CaseCreationFailedBecauseCaseAlreadyExisted caseCreationFailedEvent = (CaseCreationFailedBecauseCaseAlreadyExisted) events.findFirst().get();
        assertThat("Sets case id", caseCreationFailedEvent.getCaseId(), equalTo(aCase.getId()));
        assertThat("Sets urn", caseCreationFailedEvent.getUrn(), equalTo(aCase.getUrn()));
    }
}
