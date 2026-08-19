package uk.gov.moj.cpp.sjp.query.view.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaMethod.ONLINE;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.query.view.converter.Prompt.DDD_DISQUALIFICATION_PERIOD;
import static uk.gov.moj.cpp.sjp.query.view.converter.Prompt.DDO_DISQUALIFICATION_PERIOD;
import static uk.gov.moj.cpp.sjp.query.view.converter.Prompt.DDP_DISQUALIFICATION_PERIOD;
import static uk.gov.moj.cpp.sjp.query.view.converter.Prompt.DDP_NOTIONAL_PENALTY_POINTS;
import static uk.gov.moj.cpp.sjp.query.view.converter.Prompt.LEA_REASON_FOR_PENALTY_POINTS;
import static uk.gov.moj.cpp.sjp.query.view.converter.Prompt.LEP_PENALTY_POINTS;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.DDD;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.DDO;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.DDP;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.LEA;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.LEN;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.LEP;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.NSP;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.query.view.util.JsonHelper.readJsonFromFile;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OffenceHelperTest {

    private static final UUID caseId = randomUUID();
    private static final UUID defendantId = randomUUID();
    private static final UUID offenceId1 = randomUUID();
    private static final UUID offenceId2 = randomUUID();
    private static final UUID offenceId3 = randomUUID();
    @Mock
    private ReferenceDataService referenceDataService;
    private OffenceHelper offenceHelper;
    private CaseView caseView;
    private Employer employer;

    @BeforeEach
    public void init() {
        offenceHelper = new OffenceHelper(referenceDataService);
        final OffenceDetail offenceDetail1 = OffenceDetail.builder()
                .setId(offenceId1)
                .setCode("PS90010")
                .setSequenceNumber(1)
                .setWording("On 02/07/2015 At Threadneedle Street EC2 Being a passenger on a Public service Vehicle operated on behalf of London Bus Services Limited being used for the carriage of passengers at separate fares where the vehicle was being operated by a Driver without a Conductor did not as directed by the Driver an Inspector or a Notice displayed on the vehicle pay the fare for the journey in accordance with the direction ")
                .setWordingWelsh("Welsh wording: On 02/07/2015 At Threadneedle Street EC2 Being a passenger on a Public service Vehicle operated on behalf of London Bus Services Limited being used for the carriage of passengers at separate fares where the vehicle was being operated by a Driver without a Conductor did not as directed by the Driver an Inspector or a Notice displayed on the vehicle pay the fare for the journey in accordance with the direction ")
                .setStartDate(LocalDate.now().minusDays(1))
                .setChargeDate(LocalDate.now().plusDays(1))
                .withCompensation(BigDecimal.valueOf(10))
                .withProsecutionFacts("An incident took place at GREEN PARK station whereby you were spoken to by a member of London Underground staff regarding your train journey and the associated fare.The facts of this incidents are now being considered and I must advise you that legal proceedings may be initiated against you regarding this matter in accordance with the LU prosecution policy")
                .build();

        final OffenceDetail offenceDetail2 = OffenceDetail.builder()
                .setId(offenceId2)
                .setCode("PS90011")
                .setPlea(GUILTY)
                .setSequenceNumber(2)
                .setWording("On 02/07/2015 At Threadneedle Street EC2 Being a passenger on a Public service Vehicle operated on behalf of London Bus Services Limited being used for the carriage of passengers at separate fares where the vehicle was being operated by a Driver without a Conductor did not as directed by the Driver an Inspector or a Notice displayed on the vehicle pay the fare for the journey in accordance with the direction ")
                .setChargeDate(LocalDate.now().plusDays(1))
                .build();

        final OffenceDetail offenceDetail3 = OffenceDetail.builder()
                .setId(offenceId3)
                .setCode("PS90015")
                .setPlea(GUILTY)
                .setPleaMethod(ONLINE)
                .setSequenceNumber(3)
                .setWording("On 02/07/2015 At Threadneedle Street EC2 Being a passenger on a Public service Vehicle operated on behalf of London Bus Services Limited being used for the carriage of passengers at separate fares where the vehicle was being operated by a Driver without a Conductor did not as directed by the Driver an Inspector or a Notice displayed on the vehicle pay the fare for the journey in accordance with the direction ")
                .setChargeDate(LocalDate.now().plusDays(1))
                .build();

        DefendantDetail defendantDetail = new DefendantDetail(defendantId);
        defendantDetail.setOffences(asList(offenceDetail1, offenceDetail2, offenceDetail3));

        CaseDetail caseDetail = new CaseDetail(caseId);
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

        JsonObject prosecutorPayload = createObjectBuilder()
                .add("fullName", "DVLA")
                .add("policeFlag", false)
                .build();
        caseView = new CaseView(caseDetail, prosecutorPayload);//readJsonFromFile("data/sjp.query.case.json", CaseView.class);
        employer = new Employer(defendantId, "McDonald's", "12345", "020 7998 9300",
                new Address("14 Tottenham Court Road", "London", "England", "UK", "Greater London", "W1T 1JY"));

    }

    @Test
    public void shouldNotPopulateOffencesForIncorrectResultCode() {
        final JsonObjectBuilder eventPayloadBuilder = createObjectBuilder().add(CASE_ID, caseId.toString());
        final JsonObject decision = readJsonFromFile("data/resulting.events.referenced-decisions-saved-incorrect-result-code.json");

        final JsonObjectBuilder metadataBuilder = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("name", "resulting.events.referenced-decisions-saved");
        final JsonEnvelope envelope = envelopeFrom(metadataFrom(metadataBuilder.build()), decision);

        final JsonArray offenceDecisions = offenceHelper.populateOffences(caseView, employer, envelope);
        eventPayloadBuilder.add("offences", offenceDecisions);
        final JsonObject actualPayload = eventPayloadBuilder.build();

        assertThat(actualPayload,
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", is(caseId.toString()))
                ))
        );

        final JsonArray offences = actualPayload.getJsonArray("offences");
        assertThat(offences.size(), is(1));
        final JsonObject actualOffence = offences.getValuesAs(JsonObject.class).get(0);
        assertThat(actualOffence.getString("id"), is("7d566f6c-f7ac-484c-bfb6-38fde2d687da"));
        assertThat(actualOffence.getJsonArray("results").size(), is(0));
    }

    @Test
    public void shouldPopulateOffencesCorrectly() {
        when(referenceDataService.getAllFixedList(any())).thenReturn(Optional.of(readJsonFromFile("data/referencedata.get-all-fixed-list.json")));
        final JsonObjectBuilder eventPayloadBuilder = createObjectBuilder().add(CASE_ID, caseId.toString());
        final JsonObject decision = readJsonFromFile("data/resulting.events.referenced-decisions-saved.json");

        final JsonObjectBuilder metadataBuilder = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("name", "resulting.events.referenced-decisions-saved");

        final JsonEnvelope envelope = envelopeFrom(metadataFrom(metadataBuilder.build()), decision);

        final JsonArray offences = offenceHelper.populateOffences(caseView, employer, envelope);
        eventPayloadBuilder.add("offences", offences);
        final JsonObject actualPayload = eventPayloadBuilder.build();

        assertThat(actualPayload,
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", is(caseId.toString()))
                ))
        );

        final JsonObject expectedPayload = readJsonFromFile("data/expected-public.events.referenced-decisions-saved.json");

        assertOffencesEquals(expectedPayload, actualPayload);
    }

    @Test
    public void shouldPopulateOffencesCorrectlyWithBackDutyAndExcisePenalty() {
        when(referenceDataService.getAllFixedList(any())).thenReturn(Optional.of(readJsonFromFile("data/referencedata.get-all-fixed-list.json")));
        final JsonObjectBuilder eventPayloadBuilder = createObjectBuilder().add(CASE_ID, caseId.toString());
        final JsonObject decision = readJsonFromFile("data/resulting.events.referenced-decisions-saved-back-duty-and-excise-penalty.json");

        final JsonObjectBuilder metadataBuilder = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("name", "resulting.events.referenced-decisions-saved");

        final JsonEnvelope envelope = envelopeFrom(metadataFrom(metadataBuilder.build()), decision);

        final JsonArray offences = offenceHelper.populateOffences(caseView, employer, envelope);
        eventPayloadBuilder.add("offences", offences);
        final JsonObject actualPayload = eventPayloadBuilder.build();

        assertThat(actualPayload,
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", is(caseId.toString()))
                ))
        );

        final JsonObject expectedPayload = readJsonFromFile("data/expected-public.events.referenced-decisions-saved-back-duty-and-excise-penalty.json");

        assertOffencesEquals(expectedPayload, actualPayload);
    }

    @Test
    public void shouldCreateResultsForNoSeparatePenalty() {
        final JsonObject decision = readJsonFromFile("data/resulting.events.referenced-decisions-saved-results-no-separate-penalty.json");
        final JsonEnvelope envelope = createEnvelope(decision);

        final JsonArray offences = offenceHelper.populateOffences(caseView, employer, envelope);

        final JsonObject result = getSingleResult(offences);
        assertThat(result.getString("resultDefinitionId"), equalTo(NSP.getResultDefinitionId().toString()));
        assertThat(result.getJsonArray("prompts"), empty());
    }

    @Test
    public void shouldCreateResultsForLEN() {
        final JsonObject decision = readJsonFromFile("data/resulting.events.referenced-decisions-saved-results-LEN.json");
        final JsonEnvelope envelope = createEnvelope(decision);

        final JsonArray offences = offenceHelper.populateOffences(caseView, employer, envelope);

        final JsonObject result = getSingleResult(offences);
        assertThat(result.getString("resultDefinitionId"), equalTo(LEN.getResultDefinitionId().toString()));
        assertThat(result.getJsonArray("prompts"), empty());
    }

    @Test
    public void shouldCreateResultsForLEA() {
        final JsonObject decision = readJsonFromFile("data/resulting.events.referenced-decisions-saved-results-LEA.json");
        final JsonEnvelope envelope = createEnvelope(decision);

        final JsonArray offences = offenceHelper.populateOffences(caseView, employer, envelope);

        final JsonObject result = getSingleResult(offences);
        final JsonObject prompt = getSinglePrompt(result);
        assertThat(result.getString("resultDefinitionId"), equalTo(LEA.getResultDefinitionId().toString()));
        assertThat(prompt.getString("promptDefinitionId"), equalTo(LEA_REASON_FOR_PENALTY_POINTS.getId().toString()));
        assertThat(prompt.getString("value"), equalTo("10"));
    }

    @Test
    public void shouldCreateResultsForLEP() {
        final JsonObject decision = readJsonFromFile("data/resulting.events.referenced-decisions-saved-results-LEP.json");
        final JsonEnvelope envelope = createEnvelope(decision);

        final JsonArray offences = offenceHelper.populateOffences(caseView, employer, envelope);

        final JsonObject result = getSingleResult(offences);
        final JsonObject prompt = getSinglePrompt(result);
        assertThat(result.getString("resultDefinitionId"), equalTo(LEP.getResultDefinitionId().toString()));
        assertThat(prompt.getString("promptDefinitionId"), equalTo(LEP_PENALTY_POINTS.getId().toString()));
        assertThat(prompt.getString("value"), equalTo("9"));
    }

    @Test
    public void shouldCreateResultsForDDD() {
        final JsonObject decision = readJsonFromFile("data/resulting.events.referenced-decisions-saved-results-DDD.json");
        final JsonEnvelope envelope = createEnvelope(decision);

        final JsonArray offences = offenceHelper.populateOffences(caseView, employer, envelope);

        final JsonObject result = getSingleResult(offences);
        final JsonObject prompt = getSinglePrompt(result);
        assertThat(result.getString("resultDefinitionId"), equalTo(DDD.getResultDefinitionId().toString()));
        assertThat(prompt.getString("promptDefinitionId"), equalTo(DDD_DISQUALIFICATION_PERIOD.getId().toString()));
        assertThat(prompt.getString("value"), equalTo("1 day"));
    }

    @Test
    public void shouldCreateResultsForDDP() {
        final JsonObject decision = readJsonFromFile("data/resulting.events.referenced-decisions-saved-results-DDP.json");
        final JsonEnvelope envelope = createEnvelope(decision);

        final JsonArray offences = offenceHelper.populateOffences(caseView, employer, envelope);

        final JsonObject result = getSingleResult(offences);
        final JsonArray prompts = result.getJsonArray("prompts");
        assertThat(prompts, hasSize(2));
        assertThat(result.getString("resultDefinitionId"), equalTo(DDP.getResultDefinitionId().toString()));
        assertThat(prompts.getJsonObject(0).getString("promptDefinitionId"), equalTo(DDP_DISQUALIFICATION_PERIOD.getId().toString()));
        assertThat(prompts.getJsonObject(0).getString("value"), equalTo("1 year"));
        assertThat(prompts.getJsonObject(1).getString("promptDefinitionId"), equalTo(DDP_NOTIONAL_PENALTY_POINTS.getId().toString()));
        assertThat(prompts.getJsonObject(1).getString("value"), equalTo("11"));
    }

    @Test
    public void shouldCreateResultsForDDO() {
        final JsonObject decision = readJsonFromFile("data/resulting.events.referenced-decisions-saved-results-DDO.json");
        final JsonEnvelope envelope = createEnvelope(decision);

        final JsonArray offences = offenceHelper.populateOffences(caseView, employer, envelope);

        final JsonObject result = getSingleResult(offences);
        final JsonObject prompt = getSinglePrompt(result);
        assertThat(result.getString("resultDefinitionId"), equalTo(DDO.getResultDefinitionId().toString()));
        assertThat(prompt.getString("promptDefinitionId"), equalTo(DDO_DISQUALIFICATION_PERIOD.getId().toString()));
        assertThat(prompt.getString("value"), equalTo("1 month"));
    }

    private JsonEnvelope createEnvelope(final JsonObject decision) {
        final JsonObjectBuilder metadataBuilder = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("name", "resulting.events.referenced-decisions-saved");
        return envelopeFrom(metadataFrom(metadataBuilder.build()), decision);
    }

    private JsonObject getSingleResult(final JsonArray offences) {
        assertThat(offences, hasSize(1));
        assertThat(offences.getJsonObject(0).getJsonArray("results"), hasSize(1));
        return offences.getJsonObject(0).getJsonArray("results").getJsonObject(0);
    }

    private JsonObject getSinglePrompt(final JsonObject result) {
        assertThat(result.getJsonArray("prompts"), hasSize(1));
        return result.getJsonArray("prompts").getJsonObject(0);
    }

    private void assertOffencesEquals(final JsonObject expectedPayload, final JsonObject actualPayload) {
        Map<String, Map<String, Map<String, String>>> expectedOffenceMap = mapOffence(expectedPayload.getJsonArray("offences"));
        Map<String, Map<String, Map<String, String>>> actualOffenceMap = mapOffence(actualPayload.getJsonArray("offences"));

        assertThat(actualOffenceMap.size(), is(expectedOffenceMap.size()));

        expectedOffenceMap.keySet().forEach(
                expectedOffenceId -> {
                    Map<String, Map<String, String>> actualResultsMap = actualOffenceMap.get(expectedOffenceId);
                    Map<String, Map<String, String>> expectedResultsMap = expectedOffenceMap.get(expectedOffenceId);

                    expectedResultsMap.keySet().forEach(
                            expectedResultId -> {
                                Map<String, String> actualPromptMap = actualResultsMap.get(expectedResultId);
                                Map<String, String> expectedPromptMap = expectedResultsMap.get(expectedResultId);

                                assertThat(format("Missing prompts for resultId : %s ", expectedResultId), actualPromptMap, is(Matchers.notNullValue()));

                                expectedPromptMap.keySet().forEach(
                                        expectedPromptId -> {

                                            if (actualPromptMap.containsKey(expectedPromptId.trim())) {

                                                String actualValue = actualPromptMap.get(expectedPromptId.trim());
                                                String expectedValue = expectedPromptMap.get(expectedPromptId.trim());

                                                assertThat(format("{ expectedPromptId : %s  expectedValue: %s  Actual: %s }", expectedPromptId, expectedValue, actualValue), actualValue, is(expectedValue));

                                            } else {

                                                fail(format("In Result with id: %s Missing prompt { expectedPromptId %s  expectedValue %s ", expectedResultId, expectedPromptId, expectedPromptMap.get(expectedPromptId)));
                                            }
                                        }
                                );

                                assertThat(format("In Result with id: %s { Expected number of prompts : %d  Actual : %d } ", expectedResultId, expectedPromptMap.size(), actualPromptMap.size()), actualPromptMap.size(), is(expectedPromptMap.size()));
                            }
                    );

                    assertThat(format("In offence with id: %s { Expected number of results : %d Actual : %d } ", expectedOffenceId, expectedResultsMap.size(), actualResultsMap.size()), actualResultsMap.size(), is(expectedResultsMap.size()));
                }
        );
    }

    private Map<String, Map<String, Map<String, String>>> mapOffence(final JsonArray offences) {
        Map<String, Map<String, Map<String, String>>> offenceMap = new HashMap<>();
        offences.getValuesAs(JsonObject.class).forEach(offence -> {
            String offenceId = offence.getString("id");
            Map<String, Map<String, String>> resultsMap = new HashMap<>();
            offenceMap.put(offenceId.trim(), resultsMap);
            if (offence.containsKey("results")) {
                offence.getJsonArray("results").getValuesAs(JsonObject.class).forEach(result -> {
                    Optional<String> resultDefinitionIdOptional = Optional.ofNullable(result.getString("resultDefinitionId", null));
                    if (resultDefinitionIdOptional.isPresent()) {
                        Map<String, String> promptsMap = new HashMap<>();
                        resultsMap.put(resultDefinitionIdOptional.get().trim(), promptsMap);
                        result.getJsonArray("prompts").getValuesAs(JsonObject.class).forEach(prompt -> {
                            String promptDefinitionId = prompt.getString("promptDefinitionId", null);
                            String value = prompt.getString("value");
                            promptsMap.put(promptDefinitionId.trim(), value);
                        });
                    } else {
                        fail("Missing resultDefinitionId from offence with id: " + offenceId);
                    }
                });
            } else {
                fail("No results json element found for offenceId" + offenceId);
            }
        });
        return offenceMap;
    }
}
