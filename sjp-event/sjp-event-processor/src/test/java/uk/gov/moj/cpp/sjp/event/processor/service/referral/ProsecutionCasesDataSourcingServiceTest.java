package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.queries.Defendant.defendant;
import static uk.gov.justice.json.schemas.domains.sjp.queries.OnlinePleaDetail.onlinePleaDetail;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.json.schemas.domains.sjp.queries.Session;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.json.schemas.domains.sjp.query.EmployerDetails;
import uk.gov.justice.json.schemas.domains.sjp.query.PleaDetails;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.ProsecutionCasesViewHelper;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonObject;
import javax.json.JsonValue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCasesDataSourcingServiceTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID OFFENCE_ID1 = randomUUID();
    private static final UUID OFFENCE_ID2 = randomUUID();
    private static final UUID OFFENCE_ID3 = randomUUID();
    private static final String TFL = "TFL";
    private static final String PLEA_MITIGATION = "mitigation";
    private static final ZonedDateTime DECISION_DATE = ZonedDateTime.now();
    private static final JsonEnvelope EMPTY_ENVELOPE = envelopeFrom(metadataWithRandomUUIDAndName(), JsonValue.NULL);
    private static final JsonObject PROSECUTOR = createObjectBuilder().build();
    private static final List<Offence> offences = createOffences(OFFENCE_ID1, OFFENCE_ID2, OFFENCE_ID3);
    private static final CaseDetails CASE_DETAILS = CaseDetails.caseDetails()
            .withId(CASE_ID)
            .withProsecutingAuthority(TFL)
            .withDefendant(defendant()
                    .withOffences(offences)
                    .build())
            .build();

    private static final CaseDecision CASE_DECISION = createCaseDecision();
    private static final String COURT_HOUSE_NAME = "Court house name";
    private static final String COURT_HOUSE_CODE = "Court house code";
    private static final String MAGISTRATE_NAME = "magistrate name";
    private static final CaseReferredForCourtHearing REFERRAL_EVEN_PAYLOAD = caseReferredForCourtHearing()
            .withReferredAt(DECISION_DATE)
            .withReferredOffences(createDecisionInformationList(OFFENCE_ID1, OFFENCE_ID2))
            .build();
    private static final EmployerDetails EMPLOYER = EmployerDetails.employerDetails().build();

    private static final JsonObject CASE_FILE_DETAILS = createCaseFileDetails();

    private static final JsonObject CASE_FILE_DEFENDANT_DETAILS = createCaseFileDefendantDetails();
    private static final String DEFENDANT_NATIONALITY_CODE = "defendant nationality code";
    private static final String DEFENDANT_NATIONALITY_ID = "defendant nationality id";
    private static final String DEFENDANT_ETHNICITY_CODE = "defendant ethnicity code";
    private static final String DEFENDANT_ETHNICITY_ID = "defendant ethnicity id";
    private static final JsonObject ETHNICITY = createObjectBuilder().add("id", DEFENDANT_ETHNICITY_ID).build();
    private static final String OFFENCE_CJS_CODE = "offence CJS code";
    private static final String OFFENCE_MAX_PENALTY = "max penalty";
    private static final UUID OFFENCE_DEFINITION_ID = randomUUID();

    @Mock
    private ReferenceDataService referenceDataService;
    @Mock
    private SjpService sjpService;
    @Mock
    private ProsecutionCasesViewHelper prosecutionCasesViewHelper;
    @Mock
    private ReferenceDataOffencesService referenceDataOffencesService;

    @InjectMocks
    private ProsecutionCasesDataSourcingService prosecutionCasesDataSourcingService;

    @Test
    public void shouldCreateProsecutionCaseViewsDecision() {
        when(referenceDataService.getProsecutors(TFL, EMPTY_ENVELOPE)).thenReturn(PROSECUTOR);
        when(referenceDataOffencesService.getOffenceDefinitionByOffenceCode(mockOffenceCodes(), DECISION_DATE.toLocalDate(), EMPTY_ENVELOPE)).thenReturn(mockCJSOffenceCodeToOffenceDefinition());
        when(sjpService.getEmployerDetails(CASE_DETAILS.getDefendant().getId(), EMPTY_ENVELOPE)).thenReturn(EMPLOYER);
        when(referenceDataService.getEthnicity(DEFENDANT_ETHNICITY_CODE, EMPTY_ENVELOPE)).thenReturn(ofNullable(ETHNICITY));
        when(referenceDataService.getNationality(DEFENDANT_NATIONALITY_CODE, EMPTY_ENVELOPE))
                .thenReturn(Optional.of(createObjectBuilder().add("id", DEFENDANT_NATIONALITY_ID).build()));

        final DefendantsOnlinePlea defendantPlea = DefendantsOnlinePlea.defendantsOnlinePlea()
                .withPleaDetails(PleaDetails.pleaDetails()
                        .build())
                .withOnlinePleaDetails(singletonList(onlinePleaDetail().withMitigation(PLEA_MITIGATION).build()))
                .build();
        prosecutionCasesDataSourcingService.createProsecutionCaseViews(
                CASE_DETAILS,
                CASE_DECISION,
                REFERRAL_EVEN_PAYLOAD.getReferredOffences(),
                REFERRAL_EVEN_PAYLOAD.getReferredAt(),
                REFERRAL_EVEN_PAYLOAD.getDefendantCourtOptions(),
                REFERRAL_EVEN_PAYLOAD.getConvictionDate(),
                REFERRAL_EVEN_PAYLOAD.getConvictingCourt(),
                defendantPlea,
                CASE_FILE_DETAILS,
                CASE_FILE_DEFENDANT_DETAILS,
                EMPTY_ENVELOPE);

        verify(prosecutionCasesViewHelper)
                .createProsecutionCaseViews(
                        CASE_DETAILS,
                        CASE_DECISION,
                        PROSECUTOR,
                        CASE_FILE_DETAILS,
                        CASE_FILE_DEFENDANT_DETAILS,
                        EMPLOYER,
                        DEFENDANT_NATIONALITY_ID,
                        DEFENDANT_ETHNICITY_ID,
                        REFERRAL_EVEN_PAYLOAD.getReferredAt(),
                        REFERRAL_EVEN_PAYLOAD.getDefendantCourtOptions(),
                        REFERRAL_EVEN_PAYLOAD.getConvictionDate(),
                        REFERRAL_EVEN_PAYLOAD.getConvictingCourt(),
                        PLEA_MITIGATION,
                        mockCJSOffenceCodeToOffenceDefinition(),
                        createOffences(OFFENCE_ID1, OFFENCE_ID2), EMPTY_ENVELOPE);
    }

    @Test
    public void shouldUseNullForMitigationIfPleaNotPresent() {
        when(referenceDataService.getProsecutors(TFL, EMPTY_ENVELOPE)).thenReturn(PROSECUTOR);
        when(referenceDataOffencesService.getOffenceDefinitionByOffenceCode(mockOffenceCodes(), DECISION_DATE.toLocalDate(), EMPTY_ENVELOPE)).thenReturn(mockCJSOffenceCodeToOffenceDefinition());
        when(sjpService.getEmployerDetails(CASE_DETAILS.getDefendant().getId(), EMPTY_ENVELOPE)).thenReturn(EMPLOYER);
        when(referenceDataService.getEthnicity(DEFENDANT_ETHNICITY_CODE, EMPTY_ENVELOPE)).thenReturn(ofNullable(ETHNICITY));
        when(referenceDataService.getNationality(DEFENDANT_NATIONALITY_CODE, EMPTY_ENVELOPE))
                .thenReturn(Optional.of(createObjectBuilder().add("id", DEFENDANT_NATIONALITY_ID).build()));

        prosecutionCasesDataSourcingService.createProsecutionCaseViews(
                CASE_DETAILS,
                CASE_DECISION,
                REFERRAL_EVEN_PAYLOAD.getReferredOffences(),
                REFERRAL_EVEN_PAYLOAD.getReferredAt(),
                REFERRAL_EVEN_PAYLOAD.getDefendantCourtOptions(),
                REFERRAL_EVEN_PAYLOAD.getConvictionDate(),
                REFERRAL_EVEN_PAYLOAD.getConvictingCourt(),
                null,
                CASE_FILE_DETAILS,
                CASE_FILE_DEFENDANT_DETAILS,
                EMPTY_ENVELOPE);

        verify(prosecutionCasesViewHelper).createProsecutionCaseViews(
                CASE_DETAILS,
                CASE_DECISION,
                PROSECUTOR,
                CASE_FILE_DETAILS,
                CASE_FILE_DEFENDANT_DETAILS,
                EMPLOYER,
                DEFENDANT_NATIONALITY_ID,
                DEFENDANT_ETHNICITY_ID,
                REFERRAL_EVEN_PAYLOAD.getReferredAt(),
                REFERRAL_EVEN_PAYLOAD.getDefendantCourtOptions(),
                REFERRAL_EVEN_PAYLOAD.getConvictionDate(),
                REFERRAL_EVEN_PAYLOAD.getConvictingCourt(),
                null,
                mockCJSOffenceCodeToOffenceDefinition(),
                createOffences(OFFENCE_ID1, OFFENCE_ID2), EMPTY_ENVELOPE);
    }

    @Test
    public void shouldNotGetEthnicityAndNationalityWhenCaseFileDetailsNotPresent() {
        when(referenceDataService.getProsecutors(TFL, EMPTY_ENVELOPE)).thenReturn(PROSECUTOR);
        when(referenceDataOffencesService.getOffenceDefinitionByOffenceCode(mockOffenceCodes(), DECISION_DATE.toLocalDate(), EMPTY_ENVELOPE)).thenReturn(mockCJSOffenceCodeToOffenceDefinition());
        when(sjpService.getEmployerDetails(CASE_DETAILS.getDefendant().getId(), EMPTY_ENVELOPE)).thenReturn(EMPLOYER);

        prosecutionCasesDataSourcingService.createProsecutionCaseViews(
                CASE_DETAILS,
                CASE_DECISION,
                REFERRAL_EVEN_PAYLOAD.getReferredOffences(),
                REFERRAL_EVEN_PAYLOAD.getReferredAt(),
                REFERRAL_EVEN_PAYLOAD.getDefendantCourtOptions(),
                REFERRAL_EVEN_PAYLOAD.getConvictionDate(),
                REFERRAL_EVEN_PAYLOAD.getConvictingCourt(),
                null,
                CASE_FILE_DETAILS,
                null,
                EMPTY_ENVELOPE);

        verify(referenceDataService, never()).getNationality(any(), any());
        verify(referenceDataService, never()).getEthnicity(any(), any());
        verify(prosecutionCasesViewHelper).createProsecutionCaseViews(
                CASE_DETAILS,
                CASE_DECISION,
                PROSECUTOR,
                CASE_FILE_DETAILS,
                null,
                EMPLOYER,
                null,
                null,
                REFERRAL_EVEN_PAYLOAD.getReferredAt(),
                REFERRAL_EVEN_PAYLOAD.getDefendantCourtOptions(),
                REFERRAL_EVEN_PAYLOAD.getConvictionDate(),
                REFERRAL_EVEN_PAYLOAD.getConvictingCourt(),
                null,
                mockCJSOffenceCodeToOffenceDefinition(),
                createOffences(OFFENCE_ID1, OFFENCE_ID2), EMPTY_ENVELOPE);
    }

    private static JsonObject createCaseFileDefendantDetails() {
        return createObjectBuilder()
                .add("selfDefinedInformation", createObjectBuilder()
                        .add("nationality", DEFENDANT_NATIONALITY_CODE)
                        .add("ethnicity", DEFENDANT_ETHNICITY_CODE))
                .build();
    }

    private static JsonObject createCaseFileDetails() {
        return createObjectBuilder()
                .add("defendants", createArrayBuilder().add(createCaseFileDefendantDetails()))
                .build();
    }

    private static List<Offence> createOffences(final UUID... ids) {
        return Arrays.stream(ids)
                .map(id -> Offence.offence().withId(id).withCjsCode(OFFENCE_CJS_CODE).build())
                .collect(Collectors.toList());
    }

    private static List<OffenceDecisionInformation> createDecisionInformationList(final UUID... ids) {
        return Arrays.stream(ids)
                .map(id -> OffenceDecisionInformation.createOffenceDecisionInformation(id, VerdictType.NO_VERDICT))
                .collect(Collectors.toList());
    }

    private static Map<String, JsonObject> mockCJSOffenceCodeToOffenceDefinition() {
        JsonObject offenceDefinition = createObjectBuilder().add("offences", createArrayBuilder()
                .add(createObjectBuilder()
                        .add("cjsoffencecode", OFFENCE_CJS_CODE)
                        .add("offenceId", OFFENCE_DEFINITION_ID.toString())
                        .add("maxPenalty", OFFENCE_MAX_PENALTY
                        ))).build();
        return ImmutableMap.of(OFFENCE_CJS_CODE, offenceDefinition);
    }

    private static Set<String> mockOffenceCodes() {
        return Sets.newHashSet(OFFENCE_CJS_CODE);
    }

    private static CaseDecision createCaseDecision() {
        return CaseDecision.caseDecision()
                .withSession(Session.session()
                        .withCourtHouseName(COURT_HOUSE_NAME)
                        .withCourtHouseCode(COURT_HOUSE_CODE)
                        .withMagistrate(MAGISTRATE_NAME)
                        .build())
                .build();
    }
}
