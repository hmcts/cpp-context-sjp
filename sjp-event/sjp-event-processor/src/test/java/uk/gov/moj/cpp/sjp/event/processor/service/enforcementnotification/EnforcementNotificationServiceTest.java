package uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification;

import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationType;
import uk.gov.justice.json.schemas.domains.sjp.CaseApplication;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.queries.QueryFinancialImposition;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.moj.cpp.sjp.domain.EnforcementPendingApplicationRequiredNotification;
import uk.gov.moj.cpp.sjp.event.CaseApplicationRecorded;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("squid:S2187")
public class EnforcementNotificationServiceTest {
    static final String CASE_APPLICATION_RECORDED_PUBLIC_EVENT = "public.sjp.case-application-recorded";
    static final String ENFORCEMENT_PENDING_APPLICATION_INITIATE_NOTIFICATION_COMMAND = "sjp.command.enforcement-pending-application-check-requires-notification";
    private static final UUID APPLICATION_ID = randomUUID();

    private final static UUID CASE_ID = randomUUID();
    private static final String STAT_DEC_CODE = "MC80528";
    private static final LocalDate APPLICATION_RECEIVED_DATE = LocalDate.now().minusDays(1);
    private static final String POST_CODE = "ABC1AB";
    private static final String GOB_ACCOUNT_NUMBER = "GOB";
    private static final String URN = "URN";
    private static final String FIRST_NAME = "FIRST_NAME";
    private static final String LAST_NAME = "LAST_NAME";
    private static final int DIVISION_CODE = 10;

    private CaseApplication caseApplication;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private CaseDetails caseDetails;
    @Spy
    private Enveloper envelopers = createEnveloper();

    @Mock
    private DivisionCodeHelper divisionCodeHelper;

    @Mock
    private FinancialImpositionHelper financialImpositionHelper;

    @Mock
    private Sender sender;

    @Mock
    private LastDecisionHelper lastDecisionHelper;

    @Mock
    private EnforcementAreaEmailHelper enforcementAreaEmailHelper;

    @Mock
    private CaseDecision lastDecision;

    @Mock
    private List<CaseDecision> caseDecisions;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Defendant defendant;

    @Mock
    private SjpService sjpService;

    @Mock
    private Stream<CaseDecision> streamOfCaseDecisions;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> captor;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private QueryFinancialImposition queryFinancialImposition;

    @InjectMocks
    EnforcementNotificationService enforcementNotificationService;

    @Test
    public void shouldRaiseEnforcementPendingApplicationInitateNotificationCommand() {

        final CaseApplicationRecorded caseApplicationRecorded = new CaseApplicationRecorded.Builder()
                .withCourtApplication(STAT_DEC_TYPE_APP)
                .withCaseId(CASE_ID)
                .build();

        final JsonObject jsonObject = mock(JsonObject.class);
        final JsonEnvelope privateEvent = EnvelopeFactory.createEnvelope(CASE_APPLICATION_RECORDED_PUBLIC_EVENT, jsonObject);
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());

        when(defendant.getPersonalDetails().getAddress().getPostcode()).thenReturn(POST_CODE);
        when(divisionCodeHelper.divisionCode(privateEvent, caseDetails, POST_CODE)).thenReturn(DIVISION_CODE);
        when(caseDetails.getDefendant()).thenReturn(defendant);
        when(caseDetails.getCaseDecisions()).thenReturn(caseDecisions);

        when(sjpService.getCaseDetails(CASE_ID, privateEvent)).thenReturn(caseDetails);

        enforcementNotificationService.checkIfEnforcementToBeNotified(CASE_ID, privateEvent);
        verify(sender).send(captor.capture());
        final Envelope<JsonObject> argumentCaptor = captor.getValue();
        final Metadata metadata = argumentCaptor.metadata();
        final EnforcementPendingApplicationRequiredNotification payload = (EnforcementPendingApplicationRequiredNotification) argumentCaptor.payload();
        Assert.assertThat(metadata.name(), is(ENFORCEMENT_PENDING_APPLICATION_INITIATE_NOTIFICATION_COMMAND));
        assertThat(payload.getDivisionCode(), is(DIVISION_CODE));
    }


    private final static CourtApplication STAT_DEC_TYPE_APP = CourtApplication.courtApplication()
            .withId(APPLICATION_ID)
            .withApplicationStatus(ApplicationStatus.DRAFT)
            .withApplicationReference("ApplicationReference")
            .withApplicationReceivedDate(APPLICATION_RECEIVED_DATE.toString())
            .withType(CourtApplicationType.courtApplicationType()
                    .withId(randomUUID())
                    .withType("Appearance to make statutory declaration (SJP case)")
                    .withCode(STAT_DEC_CODE)
                    .withAppealFlag(false)
                    .build())
            .withApplicant(CourtApplicationParty.courtApplicationParty()
                    .withId(fromString("5002d600-af66-11e8-b568-0800200c9a67"))
                    .withSummonsRequired(false)
                    .withNotificationRequired(false)
                    .build())
            .withSubject(CourtApplicationParty.courtApplicationParty()
                    .withId(fromString("5002d600-af66-11e8-b568-0800200c9a68"))
                    .withSummonsRequired(false)
                    .withNotificationRequired(false)
                    .build())
            .build();

    private final static CourtApplication APPLICATION_FOR_NO_TYPE_APP = CourtApplication.courtApplication()
            .withId(CASE_ID)
            .withApplicationStatus(ApplicationStatus.DRAFT)
            .withApplicationReceivedDate(APPLICATION_RECEIVED_DATE.toString())
            .withApplicationReference("ApplicationReference")
            .withType(CourtApplicationType.courtApplicationType()
                    .withId(APPLICATION_ID)
                    .withType("Application to reopen case")
                    .withAppealFlag(false)
                    .build())
            .withApplicant(CourtApplicationParty.courtApplicationParty()
                    .withId(fromString("5002d600-af66-11e8-b568-0800200c9a67"))
                    .withSummonsRequired(false)
                    .withNotificationRequired(false)
                    .build())
            .withSubject(CourtApplicationParty.courtApplicationParty()
                    .withId(fromString("5002d600-af66-11e8-b568-0800200c9a68"))
                    .withSummonsRequired(false)
                    .withNotificationRequired(false)
                    .build())
            .build();

}