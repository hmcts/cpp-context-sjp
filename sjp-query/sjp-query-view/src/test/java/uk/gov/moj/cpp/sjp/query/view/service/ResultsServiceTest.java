package uk.gov.moj.cpp.sjp.query.view.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.SETASIDE;
import static uk.gov.moj.cpp.sjp.query.view.util.FileUtil.getFileContentAsJson;

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
import uk.gov.moj.cpp.sjp.persistence.entity.SetAsideOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.WithdrawOffenceDecision;
import uk.gov.moj.cpp.sjp.query.view.converter.DecisionSavedOffenceConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.ReferencedDecisionSavedOffenceConverter;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.util.fakes.FakeReferenceDataService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResultsServiceTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID SESSION_ID1 = randomUUID();
    private static final UUID SESSION_ID2 = randomUUID();
    private static final ZonedDateTime DECISION_SAVED_AT1 = ZonedDateTime.now();
    private static final ZonedDateTime DECISION_SAVED_AT2 = ZonedDateTime.now();
    private static final UUID OFFENCE_ID_1 = randomUUID();
    private static final UUID OFFENCE_ID_2 = randomUUID();
    private static final UUID DECISION_ID_1 = randomUUID();
    private static final UUID DECISION_ID_2 = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final String POSTCODE = "CR0 2GE";
    private static final String LJA_NATIONAL_COURT_CODE = "255";

    @Mock
    private CaseService caseService;
    @Mock
    private EmployerService employerService;


    private ResultsService resultsService;
    private JsonEnvelope resultedCaseEventForAdjourn;
    private JsonEnvelope resultedCaseEventForWithdraw;
    private JsonEnvelope resultedCaseEventForReferForCourtHearing;
    private CaseView caseView;
    private Employer employer;
    private MetadataBuilder metadataBuilder;

    @Before
    public void setup() {
        final FakeReferenceDataService referenceDataService = setUpFakeReferenceDataService();
        resultsService = new ResultsService(
                caseService,
                referenceDataService,
                new DecisionSavedOffenceConverter(),
                new ReferencedDecisionSavedOffenceConverter(referenceDataService),
                new OffenceHelper(referenceDataService),
                employerService);

        createEmployer();

        metadataBuilder = metadataWithRandomUUID("sjp.events.case-completed");
        resultedCaseEventForAdjourn = createResultedCaseEventForAdjourn(metadataBuilder);
        resultedCaseEventForWithdraw = createResultedCaseEventForWithdraw(metadataBuilder);
        resultedCaseEventForReferForCourtHearing = createResultedCaseEventForReferForCourtHearing(metadataBuilder);

        when(employerService.getEmployer(DEFENDANT_ID)).thenReturn(ofNullable(employer));
    }

    @Test
    public void shouldConvertWithdrawToCaseResulted() {
        createCaseView(asList(createAdjournOffenceDecision(), getWithDrawCaseDecision()));

        final JsonObject caseResults = resultsService.findCaseResults(envelope());

        assertThat(caseResults.getJsonArray("caseDecisions").getJsonObject(0).getJsonArray("offences").toString(),
                is(resultedCaseEventForAdjourn.payloadAsJsonObject().getJsonArray("offences").toString()));
        assertThat(caseResults.getJsonArray("caseDecisions").getJsonObject(1).getJsonArray("offences").toString(),
                is(resultedCaseEventForWithdraw.payloadAsJsonObject().getJsonArray("offences").toString()));
    }

    @Test
    public void shouldConvertReferForCourtHearingToCaseResults() {
        createCaseView(asList(createReferForCourtHearingDecision()));

        final JsonObject caseResults = resultsService.findCaseResults(envelope());

        assertThat(caseResults.getJsonArray("caseDecisions").getJsonObject(0).getJsonArray("offences").toString(),
                is(resultedCaseEventForReferForCourtHearing.payloadAsJsonObject().getJsonArray("offences").toString()));
    }

    @Test
    public void shouldConvertSetAsideDecision() {
        createCaseView(asList(createSetAsideDecision()));

        final JsonObject caseResults = resultsService.findCaseResults(envelope());

        final JsonArray offences = caseResults.getJsonArray("caseDecisions").getJsonObject(0).getJsonArray("offences");
        assertThat(offences, hasSize(2));
        assertThat(offences.get(0).toString(), hasSetAsideResult(OFFENCE_ID_1));
        assertThat(offences.get(1).toString(), hasSetAsideResult(OFFENCE_ID_2));
    }

    private Matcher hasSetAsideResult(final UUID offenceId) {
        return isJson(allOf(
                withJsonPath("$.id", equalTo(offenceId.toString())),
                withJsonPath("$.results[*]", contains(isJson(allOf(
                        withJsonPath("resultDefinitionId", equalTo(SETASIDE.getResultDefinitionId().toString())),
                        withJsonPath("prompts[*]", empty())
                ))))));
    }

    private JsonEnvelope envelope() {
        return envelopeFrom(metadataFrom(metadataBuilder.build()), createObjectBuilder()
                .add("caseId", CASE_ID.toString()));
    }

    private JsonEnvelope createResultedCaseEventForWithdraw(MetadataBuilder metadataBuilder) {
        return envelopeFrom(metadataBuilder,
                getFileContentAsJson("case-results-tests/case-resulted-event-for-withdraw.json",
                        ImmutableMap.<String, Object>builder()
                                .put("resultedOn", DECISION_SAVED_AT2)
                                .put("offenceId", OFFENCE_ID_1)
                                .build()));
    }

    private JsonEnvelope createResultedCaseEventForAdjourn(MetadataBuilder metadataBuilder) {
        return envelopeFrom(metadataBuilder,
                getFileContentAsJson("case-results-tests/case-resulted-event-for-adjourn.json",
                        ImmutableMap.<String, Object>builder()
                                .put("resultedOn", DECISION_SAVED_AT1)
                                .put("offenceId", OFFENCE_ID_1)
                                .build()));
    }

    private JsonEnvelope createResultedCaseEventForReferForCourtHearing(MetadataBuilder metadataBuilder) {
        return envelopeFrom(metadataBuilder,
                getFileContentAsJson("case-results-tests/case-resulted-event-for-refer-for-court-hearing.json",
                        ImmutableMap.<String, Object>builder()
                                .put("resultedOn", DECISION_SAVED_AT1)
                                .put("offenceId", OFFENCE_ID_1)
                                .put("offenceId2", OFFENCE_ID_2)
                                .build()));
    }

    private void createEmployer() {
        uk.gov.moj.cpp.sjp.domain.Address address = new uk.gov.moj.cpp.sjp.domain.Address("14 Tottenham Court Road", "London", "England", "UK", "Greater London", POSTCODE);
        employer = new Employer(DEFENDANT_ID, "McDonald's", "12345", "020 7998 9300", address);
    }

    private void createCaseView(final List<CaseDecision> caseDecisionList) {

        final OffenceDetail offenceDetail1 = OffenceDetail.builder()
                .setId(OFFENCE_ID_1)
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
                .setId(OFFENCE_ID_2)
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

        Address address = new Address("14 Tottenham Court Road", "London", "England", "UK", "Greater London", POSTCODE);
        PersonalDetails personalDetails = new PersonalDetails("title",
                "McDonald's",
                "lastName",
                LocalDate.now().minusYears(20),
                Gender.MALE,
                "NINO",
                "DriverLicense",
                null,
                address,
                null,
                null);

        DefendantDetail defendantDetail = new DefendantDetail(DEFENDANT_ID);
        defendantDetail.setPersonalDetails(personalDetails);

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
        when(caseService.findCase(CASE_ID)).thenReturn(caseView);
    }

    private CaseDecision createAdjournOffenceDecision() {
        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setId(DECISION_ID_1);
        caseDecision.setSavedAt(DECISION_SAVED_AT1);
        caseDecision.setSession(new Session(SESSION_ID1, null, null, null, LJA_NATIONAL_COURT_CODE, null, null));
        caseDecision.setCaseId(CASE_ID);
        OffenceDecision adjournOffenceDecision = new AdjournOffenceDecision(OFFENCE_ID_1, DECISION_ID_1, "No sufficient information yet", LocalDate.of(2020, 02, 18), FOUND_NOT_GUILTY, null, null);
        caseDecision.setOffenceDecisions(asList(adjournOffenceDecision));
        return caseDecision;
    }

    private CaseDecision createReferForCourtHearingDecision() {
        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setId(DECISION_ID_1);
        caseDecision.setSavedAt(DECISION_SAVED_AT1);
        caseDecision.setSession(new Session(SESSION_ID1, null, null, null, LJA_NATIONAL_COURT_CODE, null, null));
        caseDecision.setCaseId(CASE_ID);


        final OffenceDecision referredToOpenCourt = new ReferForCourtHearingDecision(
                OFFENCE_ID_1,
                DECISION_ID_1,
                UUID.fromString("809f7aac-d285-43a5-9fb1-3a894db71530"),
                10,
                "",
                NO_VERDICT,
                null, null);
        final OffenceDecision referredToOpenCourt2 = new ReferForCourtHearingDecision(
                OFFENCE_ID_2,
                DECISION_ID_2,
                UUID.fromString("809f7aac-d285-43a5-9fb1-3a894db71530"),
                10,
                "",
                null,
                null, null);
        caseDecision.setOffenceDecisions(asList(referredToOpenCourt, referredToOpenCourt2));
        return caseDecision;
    }

    private CaseDecision createSetAsideDecision() {
        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setId(DECISION_ID_1);
        caseDecision.setSavedAt(DECISION_SAVED_AT1);
        caseDecision.setSession(new Session(SESSION_ID1, null, null, null, LJA_NATIONAL_COURT_CODE, null, null));
        caseDecision.setCaseId(CASE_ID);
        caseDecision.setOffenceDecisions(asList(
                new SetAsideOffenceDecision(OFFENCE_ID_1, DECISION_ID_1, null),
                new SetAsideOffenceDecision(OFFENCE_ID_2, DECISION_ID_2, null))
        );
        return caseDecision;
    }

    private CaseDecision getWithDrawCaseDecision() {
        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setId(DECISION_ID_2);
        caseDecision.setSavedAt(DECISION_SAVED_AT2);
        caseDecision.setSession(new Session(SESSION_ID2, null, null, null, LJA_NATIONAL_COURT_CODE, null, null));
        caseDecision.setCaseId(CASE_ID);
        final WithdrawOffenceDecision withdrawOffenceDecision = new WithdrawOffenceDecision(OFFENCE_ID_1, DECISION_ID_2, UUID.fromString("030d4335-f9fe-39e0-ad7e-d01a0791ff87"), FOUND_NOT_GUILTY, null);
        caseDecision.setOffenceDecisions(asList(withdrawOffenceDecision));
        return caseDecision;
    }

    private FakeReferenceDataService setUpFakeReferenceDataService() {
        final FakeReferenceDataService referenceDataService = new FakeReferenceDataService();
        referenceDataService.addWithdrawalReason(fromString("030d4335-f9fe-39e0-ad7e-d01a0791ff87"), "Insufficient Evidence");
        referenceDataService.addWithdrawalReason(fromString("93c6b978-4cad-31f0-a6d9-59cfd71c324a"), "Agreement between prosecutor and defendant reached");
        referenceDataService.addWithdrawalReason(fromString("a11670b7-681a-39dc-951e-f2f1c24fb4c9"), "Not in public interest to proceed");
        referenceDataService.addWithdrawalReason(fromString("3c582e1a-a896-35ee-bb1d-1317c3f22982"), "Alternative Charge");
        referenceDataService.addWithdrawalReason(fromString("11b9087a-4681-3484-b2cf-684295353ac6"), "Other");

        final JsonObject enforcementArea = createObjectBuilder().add("accountDivisionCode", 77).add("enforcingCourtCode", 828).build();
        referenceDataService.addEnforcementAreaByPostcode(POSTCODE, enforcementArea);
        referenceDataService.addEnforcementAreaByLocalJusticeAreaNationalCourtCode(LJA_NATIONAL_COURT_CODE, enforcementArea);

        return referenceDataService;
    }
}
