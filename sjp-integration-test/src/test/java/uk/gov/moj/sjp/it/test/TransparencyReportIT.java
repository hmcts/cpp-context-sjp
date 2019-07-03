package uk.gov.moj.sjp.it.test;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToCompressingWhiteSpace;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.TransparencyReportDBHelper.checkIfFileExists;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryOffences;
import static uk.gov.moj.sjp.it.stub.SystemDocumentGeneratorStub.pollDocumentGenerationRequests;
import static uk.gov.moj.sjp.it.stub.SystemDocumentGeneratorStub.stubDocumentGeneratorEndPoint;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.TransparencyReportHelper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.google.common.collect.Sets;
import org.hamcrest.Matcher;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class TransparencyReportIT extends BaseIntegrationTest {

    private TransparencyReportHelper transparencyReportHelper = new TransparencyReportHelper();
    private CreateCasePayloadBuilder createCasePayloadBuilder;

    private static final String SJP_EVENTS_TRANSPARENCY_REPORT_REQUESTED = "sjp.events.transparency-report-requested";
    private static final String SJP_EVENTS_TRANSPARENCY_REPORT_GENERATED = "sjp.events.transparency-report-generated";

    @Before
    public void setUp() {
    }

    @Test
    public void shouldGenerateTransparencyReports() throws IOException {
        // stub the document generator
        stubDocumentGeneratorEndPoint();
        // stub prosecutor and offences, reference data
        stubProsecutorQuery(ProsecutingAuthority.TFL.toString(), randomUUID());
        stubProsecutorQuery(ProsecutingAuthority.TVL.toString(), randomUUID());
        stubProsecutorQuery(ProsecutingAuthority.DVLA.toString(), randomUUID());
        stubQueryOffences("stub-data/referencedata.query.offences.json");

        // CASE1
        final UUID caseId1 = randomUUID();
        final UUID offenceId1 = randomUUID();
        final String case1defendantLastName = randomUUID().toString() + "_LastName";
        createCase(caseId1, offenceId1, case1defendantLastName);
        // put the case in ready  status
        pollUntilCaseReady(caseId1);

        // CASE2
        final UUID caseId2 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final String case2defendantLastName = randomUUID().toString() + "_LastName";
        createCase(caseId2, offenceId2, case2defendantLastName);
        // put the case in ready  status
        pollUntilCaseReady(caseId2);

        // make a request to generate the file
        final EventListener eventListener = new EventListener().withMaxWaitTime(40000);
        eventListener
                .subscribe(SJP_EVENTS_TRANSPARENCY_REPORT_REQUESTED)
                .subscribe(SJP_EVENTS_TRANSPARENCY_REPORT_GENERATED)
                .run(() -> transparencyReportHelper.requestToGenerateTransparencyReport());

        final Optional<JsonEnvelope> transparencyReportRequestedEvent = eventListener.popEvent(SJP_EVENTS_TRANSPARENCY_REPORT_REQUESTED);
        final Optional<JsonEnvelope> transparencyReportDataEvent = eventListener.popEvent(SJP_EVENTS_TRANSPARENCY_REPORT_GENERATED);

        assertThat(transparencyReportRequestedEvent.isPresent(), is(true));
        assertThat(transparencyReportDataEvent.isPresent(), is(true));

        final JsonEnvelope transparencyReportDataEnvelope = transparencyReportDataEvent.get();
        final JsonObject englishReportMetadata = transparencyReportDataEnvelope.payloadAsJsonObject()
                .getJsonObject("englishReportMetadata");
        final JsonObject welshReportMetadata = transparencyReportDataEnvelope.payloadAsJsonObject()
                .getJsonObject("welshReportMetadata");
        final JsonArray caseIds = transparencyReportDataEnvelope.payloadAsJsonObject().getJsonArray("caseIds");

        final Set<String> savedCaseIds = Sets.newHashSet(caseId1.toString(), caseId2.toString());

        // check the cases that are created are in the transparency report generated event payload
        final List filteredCaseIDs = caseIds.getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .filter(savedCaseIds::contains)
                .collect(toList());

        assertThat(filteredCaseIDs.size(), is(2));

        // assert that a request is sent to put the english file on the file server
        assertThat(checkIfFileExists(fromString(englishReportMetadata.getString("fileId"))), is(true));

        // assert that a request is sent to put the welsh file on the file server
        assertThat(checkIfFileExists(fromString(welshReportMetadata.getString("fileId"))), is(true));

        // get the captured requests
        final List<JSONObject> documentGenerationRequests = pollDocumentGenerationRequests(hasSize(2));

        // validate the english and welsh payloads
        validateDocumentGenerationRequest(documentGenerationRequests.get(0), case1defendantLastName, "PendingCasesEnglish");
        validateDocumentGenerationRequest(documentGenerationRequests.get(1), case2defendantLastName, "PendingCasesWelsh");

        // get the report metadata
        final Matcher matcher = withJsonPath("reportsMetadata.*", hasItem(
                isJson(allOf(
                        withJsonPath("fileId", equalTo(englishReportMetadata.getString("fileId")))
                ))));

        final JsonObject reportsMetadata = transparencyReportHelper.pollForTransparencyReportMetadata(matcher);
        final JsonArray reportsArray = reportsMetadata.getJsonArray("reportsMetadata");
        final JsonObject englishReport = reportsArray.getJsonObject(0);
        final JsonObject welshReport = reportsArray.getJsonObject(1);


        validateMetadata(englishReport, englishReportMetadata, false);
        validateMetadata(welshReport, welshReportMetadata, true);

        // validate the content
        final String englishContent = transparencyReportHelper.requestToGetTransparencyReportContent(englishReportMetadata.getString("fileId"));
        validateThePdfContent(englishContent);

        final String welshContent = transparencyReportHelper.requestToGetTransparencyReportContent(welshReportMetadata.getString("fileId"));
        validateThePdfContent(welshContent);
    }

    private void validateMetadata(final JsonObject reportObject,
                                  final JsonObject reportMetadata,
                                  final boolean welsh) {
        assertThat(reportObject.getString("fileId"), is(reportMetadata.getString("fileId")));
        assertThat(reportObject.getString("reportIn"), is(welsh ? "Welsh" : "English"));
        assertThat(reportObject.get("size").toString(), is(notNullValue()));
        assertThat(reportObject.getInt("pages"), is(reportMetadata.getInt("numberOfPages")));
        assertThat(reportObject.getString("generatedAt"), is(notNullValue()));
    }

    private void validateDocumentGenerationRequest(final JSONObject documentGenerationRequest,
                                                   final String defendantLastName,
                                                   final String templateName) {
        assertThat(documentGenerationRequest.getString("templateName"), is(templateName));
        assertThat(documentGenerationRequest.getString("conversionFormat"), is("pdf"));

        final JSONObject payload = documentGenerationRequest.getJSONObject("templatePayload");

        assertThat(payload.getInt("totalNumberOfRecords"), is(greaterThanOrEqualTo(2)));
        assertThat(payload.getString("generatedDateAndTime"), is(notNullValue()));

        final JSONArray readyCases = payload.getJSONArray("readyCases");
        final List<JSONObject> jsonObjects = new ArrayList<>();
        IntStream
                .range(0, readyCases.length())
                .forEach((index) -> jsonObjects.add(readyCases.getJSONObject(index)));

        final List<JSONObject> filteredReadyCases = jsonObjects
                .stream()
                .filter((eachCase) -> eachCase.get("defendantName").equals("F " + defendantLastName))
                .collect(toList());
        assertThat(filteredReadyCases.size(), is(1));

        final JSONObject readyCase = filteredReadyCases.get(0);
        assertThat(readyCase.get("county"), is("Greater London"));
        assertThat(readyCase.get("offenceTitle"), is("Public service vehicle - passenger use altered / defaced ticket"));
        assertThat(readyCase.get("postcode"), is("W1"));
        assertThat(readyCase.get("prosecutorName"), is("TFL"));
        assertThat(readyCase.get("town"), is("UK"));
    }

    private void createCase(final UUID caseId,
                            final UUID offenceId,
                            final String defendantLastName) {
        final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
        createCasePayloadBuilder = CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withOffenceId(offenceId)
                .withPostingDate(postingDate);

        // generate random defendant last name to assert on the submitted payload
        createCasePayloadBuilder.getDefendantBuilder().withLastName(defendantLastName);
        createCasePayloadBuilder.getDefendantBuilder().withFirstName("FirstName");

        createCaseForPayloadBuilder(createCasePayloadBuilder);
    }

    private void validateThePdfContent(final String mockedContent) {
        assertThat(mockedContent, equalToCompressingWhiteSpace(transparencyReportHelper.getStubbedContent()));
    }

}
