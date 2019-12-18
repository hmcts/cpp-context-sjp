package uk.gov.moj.cpp.sjp.query.view.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaMethod.ONLINE;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.query.view.util.JsonHelper.readJsonFromFile;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OffenceHelperTest {

    /*@Mock
    private SjpService sjpService;*/

    @Mock
    private ReferenceDataService referenceDataService;

    @Spy
    @InjectMocks
    private OffenceHelper offenceHelper = new OffenceHelper();

    private static final UUID caseId = randomUUID();
    private static final UUID defendantId = randomUUID();
    private static final UUID offenceId1 = randomUUID();
    private static final UUID offenceId2 = randomUUID();
    private static final UUID offenceId3 = randomUUID();
    private CaseView caseView;
    private Employer employer;

    @Before
    public void init(){
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

        //final CaseDetail caseDetail1 = readJsonFromFile("data/sjp.query.case.json", CaseDetail.class);
        caseView = new CaseView(caseDetail);//readJsonFromFile("data/sjp.query.case.json", CaseView.class);
        employer = new Employer(defendantId, "McDonald's", "12345", "020 7998 9300",
                new Address("14 Tottenham Court Road", "London", "England", "UK", "Greater London", "W1T 1JY"));
//readJsonFromFile("data/sjp.query.employer.json", Employer.class);
        //when(sjpService.getCaseView(any(), any())).thenReturn(caseView);
        /*final JsonEnvelope employerEnvelope = createEnvelope(
                "dummy",
                readJsonFromFile("data/sjp.query.employer.json")
        );*/
        //when(sjpService.getDefendantEmployer(any(), any())).thenReturn(employerEnvelope);
        when(referenceDataService.getAllFixedList(any())).thenReturn(Optional.of(readJsonFromFile("data/referencedata.get-all-fixed-list.json")));
    }

    @Test
    public void shouldNotPopulateOffencesForincorrectResultCode(){

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
    public void shouldPopulateOffencesCorrectly(){

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

    private void assertOffencesEquals(final JsonObject expectedPayload, final JsonObject actualPayload){
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

                                assertThat(format("Missing prompts for resultId : %s ",  expectedResultId)  , actualPromptMap, is(Matchers.notNullValue()));

                                expectedPromptMap.keySet().forEach(
                                        expectedPromptId -> {

                                            if(actualPromptMap.containsKey(expectedPromptId.trim())) {

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

    private Map<String, Map<String, Map<String, String>>> mapOffence(final JsonArray offences){
        //offence, results, prompts

        Map<String, Map<String, Map<String, String>>> offenceMap = new HashMap<>();
        //first check size
        offences.getValuesAs(JsonObject.class).forEach(offence -> {
            String offenceId = offence.getString("id");
            Map<String, Map<String, String>> resultsMap = new HashMap<>();
            offenceMap.put(offenceId.trim(), resultsMap);
            if(offence.containsKey("results")) {
                offence.getJsonArray("results").getValuesAs(JsonObject.class).forEach(result -> {
                    Optional<String> resultDefinitionIdOptional = Optional.ofNullable(result.getString("resultDefinitionId", null));
                    if(resultDefinitionIdOptional.isPresent()){
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
