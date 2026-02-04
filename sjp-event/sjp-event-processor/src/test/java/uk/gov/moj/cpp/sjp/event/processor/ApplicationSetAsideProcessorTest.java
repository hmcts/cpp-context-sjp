package uk.gov.moj.cpp.sjp.event.processor;

import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.converter.JsonObjectToObjectConverterFactory.createJsonObjectToObjectConverter;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationType;
import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.DecisionType;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.json.schemas.domains.sjp.queries.QueryApplicationDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.QueryOffenceDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.Session;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.decision.ApplicationDecisionSetAside;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.models.CaseDetailsDecorator;
import uk.gov.moj.cpp.sjp.event.processor.service.notification.EndorsementRemovalNotificationService;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.ApplicationDecisionSetAsideEnvelope;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationSetAsideProcessorTest {

    private static final String OFFENCE_CODE = "OFFENCE_CODE";
    private static final LocalDate OFFENCE_START_DATE = LocalDate.of(2020, 1, 1);
    private static final String LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE = "2577";
    private static final ZonedDateTime ORIGINAL_CONVICTION_DATE = ZonedDateTime.of(2020, 1, 2, 0, 0, 0, 0, ZoneId.systemDefault());

    @Spy
    private JsonObjectToObjectConverter converter = createJsonObjectToObjectConverter();
    @Mock
    private EndorsementRemovalNotificationService endorsementRemovalNotificationService;
    @Mock
    private SjpService sjpService;
    @Mock
    private Sender sender;
    @InjectMocks
    private ApplicationSetAsideProcessor processor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    private UUID caseId;
    private UUID sessionId;
    private UUID applicationId;
    private UUID applicationDecisionId;
    private Session session;
    private JsonEnvelope envelope;
    private ZonedDateTime applicationSavedAt;
    private CaseDetails caseDetails;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID();
        sessionId = randomUUID();
        applicationId = randomUUID();
        applicationSavedAt = ZonedDateTime.of(LocalDate.of(2020, 11, 20), LocalTime.now(), ZoneId.systemDefault());
        applicationDecisionId = randomUUID();
        final ApplicationDecisionSetAside applicationDecisionSetAside = new ApplicationDecisionSetAside(applicationId, caseId, "TVLXYZ01");
        envelope = ApplicationDecisionSetAsideEnvelope.of(applicationDecisionSetAside);
    }

    @Test
    public void shouldPublishPublicEventOnApplicationSetAside() throws FileServiceException {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("applicationID", applicationDecisionId.toString())
                .add("caseUrn", "TVLXYZ01")
                .build();
        envelope = ApplicationDecisionSetAsideEnvelope.of(payload);
        givenCaseWithoutEndorsementsToBeRemoved();

        processor.handleApplicationDecisionSetAside(envelope);

        verify(sender).send(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().metadata().name(), is("public.sjp.application-decision-set-aside"));
        assertThat(envelopeCaptor.getValue().payloadAsJsonObject(), is(payload));
        assertThat(envelopeCaptor.getValue().payloadAsJsonObject().getString("caseUrn"), is("TVLXYZ01"));
    }

    @Test
    public void shouldRequestFileGenerationWhenThereAreEndorsementsToBeRemoved() throws FileServiceException {
        givenCaseWithEndorsementsToBeRemoved();
        final CaseDetailsDecorator decoratedCaseDetails = new CaseDetailsDecorator(caseDetails);
        when(endorsementRemovalNotificationService.hasEndorsementsToBeRemoved(decoratedCaseDetails)).thenReturn(true);

        processor.handleApplicationDecisionSetAside(envelope);

        verify(endorsementRemovalNotificationService).generateNotification(decoratedCaseDetails, envelope);
    }

    @Test
    public void shouldNotRequestFileGenerationWhenThereAreNoEndorsementsToBeRemoved() throws FileServiceException {
        givenCaseWithoutEndorsementsToBeRemoved();
        when(endorsementRemovalNotificationService.hasEndorsementsToBeRemoved(new CaseDetailsDecorator(caseDetails))).thenReturn(false);

        processor.handleApplicationDecisionSetAside(envelope);

        verify(endorsementRemovalNotificationService, never()).generateNotification(new CaseDetailsDecorator(caseDetails), envelope);
    }

    private Offence.Builder createOffence() {
        return Offence.offence()
                .withId(randomUUID())
                .withOffenceCode(OFFENCE_CODE)
                .withStartDate(OFFENCE_START_DATE.toString());
    }

    private CaseDecision statDecGrantedDecision(final ZonedDateTime previousFinalDecision) {
        return CaseDecision.caseDecision()
                .withId(applicationDecisionId)
                .withSavedAt(applicationSavedAt)
                .withApplicationDecision(applicationDecision(previousFinalDecision).build())
                .withSession(session)
                .build();
    }

    private CaseDecision.Builder financialPenaltyDecision(final UUID offenceId) {
        return CaseDecision.caseDecision()
                .withId(randomUUID())
                .withSavedAt(now())
                .withOffenceDecisions(singletonList(
                        financialPenaltyWithEndorsements(offenceId, ORIGINAL_CONVICTION_DATE.toLocalDate())))
                .withSession(session);
    }

    private QueryOffenceDecision financialPenaltyWithEndorsements(final UUID offenceId, final LocalDate originalConvictionDate) {
        return QueryOffenceDecision.queryOffenceDecision()
                .withOffenceId(offenceId)
                .withDecisionType(DecisionType.FINANCIAL_PENALTY)
                .withLicenceEndorsement(true)
                .withPenaltyPointsImposed(10)
                .withConvictionDate(originalConvictionDate)
                .build();
    }

    private void startSession() {
        session = Session.session()
                .withSessionId(sessionId)
                .withCourtHouseCode("Court House Code")
                .withCourtHouseName("Court House Name")
                .withLocalJusticeAreaNationalCourtCode(LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE)
                .build();
    }

    private QueryApplicationDecision.Builder applicationDecision(final ZonedDateTime previousFinalDecisionSavedAt) {
        return QueryApplicationDecision.queryApplicationDecision()
                .withApplicationType(ApplicationType.STAT_DEC)
                .withOutOfTime(false)
                .withGranted(true)
                .withPreviousFinalDecision(previousFinalDecisionSavedAt);
    }


    private CaseDetails.Builder createCase(final List<Offence> offences) {
        final CaseDetails.Builder builder = CaseDetails.caseDetails()
                .withId(caseId)
                .withUrn("Case URN")
                .withDefendant(Defendant.defendant()
                        .withPersonalDetails(PersonalDetails.personalDetails()
                                .withFirstName("Robert")
                                .withLastName("Robertson")
                                .withDateOfBirth(LocalDate.of(1980, 5, 21))
                                .withGender(Gender.MALE)
                                .withDriverNumber("Driver Number")
                                .withAddress(Address.address()
                                        .withAddress1("line1")
                                        .withAddress2("line2")
                                        .withPostcode("POSTCODE")
                                        .build())
                                .build())
                        .withOffences(offences)
                        .build());

        final CaseDecision caseDecision1 = financialPenaltyDecision(offences.get(0).getId()).build();
        final CaseDecision caseDecision2 = statDecGrantedDecision(caseDecision1.getSavedAt());
        builder.withCaseDecisions(asList(caseDecision1, caseDecision2));

        return builder;
    }

    private void givenCaseWithEndorsementsToBeRemoved() {
        startSession();

        final Offence offence = createOffence().build();
        caseDetails = createCase(singletonList(offence)).build();
        when(sjpService.getCaseDetails(caseId, envelope)).thenReturn(caseDetails);
    }

    private void givenCaseWithoutEndorsementsToBeRemoved() {
        caseDetails = CaseDetails.caseDetails()
                .withCaseDecisions(emptyList())
                .build();
        when(sjpService.getCaseDetails(caseId, envelope)).thenReturn(caseDetails);
    }
}
