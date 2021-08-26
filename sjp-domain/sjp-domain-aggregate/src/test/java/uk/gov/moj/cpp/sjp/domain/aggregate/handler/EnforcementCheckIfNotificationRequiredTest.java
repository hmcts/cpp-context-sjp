package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_PENDING;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationType.STAT_DEC;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.EnforcementCheckIfNotificationRequired.INSTANCE;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.moj.cpp.sjp.domain.EnforcementPendingApplicationRequiredNotification;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.Application;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.FinancialImpositionExportDetails;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationRequired;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
@SuppressWarnings("squid:S2187")
@RunWith(MockitoJUnitRunner.class)
public class EnforcementCheckIfNotificationRequiredTest extends TestCase {
    private final CaseAggregateState caseAggregateState = new CaseAggregateState();
    // given
    final UUID caseId = randomUUID();
    final UUID applicationId = randomUUID();
    final UUID defendantId = randomUUID();
    final String urn = "urn";
    final String accountNumber = "123456";
    final String firstName = "FirstName";
    final String lastName = "LastName";
    final int divisionCode = 100;
    final LocalDate applicationListedDate = LocalDate.now();

    @Mock
    private CourtApplication courtApplication;

    @Before
    public void setUp() {

        caseAggregateState.setCaseId(caseId);
        caseAggregateState.setDefendantId(defendantId);

        final Application application = new Application(null);
        application.setType(STAT_DEC);
        application.setStatus(STATUTORY_DECLARATION_PENDING);
        final FinancialImpositionExportDetails exportDetails = new FinancialImpositionExportDetails();
        exportDetails.setAccountNumber(accountNumber);
        caseAggregateState.addFinancialImpositionExportDetails(defendantId, exportDetails);
        caseAggregateState.setCurrentApplication(application);
        caseAggregateState.setUrn(urn);
        caseAggregateState.setDefendantFirstName(firstName);
        caseAggregateState.setDefendantLastName(lastName);

    }

    @Test
    public void shouldCreateEnforcementNotificationRequiredEvent() {
        // when
        final EnforcementPendingApplicationRequiredNotification checkNotificationRequired = new EnforcementPendingApplicationRequiredNotification(caseId, divisionCode);
        final Application application = new Application(courtApplication);

        when(courtApplication.getApplicationReceivedDate()).thenReturn(LocalDate.now().toString());
        when(courtApplication.getId()).thenReturn(applicationId);

        application.setStatus(STATUTORY_DECLARATION_PENDING);
        application.setType(STAT_DEC);
        caseAggregateState.setCurrentApplication(application);
        final List<Object> eventStream = INSTANCE.checkIfPendingApplicationToNotified(caseAggregateState, checkNotificationRequired).collect(toList());

        // then
        final EnforcementPendingApplicationNotificationRequired notificationRequired = (EnforcementPendingApplicationNotificationRequired) (eventStream.get(0));
        assertThat(notificationRequired.getDefendantName(), is(firstName + " " + lastName));
        assertThat(notificationRequired.getApplicationId(), is(applicationId));
        assertThat(notificationRequired.getGobAccountNumber(), is(accountNumber));
        assertThat(notificationRequired.getUrn(), is(urn));
        assertThat(notificationRequired.getDivisionCode(), is(divisionCode));
        assertThat(notificationRequired.getInitiatedTime(), is(notNullValue()));
    }

    @Test
    public void shouldNotCreateEnforcementNotificationRequiredEvent() {
        // when
        caseAggregateState.addFinancialImpositionExportDetails(defendantId,null);
        final EnforcementPendingApplicationRequiredNotification checkNotificationRequired = new EnforcementPendingApplicationRequiredNotification(caseId, divisionCode);
        final List<Object> eventStream = INSTANCE.checkIfPendingApplicationToNotified(caseAggregateState, checkNotificationRequired).collect(toList());

        // then
        assertThat(eventStream.isEmpty(),is(true));

    }
}