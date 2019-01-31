package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.queries.Defendant.defendant;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;

import uk.gov.justice.json.schemas.domains.sjp.ProsecutingAuthority;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.json.schemas.domains.sjp.query.EmployerDetails;
import uk.gov.justice.json.schemas.domains.sjp.query.PleaDetails;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.NotifiedPleaView;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.ResultingService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.NotifiedPleaViewHelper;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.ProsecutionCasesViewHelper;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProsecutionCasesDataSourcingServiceTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final String PLEA_MITIGATION = "mitigation";
    private static final ZonedDateTime DECISION_DATE = ZonedDateTime.now();
    private static final JsonEnvelope EMPTY_ENVELOPE = envelopeFrom(metadataWithRandomUUIDAndName(), JsonValue.NULL);
    private static final JsonObject PROSECUTOR = createObjectBuilder().build();
    private static final Offence DEFENDANT_OFFENCE = Offence.offence().build();
    private static final CaseDetails CASE_DETAILS = CaseDetails.caseDetails()
            .withId(CASE_ID)
            .withProsecutingAuthority(ProsecutingAuthority.TFL)
            .withDefendant(defendant()
                    .withOffences(singletonList(DEFENDANT_OFFENCE))
                    .build())
            .build();
    private static final JsonObject CASE_DECISIONS = createObjectBuilder()
            .add("caseDecisions", createArrayBuilder().add(createObjectBuilder()))
            .build();
    private static final CaseReferredForCourtHearing REFERRAL_EVEN_PAYLOAD = caseReferredForCourtHearing()
            .withReferredAt(DECISION_DATE)
            .build();
    private static final JsonObject REFERENCE_DATA_OFFENCES = createObjectBuilder().build();
    private static final EmployerDetails EMPLOYER = EmployerDetails.employerDetails().build();
    private static final JsonObject CASE_FILE_DEFENDANT_DETAILS = createCaseFileDefendantDetails();
    private static final String DEFENDANT_NATIONALITY_CODE = "defendant nationality code";
    private static final String DEFENDANT_NATIONALITY_ID = "defendant nationality id";
    private static final String DEFENDANT_ETHNICITY_CODE = "defendant ethnicity code";
    private static final String DEFENDANT_ETHNICITY_ID = "defendant ethnicity id";
    private static final JsonObject ETHNICITY = createObjectBuilder().add("id", DEFENDANT_ETHNICITY_ID).build();

    @Mock
    private ReferenceDataService referenceDataService;
    @Mock
    private ReferenceDataOffencesService referenceDataOffencesService;
    @Mock
    private SjpService sjpService;
    @Mock
    private ProsecutionCasesViewHelper prosecutionCasesViewHelper;
    @Mock
    private NotifiedPleaViewHelper notifiedPleaViewHelper;
    @Mock
    private ResultingService resultingService;

    @InjectMocks
    private ProsecutionCasesDataSourcingService prosecutionCasesDataSourcingService;

    @Before
    public void setUp() {
        when(referenceDataService.getProsecutor(ProsecutingAuthority.TFL, EMPTY_ENVELOPE)).thenReturn(PROSECUTOR);
        when(referenceDataOffencesService.getOffences(DEFENDANT_OFFENCE, EMPTY_ENVELOPE)).thenReturn(REFERENCE_DATA_OFFENCES);
        when(sjpService.getEmployerDetails(CASE_DETAILS.getDefendant().getId(), EMPTY_ENVELOPE)).thenReturn(EMPLOYER);
        when(referenceDataService.getEthnicity(DEFENDANT_ETHNICITY_CODE, EMPTY_ENVELOPE)).thenReturn(ofNullable(ETHNICITY));
        when(referenceDataService.getNationality(DEFENDANT_NATIONALITY_CODE, EMPTY_ENVELOPE))
                .thenReturn(Optional.of(createObjectBuilder().add("id", DEFENDANT_NATIONALITY_ID).build()));
        when(resultingService.getCaseDecisions(CASE_ID, EMPTY_ENVELOPE)).thenReturn(CASE_DECISIONS);
    }

    @Test
    public void shouldCreateProsecutionCaseViewsDecision() {
        final DefendantsOnlinePlea defendantPlea = DefendantsOnlinePlea.defendantsOnlinePlea()
                .withPleaDetails(PleaDetails.pleaDetails()
                        .withMitigation(PLEA_MITIGATION)
                        .build())
                .build();
        final NotifiedPleaView notifiedPleaView = new NotifiedPleaView(OFFENCE_ID, LocalDate.now(), "Plea value");
        when(notifiedPleaViewHelper.createNotifiedPleaView(REFERRAL_EVEN_PAYLOAD, singletonList(DEFENDANT_OFFENCE))).thenReturn(notifiedPleaView);

        prosecutionCasesDataSourcingService.createProsecutionCaseViews(
                CASE_DETAILS,
                REFERRAL_EVEN_PAYLOAD,
                defendantPlea,
                CASE_FILE_DEFENDANT_DETAILS,
                EMPTY_ENVELOPE);

        verify(prosecutionCasesViewHelper)
                .createProsecutionCaseViews(
                        CASE_DETAILS,
                        REFERENCE_DATA_OFFENCES,
                        PROSECUTOR,
                        createObjectBuilder().build(),
                        CASE_FILE_DEFENDANT_DETAILS,
                        EMPLOYER,
                        DEFENDANT_NATIONALITY_ID,
                        DEFENDANT_ETHNICITY_ID,
                        DECISION_DATE.toLocalDate(),
                        notifiedPleaView,
                        PLEA_MITIGATION);
    }

    @Test
    public void shouldUseNullForMitigationIfPleaNotPresent() {
        final CaseReferredForCourtHearing referralEventPayload = caseReferredForCourtHearing()
                .withReferredAt(DECISION_DATE)
                .build();

        prosecutionCasesDataSourcingService.createProsecutionCaseViews(
                CASE_DETAILS,
                referralEventPayload,
                null,
                CASE_FILE_DEFENDANT_DETAILS,
                EMPTY_ENVELOPE);

        verify(prosecutionCasesViewHelper).createProsecutionCaseViews(
                CASE_DETAILS,
                REFERENCE_DATA_OFFENCES,
                PROSECUTOR,
                createObjectBuilder().build(),
                CASE_FILE_DEFENDANT_DETAILS,
                EMPLOYER,
                DEFENDANT_NATIONALITY_ID,
                DEFENDANT_ETHNICITY_ID,
                DECISION_DATE.toLocalDate(),
                null,
                null);
    }

    @Test
    public void shouldNotGetEthnicityAndNationalityWhenCaseFileDetailsNotPresent() {
        final CaseReferredForCourtHearing referralEventPayload = caseReferredForCourtHearing()
                .withReferredAt(DECISION_DATE)
                .build();

        prosecutionCasesDataSourcingService.createProsecutionCaseViews(
                CASE_DETAILS,
                referralEventPayload,
                null,
                null,
                EMPTY_ENVELOPE);

        verify(referenceDataService, never()).getNationality(any(), any());
        verify(referenceDataService, never()).getEthnicity(any(), any());
        verify(prosecutionCasesViewHelper).createProsecutionCaseViews(
                CASE_DETAILS,
                REFERENCE_DATA_OFFENCES,
                PROSECUTOR,
                createObjectBuilder().build(),
                null,
                EMPLOYER,
                null,
                null,
                DECISION_DATE.toLocalDate(),
                null,
                null);
    }

    private static JsonObject createCaseFileDefendantDetails() {
        return createObjectBuilder()
                .add("selfDefinedInformation", createObjectBuilder()
                        .add("nationality", DEFENDANT_NATIONALITY_CODE)
                        .add("ethnicity", DEFENDANT_ETHNICITY_CODE))
                .build();
    }

}
