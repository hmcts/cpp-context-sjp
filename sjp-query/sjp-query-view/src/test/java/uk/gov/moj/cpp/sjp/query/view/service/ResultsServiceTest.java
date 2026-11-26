package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;
import static uk.gov.moj.cpp.sjp.query.view.util.FileUtil.getFileContentAsJson;
import static uk.gov.moj.cpp.sjp.query.view.util.FileUtil.getFileContentAsJsonArray;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.AdjournOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.ReferForCourtHearingDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;
import uk.gov.moj.cpp.sjp.persistence.entity.WithdrawOffenceDecision;
import uk.gov.moj.cpp.sjp.query.view.converter.DecisionSavedOffenceConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.ReferencedDecisionSavedOffenceConverter;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResultsServiceTest {

    @Mock
    private CaseService caseService;

    @Mock
    private EmployerService employerService;

    private OffenceHelper offenceHelper;

    private ResultsService resultsService;

    private static final UUID CASE_ID = randomUUID();
    private static final UUID SESSION_ID1 = randomUUID();
    private static final UUID SESSION_ID2 = randomUUID();

    private static final ZonedDateTime DECISION_SAVED_AT1 = ZonedDateTime.now();
    private static final ZonedDateTime DECISION_SAVED_AT2 = ZonedDateTime.now();

    private final UUID OFFENCE_ID = randomUUID();
    private final UUID OFFENCE_ID2 = randomUUID();


    private final UUID DECISION_ID1 = randomUUID();
    private final UUID DECISION_ID2 = randomUUID();

    private JsonEnvelope resultedCaseEventForAdjourn;
    private JsonEnvelope resultedCaseEventForWithdraw;
    private JsonEnvelope resultedCaseEventForReferForCourtHearing;

    private static final UUID defendantId = randomUUID();

    private final String postcode = "CR0 2GE";
    private final String ljaNationalCourtCode = "255";

    @Mock
    private ReferenceDataService referenceDataService;

    private CaseView caseView;
    private Employer employer;

    MetadataBuilder metadataBuilder;

    @BeforeEach
    public void setup() {

        final DecisionSavedOffenceConverter decisionSavedOffenceConverter = new DecisionSavedOffenceConverter();

        final ReferencedDecisionSavedOffenceConverter referencedDecisionSavedOffenceConverter
                = new ReferencedDecisionSavedOffenceConverter(referenceDataService);
        offenceHelper = new OffenceHelper(referenceDataService);

        resultsService = new ResultsService(
                caseService,
                referenceDataService,
                decisionSavedOffenceConverter,
                referencedDecisionSavedOffenceConverter,
                offenceHelper,
                employerService);

        createEmployer();

        metadataBuilder = metadataWithRandomUUID("sjp.events.case-completed");

        resultedCaseEventForAdjourn = createResultedCaseEventForAdjourn(metadataBuilder);
        resultedCaseEventForWithdraw = createResultedCaseEventForWithdraw(metadataBuilder);
        resultedCaseEventForReferForCourtHearing = createResultedCaseEventForReferForCourtHearing(metadataBuilder);

        final JsonArray jsonArray =  getFileContentAsJsonArray("case-results-tests/referencedata.query.results.json", new HashMap<>());
        when(referenceDataService.getResultIds(ArgumentMatchers.isA(JsonEnvelope.class))).thenReturn(jsonArray.getValuesAs(JsonObject.class));
    }

    @Test
    public void shouldConvertWithdrawToCaseResulted() {
        final JsonArray withdrawalRequestReasons =  getFileContentAsJsonArray("case-results-tests/referencedata.offence-withdraw-request-reasons.json", new HashMap<>());
        when(referenceDataService.getWithdrawalReasons(ArgumentMatchers.isA(JsonEnvelope.class))).thenReturn(withdrawalRequestReasons.getValuesAs(JsonObject.class));
        createCaseView(asList(getAdjournOffenceDecision(), getWithDrawCaseDecision()));

        when(caseService.findCase(CASE_ID)).thenReturn(caseView);
        when(employerService.getEmployer(defendantId)).thenReturn(ofNullable(employer));


        final JsonObject enforcementArea = createObjectBuilder().add("accountDivisionCode", 77).add("enforcingCourtCode", 828).build();
        when(referenceDataService.getEnforcementAreaByPostcode(any(), any())).thenReturn(Optional.of(enforcementArea));

        // find case results
        final JsonEnvelope envelope = envelopeFrom(metadataFrom(metadataBuilder.build()), createObjectBuilder()
                .add("caseId", CASE_ID.toString()));
        final JsonObject caseResults = resultsService.findCaseResults(envelope);

        assertThat(caseResults.getJsonArray("caseDecisions").getJsonObject(0).getJsonArray("offences").toString(),
                is(resultedCaseEventForAdjourn.payloadAsJsonObject().getJsonArray("offences").toString()));

        assertThat(caseResults.getJsonArray("caseDecisions").getJsonObject(1).getJsonArray("offences").toString(),
                is(resultedCaseEventForWithdraw.payloadAsJsonObject().getJsonArray("offences").toString()));

    }

    @Test
    public void shouldConvertReferForCourtHearingToCaseResults() {
        createCaseView(asList(getReferForCourtHearingDecision()));

        when(caseService.findCase(CASE_ID)).thenReturn(caseView);
        when(employerService.getEmployer(defendantId)).thenReturn(ofNullable(employer));


        final JsonObject enforcementArea = createObjectBuilder().add("accountDivisionCode", 77).add("enforcingCourtCode", 828).build();
        when(referenceDataService.getEnforcementAreaByPostcode(any(), any())).thenReturn(Optional.of(enforcementArea));

        // find case results
        final JsonEnvelope envelope = envelopeFrom(metadataFrom(metadataBuilder.build()), createObjectBuilder()
                .add("caseId", CASE_ID.toString()));
        final JsonObject caseResults = resultsService.findCaseResults(envelope);

        assertThat(caseResults.getJsonArray("caseDecisions").getJsonObject(0).getJsonArray("offences").toString(),
                is(resultedCaseEventForReferForCourtHearing.payloadAsJsonObject().getJsonArray("offences").toString()));

    }

    private JsonEnvelope createResultedCaseEventForWithdraw(MetadataBuilder metadataBuilder) {
        return envelopeFrom(metadataBuilder,
                getFileContentAsJson("case-results-tests/case-resulted-event-for-withdraw.json",
                        ImmutableMap.<String, Object>builder()
                                .put("resultedOn", DECISION_SAVED_AT2)
                                .put("offenceId", OFFENCE_ID)
                                .build()));
    }

    private JsonEnvelope createResultedCaseEventForAdjourn(MetadataBuilder metadataBuilder) {
        return envelopeFrom(metadataBuilder,
                getFileContentAsJson("case-results-tests/case-resulted-event-for-adjourn.json",
                        ImmutableMap.<String, Object>builder()
                                .put("resultedOn", DECISION_SAVED_AT1)
                                .put("offenceId", OFFENCE_ID)
                                .build()));
    }

    private JsonEnvelope createResultedCaseEventForReferForCourtHearing(MetadataBuilder metadataBuilder) {
        return envelopeFrom(metadataBuilder,
                getFileContentAsJson("case-results-tests/case-resulted-event-for-refer-for-court-hearing.json",
                        ImmutableMap.<String, Object>builder()
                                .put("resultedOn", DECISION_SAVED_AT1)
                                .put("offenceId", OFFENCE_ID)
                                .put("offenceId2", OFFENCE_ID2)
                                .build()));
    }


    private void createEmployer() {
        uk.gov.moj.cpp.sjp.domain.Address address = new uk.gov.moj.cpp.sjp.domain.Address("14 Tottenham Court Road", "London", "England", "UK", "Greater London", postcode);
        employer = new Employer(defendantId, "McDonald's", "12345", "020 7998 9300", address);
    }

    private void createCaseView(final List<CaseDecision> caseDecisionList) {

        final OffenceDetail offenceDetail1 = OffenceDetail.builder()
                .setId(OFFENCE_ID)
                .setCode("PS90010")
                .setSequenceNumber(1)
                .setPlea(NOT_GUILTY)
                .setConviction(PROVED_SJP)
                .setWording("On 02/07/2015 At Threadneedle Street EC2 Being a passenger on a Public service Vehicle operated on behalf of London Bus Services Limited being used for the carriage of passengers at separate fares where the vehicle was being operated by a Driver without a Conductor did not as directed by the Driver an Inspector or a Notice displayed on the vehicle pay the fare for the journey in accordance with the direction ")
                .setWordingWelsh("Welsh wording: On 02/07/2015 At Threadneedle Street EC2 Being a passenger on a Public service Vehicle operated on behalf of London Bus Services Limited being used for the carriage of passengers at separate fares where the vehicle was being operated by a Driver without a Conductor did not as directed by the Driver an Inspector or a Notice displayed on the vehicle pay the fare for the journey in accordance with the direction ")
                .setStartDate(LocalDate.now().minusDays(1))
                .setChargeDate(LocalDate.now().plusDays(1))
                .withCompensation(BigDecimal.valueOf(10))
                .withProsecutionFacts("An incident took place at GREEN PARK station whereby you were spoken to by a member of London Underground staff regarding your train journey and the associated fare.The facts of this incidents are now being considered and I must advise you that legal proceedings may be initiated against you regarding this matter in accordance with the LU prosecution policy")
                .build();
        final OffenceDetail offenceDetail2 = OffenceDetail.builder()
                .setId(OFFENCE_ID2)
                .setCode("PS90011")
                .setSequenceNumber(2)
                .setPlea(NOT_GUILTY)
                .setConviction(PROVED_SJP)
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
                "DriverLicense",
                null);

        DefendantDetail defendantDetail = new DefendantDetail(defendantId);
        defendantDetail.setPersonalDetails(personalDetails);
        defendantDetail.setAddress(address);

        defendantDetail.setOffences(asList(offenceDetail1,offenceDetail2));

        CaseDetail caseDetail = new CaseDetail(CASE_ID);
        caseDetail.setUrn("TFL75947ZQ8UE");
        caseDetail.setDateTimeCreated(ZonedDateTime.now());
        caseDetail.setProsecutingAuthority("DVLA");
        caseDetail.setCompleted(false);
        caseDetail.setAssigneeId(null);
        caseDetail.setCosts(BigDecimal.valueOf(20));
        caseDetail.setPostingDate(LocalDate.now());
        caseDetail.setEnterpriseId("FNHMNHBQNV7L");
        caseDetail.setCaseStatus(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION);
        caseDetail.setListedInCriminalCourts(true);

        caseDetail.setProsecutingAuthority("DVLA");
        caseDetail.setDefendant(defendantDetail);

        caseDetail.setCaseDecisions(caseDecisionList);

        JsonObject prosecutorPayload = createObjectBuilder()
                .add("fullName", "DVLA")
                .add("policeFlag", false)
                .build();

        caseView = new CaseView(caseDetail, prosecutorPayload);
    }

    private CaseDecision getAdjournOffenceDecision() {
        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setId(DECISION_ID1);
        caseDecision.setSavedAt(DECISION_SAVED_AT1);
        caseDecision.setSession(new Session(SESSION_ID1, null, null, null, ljaNationalCourtCode, null, null));
        caseDecision.setCaseId(CASE_ID);
        OffenceDecision adjournOffenceDecision = new AdjournOffenceDecision(OFFENCE_ID, DECISION_ID1, "No sufficient information yet", LocalDate.of(2020, 02, 18), FOUND_NOT_GUILTY, null, null);
        caseDecision.setOffenceDecisions(asList(adjournOffenceDecision));
        return caseDecision;
    }

    private CaseDecision getReferForCourtHearingDecision() {
        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setId(DECISION_ID1);
        caseDecision.setSavedAt(DECISION_SAVED_AT1);
        caseDecision.setSession(new Session(SESSION_ID1, null, null, null, ljaNationalCourtCode, null, null));
        caseDecision.setCaseId(CASE_ID);


        final OffenceDecision referredToOpenCourt = new ReferForCourtHearingDecision(
                OFFENCE_ID,
                DECISION_ID1,
                UUID.fromString("809f7aac-d285-43a5-9fb1-3a894db71530"),
                10,
                "",
                NO_VERDICT,
                null, null);
        final OffenceDecision referredToOpenCourt2 = new ReferForCourtHearingDecision(
                OFFENCE_ID2,
                DECISION_ID2,
                UUID.fromString("809f7aac-d285-43a5-9fb1-3a894db71530"),
                10,
                "",
                null,
                null, null);
        caseDecision.setOffenceDecisions(asList(referredToOpenCourt,referredToOpenCourt2));
        return caseDecision;
    }

    private CaseDecision getWithDrawCaseDecision() {
        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setId(DECISION_ID2);
        caseDecision.setSavedAt(DECISION_SAVED_AT2);
        caseDecision.setSession(new Session(SESSION_ID2, null, null, null, ljaNationalCourtCode, null, null));
        caseDecision.setCaseId(CASE_ID);
        final WithdrawOffenceDecision withdrawOffenceDecision = new WithdrawOffenceDecision(OFFENCE_ID, DECISION_ID2, UUID.fromString("030d4335-f9fe-39e0-ad7e-d01a0791ff87"), FOUND_NOT_GUILTY, null);
        caseDecision.setOffenceDecisions(asList(withdrawOffenceDecision));
        return caseDecision;
    }
}
