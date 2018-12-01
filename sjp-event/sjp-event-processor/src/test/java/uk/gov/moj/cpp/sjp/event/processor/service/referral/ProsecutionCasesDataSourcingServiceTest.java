package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import static java.util.Collections.singletonList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.json.schemas.domains.sjp.ProsecutingAuthority;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.json.schemas.domains.sjp.query.PleaDetails;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.NotifiedPleaView;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.ResultingService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.NotifiedPleaViewHelper;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.ProsecutionCasesViewHelper;

import java.time.LocalDate;
import java.time.ZonedDateTime;
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

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final String PLEA_MITIGATION = "mitigation";
    private static final ZonedDateTime DECISION_DATE = ZonedDateTime.now();
    private static final JsonEnvelope EMPTY_ENVELOPE = envelopeFrom(metadataWithRandomUUIDAndName(), JsonValue.NULL);
    private static final JsonObject PROSECUTOR = createObjectBuilder().build();
    private static final Offence DEFENDANT_OFFENCE = Offence.offence().build();
    private static final CaseDetails CASE_DETAILS = CaseDetails.caseDetails()
            .withId(CASE_ID.toString())
            .withProsecutingAuthority(ProsecutingAuthority.TFL)
            .withDefendant(Defendant.defendant()
                    .withOffences(singletonList(DEFENDANT_OFFENCE))
                    .build())
            .build();
    private static final CaseReferredForCourtHearing REFERRAL_EVEN_PAYLOAD = CaseReferredForCourtHearing.caseReferredForCourtHearing()
            .withReferredAt(DECISION_DATE)
            .build();
    private static final JsonObject REFERENCE_DATA_OFFENCES = createObjectBuilder().build();

    @Mock
    private ReferenceDataService referenceDataService;
    @Mock
    private ReferenceDataOffencesService referenceDataOffencesService;
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
    }

    @Test
    public void shouldCreateProsecutionCaseViewsDecision() {
        final DefendantsOnlinePlea defendantPlea = DefendantsOnlinePlea.defendantsOnlinePlea()
                .withPleaDetails(PleaDetails.pleaDetails()
                        .withMitigation(PLEA_MITIGATION)
                        .build())
                .build();
        final JsonObject caseDecisions = createObjectBuilder()
                .add("caseDecisions", createArrayBuilder().add(createObjectBuilder()))
                .build();

        when(resultingService.getCaseDecisions(CASE_ID.toString(), EMPTY_ENVELOPE)).thenReturn(caseDecisions);

        final NotifiedPleaView notifiedPleaView = new NotifiedPleaView(OFFENCE_ID, LocalDate.now(), "Plea value");
        when(notifiedPleaViewHelper.createNotifiedPleaView(CASE_DETAILS, REFERRAL_EVEN_PAYLOAD, defendantPlea, singletonList(DEFENDANT_OFFENCE))).thenReturn(notifiedPleaView);

        prosecutionCasesDataSourcingService.createProsecutionCaseViews(
                CASE_DETAILS,
                REFERRAL_EVEN_PAYLOAD,
                defendantPlea,
                EMPTY_ENVELOPE);

        verify(prosecutionCasesViewHelper)
                .createProsecutionCaseViews(
                        CASE_DETAILS,
                        REFERENCE_DATA_OFFENCES,
                        PROSECUTOR,
                        createObjectBuilder().build(),
                        DECISION_DATE.toLocalDate(),
                        notifiedPleaView,
                        PLEA_MITIGATION);
    }

    @Test
    public void shouldUseNullForMitigationIfPleaNotPresent() {
        final CaseReferredForCourtHearing referralEventPayload = CaseReferredForCourtHearing.caseReferredForCourtHearing()
                .withReferredAt(DECISION_DATE)
                .build();

        when(resultingService.getCaseDecisions(CASE_ID.toString(), EMPTY_ENVELOPE)).thenReturn(createObjectBuilder().add("caseDecisions", createArrayBuilder()).build());

        prosecutionCasesDataSourcingService.createProsecutionCaseViews(
                CASE_DETAILS,
                referralEventPayload,
                null,
                EMPTY_ENVELOPE);

        verify(prosecutionCasesViewHelper).createProsecutionCaseViews(
                CASE_DETAILS,
                REFERENCE_DATA_OFFENCES,
                PROSECUTOR,
                null,
                DECISION_DATE.toLocalDate(),
                null,
                null);
    }


}
