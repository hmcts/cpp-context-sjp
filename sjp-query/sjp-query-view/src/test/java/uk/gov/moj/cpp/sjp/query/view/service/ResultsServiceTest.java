package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.query.view.util.FileUtil.getFileContentAsJson;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DismissOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;
import uk.gov.moj.cpp.sjp.query.view.converter.DecisionSavedOffenceConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.ReferencedDecisionSavedOffenceConverter;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.response.OffenceDecisionView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResultsServiceTest {

    @Mock
    private CaseService caseService;

    @Mock
    private DecisionSavedOffenceConverter decisionSavedOffenceConverter;

    @Mock
    private ReferencedDecisionSavedOffenceConverter referencedDecisionSavedOffenceConverter;

    @Mock
    private EmployerService employerService;

    @Mock
    private OffenceHelper offenceHelper;

    @InjectMocks
    private ResultsService resultsService;

    private static final UUID CASE_ID = randomUUID();
    private static final UUID SESSION_ID = randomUUID();
    private static final UUID RESULT_TYPE_ID = randomUUID();
    private static final UUID RESULT_DEFINITION_ID = randomUUID();
    private static final ZonedDateTime DECISION_SAVED_AT = ZonedDateTime.now();
    private final UUID OFFENCE_ID = randomUUID();
    private final UUID DECISION_ID = randomUUID();
    private JsonEnvelope decisionSavedEventForDismiss;
    private JsonEnvelope referencedDecisionSavedEventForDismiss;
    private JsonEnvelope resultedCaseEventForDismiss;

    private static final UUID defendantId = randomUUID();

    private final String postcode = "CR0 2GE";
    private final String ljaNationalCourtCode = "255";

    @Mock
    private ReferenceDataService referenceDataService;

    private CaseView caseView;
    private Employer employer;

    MetadataBuilder metadataBuilder;

    @Before
    public void setup() {
        createCaseView();
        createEmployer();

        metadataBuilder = metadataWithRandomUUID("sjp.events.case-completed");

        decisionSavedEventForDismiss = createDecisionSavedEventForDismiss(metadataBuilder);

        referencedDecisionSavedEventForDismiss = createReferencedDecisionSavedEventForDismiss(metadataBuilder);

        resultedCaseEventForDismiss = createResultedCaseEventForDismiss(metadataBuilder);
    }

    @Test
    public void shouldConvertDismissToCaseResulted() {

        when(caseService.findCase(CASE_ID)).thenReturn(caseView);
        when(employerService.getEmployer(defendantId)).thenReturn(ofNullable(employer));

        final OffenceDecisionView offenceDecisionView = caseView.getCaseDecisions().get(0).getOffenceDecisions().get(0);

        final JsonEnvelope envelope = envelopeFrom(metadataFrom(metadataBuilder.build()), createObjectBuilder()
                .add("caseId", CASE_ID.toString()));

        when(decisionSavedOffenceConverter.convertOffenceDecision(offenceDecisionView))
                .thenReturn(decisionSavedEventForDismiss.payloadAsJsonObject().getJsonArray("offenceDecisions").getJsonObject(0));

        final JsonArray offences = referencedDecisionSavedEventForDismiss.payloadAsJsonObject().getJsonArray("offences");

        when(referencedDecisionSavedOffenceConverter.convertOffenceDecisions(any()))
                .thenReturn(offences);

        final JsonObject enforcementArea = createObjectBuilder().add("accountDivisionCode", 77).add("enforcingCourtCode", 828).build();

        when(referenceDataService.getEnforcementAreaByPostcode(any(), any())).thenReturn(Optional.of(enforcementArea));
        when(referenceDataService.getEnforcementAreaByLocalJusticeAreaNationalCourtCode(any(), any())).thenReturn(Optional.of(enforcementArea));

        when(offenceHelper.populateOffences(any(), any(), any())).thenReturn(resultedCaseEventForDismiss.payloadAsJsonObject().getJsonArray("offences"));

        final JsonObject caseResults = resultsService.findCaseResults(envelope);

        assertThat(caseResults.getJsonArray("caseDecisions").getJsonObject(0).getJsonArray("offences").toString(), is(resultedCaseEventForDismiss.payloadAsJsonObject().getJsonArray("offences").toString()));
    }

    private JsonEnvelope createResultedCaseEventForDismiss(MetadataBuilder metadataBuilder) {
        return envelopeFrom(metadataBuilder,
                getFileContentAsJson("case-results-tests/case-resulted-event-for-dismiss.json",
                        ImmutableMap.<String, Object>builder()
                                .put("resultedOn", DECISION_SAVED_AT)
                                .put("offenceId", OFFENCE_ID)
                                .put("resultDefinitionId", RESULT_DEFINITION_ID)
                                .build()));
    }

    private JsonEnvelope createReferencedDecisionSavedEventForDismiss(MetadataBuilder metadataBuilder) {
        return envelopeFrom(metadataBuilder,
                getFileContentAsJson("case-results-tests/referenced-decision-saved-event-for-dismiss.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION_ID)
                                .put("resultedOn", DECISION_SAVED_AT)
                                .put("offenceId", OFFENCE_ID)
                                .put("accountDivisionCode", 77)
                                .put("enforcingCourtCode", 828)
                                .put("resultTypeId", RESULT_TYPE_ID)
                                .build()));
    }

    private JsonEnvelope createDecisionSavedEventForDismiss(MetadataBuilder metadataBuilder) {
        return envelopeFrom(metadataBuilder,
                getFileContentAsJson("case-results-tests/decision-saved-event-for-dismiss.json",
                        ImmutableMap.<String, Object>builder()
                                .put("caseId", CASE_ID)
                                .put("sessionId", SESSION_ID)
                                .put("decisionId", DECISION_ID)
                                .put("resultedOn", DECISION_SAVED_AT)
                                .put("offenceId", OFFENCE_ID)
                                .build()));
    }

    private void createEmployer() {
        uk.gov.moj.cpp.sjp.domain.Address address = new uk.gov.moj.cpp.sjp.domain.Address("14 Tottenham Court Road", "London", "England", "UK", "Greater London", postcode);
        employer = new Employer(defendantId, "McDonald's", "12345", "020 7998 9300", address);
    }

    private void createCaseView() {

        final OffenceDetail offenceDetail1 = OffenceDetail.builder()
                .setId(OFFENCE_ID)
                .setCode("PS90010")
                .setSequenceNumber(1)
                .setPlea(NOT_GUILTY)
                .setWording("On 02/07/2015 At Threadneedle Street EC2 Being a passenger on a Public service Vehicle operated on behalf of London Bus Services Limited being used for the carriage of passengers at separate fares where the vehicle was being operated by a Driver without a Conductor did not as directed by the Driver an Inspector or a Notice displayed on the vehicle pay the fare for the journey in accordance with the direction ")
                .setWordingWelsh("Welsh wording: On 02/07/2015 At Threadneedle Street EC2 Being a passenger on a Public service Vehicle operated on behalf of London Bus Services Limited being used for the carriage of passengers at separate fares where the vehicle was being operated by a Driver without a Conductor did not as directed by the Driver an Inspector or a Notice displayed on the vehicle pay the fare for the journey in accordance with the direction ")
                .setStartDate(LocalDate.now().minusDays(1))
                .setChargeDate(LocalDate.now().plusDays(1))
                .withCompensation(BigDecimal.valueOf(10))
                .withProsecutionFacts("An incident took place at GREEN PARK station whereby you were spoken to by a member of London Underground staff regarding your train journey and the associated fare.The facts of this incidents are now being considered and I must advise you that legal proceedings may be initiated against you regarding this matter in accordance with the LU prosecution policy")
                .build();

        Address address = new Address("14 Tottenham Court Road", "London", "England", "UK", "Greater London", postcode);
        PersonalDetails personalDetails = new PersonalDetails("title",
                "McDonald's",
                "lastName",
                LocalDate.now().minusYears(20),
                Gender.MALE,
                "NINO",
                address,
                null,
                null);

        DefendantDetail defendantDetail = new DefendantDetail(defendantId);
        defendantDetail.setPersonalDetails(personalDetails);

        defendantDetail.setOffences(asList(offenceDetail1));

        CaseDetail caseDetail = new CaseDetail(CASE_ID);
        caseDetail.setUrn("TFL75947ZQ8UE");
        caseDetail.setDateTimeCreated(ZonedDateTime.now());
        caseDetail.setProsecutingAuthority(ProsecutingAuthority.DVLA);
        caseDetail.setCompleted(false);
        caseDetail.setAssigneeId(null);
        caseDetail.setCosts(BigDecimal.valueOf(20));
        caseDetail.setPostingDate(LocalDate.now());
        caseDetail.setEnterpriseId("FNHMNHBQNV7L");
        caseDetail.setCaseStatus(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION);
        caseDetail.setListedInCriminalCourts(true);

        caseDetail.setProsecutingAuthority(ProsecutingAuthority.DVLA);
        caseDetail.setDefendant(defendantDetail);

        CaseDecision caseDecision = new CaseDecision();
        caseDecision.setId(DECISION_ID);
        caseDecision.setSavedAt(DECISION_SAVED_AT);

        caseDecision.setSession(new Session(SESSION_ID, null, null, null, ljaNationalCourtCode, null, null));
        caseDecision.setFinancialImposition(null);
        caseDecision.setCaseId(CASE_ID);

        OffenceDecision dismissOffenceDecision = new DismissOffenceDecision(OFFENCE_ID, DECISION_ID, FOUND_NOT_GUILTY);
        caseDecision.setOffenceDecisions(asList(dismissOffenceDecision));

        caseDetail.setCaseDecisions(asList(caseDecision));

        caseView = new CaseView(caseDetail, "DVLA");
    }
}
