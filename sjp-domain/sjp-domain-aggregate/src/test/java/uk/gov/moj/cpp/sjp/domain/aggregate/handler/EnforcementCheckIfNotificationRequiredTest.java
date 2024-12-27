package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_PENDING;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationType.STAT_DEC;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.EnforcementCheckIfNotificationRequired.INSTANCE;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.moj.cpp.sjp.domain.EnforcementPendingApplicationRequiredNotification;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.Application;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.FinancialImpositionExportDetails;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationRequired;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
@SuppressWarnings("squid:S2187")
@ExtendWith(MockitoExtension.class)
public class EnforcementCheckIfNotificationRequiredTest {
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

    @BeforeEach
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
        when(courtApplication.getSubject()).thenReturn(CourtApplicationParty.courtApplicationParty()
                .withMasterDefendant(MasterDefendant.masterDefendant()
                        .withMasterDefendantId(randomUUID())
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withAddress(Address.address()
                                                .withAddress1("Test address one")
                                                .withAddress2("Test address two")
                                                .withPostcode("RG1 9DS").build())
                                        .withDateOfBirth("26/03/1967")
                                        .withContact(ContactNumber.contactNumber()
                                                .withMobile("02032389928")
                                                .withPrimaryEmail("test@hotmail.com").build()).build()).build()).build()).build());
        application.setStatus(STATUTORY_DECLARATION_PENDING);
        application.setType(STAT_DEC);
        caseAggregateState.setCurrentApplication(application);
        caseAggregateState.setSavedAt(LocalDate.now());
        final List<Object> eventStream = INSTANCE.checkIfPendingApplicationToNotified(caseAggregateState, checkNotificationRequired).collect(toList());

        // then
        final EnforcementPendingApplicationNotificationRequired notificationRequired = (EnforcementPendingApplicationNotificationRequired) (eventStream.get(0));
        assertThat(notificationRequired.getDefendantName(), is(firstName + " " + lastName));
        assertThat(notificationRequired.getApplicationId(), is(applicationId));
        assertThat(notificationRequired.getGobAccountNumber(), is(accountNumber));
        assertThat(notificationRequired.getUrn(), is(urn));
        assertThat(notificationRequired.getDivisionCode(), is(divisionCode));
        assertThat(notificationRequired.getInitiatedTime(), is(notNullValue()));
        assertThat(notificationRequired.getDefendantAddress(), is("Test address one Test address two RG1 9DS"));
        assertThat(notificationRequired.getDefendantDateOfBirth(), is("26/03/1967"));
        assertThat(notificationRequired.getDefendantContactNumber(), is("02032389928"));
        assertThat(notificationRequired.getDefendantEmail(), is("test@hotmail.com"));
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

    @Test
    public void shouldCreateEnforcementPendingApplicationNotificationRequiredEventForOrganisationDefendant(){
        final EnforcementPendingApplicationRequiredNotification checkNotificationRequired = new EnforcementPendingApplicationRequiredNotification(caseId, divisionCode);
        final Application application = new Application(courtApplication);

        when(courtApplication.getApplicationReceivedDate()).thenReturn(LocalDate.now().toString());
        when(courtApplication.getId()).thenReturn(applicationId);
        when(courtApplication.getSubject()).thenReturn(CourtApplicationParty.courtApplicationParty()
                .withMasterDefendant(MasterDefendant.masterDefendant()
                        .withMasterDefendantId(randomUUID())
                        .withLegalEntityDefendant(LegalEntityDefendant
                                .legalEntityDefendant()
                                .withOrganisation(Organisation
                                        .organisation()
                                        .withName("Kellogs and Co")
                                        .build())
                                .build())
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withAddress(Address.address()
                                                .withAddress1("Test address one")
                                                .withAddress2("Test address two")
                                                .withPostcode("RG1 9DS").build())
                                        .withDateOfBirth("26/03/1967")
                                        .withContact(ContactNumber.contactNumber()
                                                .withMobile("02032389928")
                                                .withPrimaryEmail("test@hotmail.com").build()).build()).build()).build()).build());
        application.setStatus(STATUTORY_DECLARATION_PENDING);
        application.setType(STAT_DEC);
        caseAggregateState.setCurrentApplication(application);
        caseAggregateState.setSavedAt(LocalDate.now());
        final List<Object> eventStream = INSTANCE.checkIfPendingApplicationToNotified(caseAggregateState, checkNotificationRequired).collect(toList());
        final EnforcementPendingApplicationNotificationRequired notificationRequired = (EnforcementPendingApplicationNotificationRequired) (eventStream.get(0));
        assertThat(notificationRequired.getDefendantName(), is("Kellogs and Co"));
    }
}
