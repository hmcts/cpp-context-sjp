package uk.gov.moj.sjp.it.test;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.text.IsEqualCompressingWhiteSpace.equalToCompressingWhiteSpace;
import static uk.gov.moj.sjp.it.Constants.DEFAULT_OFFENCE_TITLE;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.DefendantBuilder.defaultDefendant;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.TransparencyReportDBHelper.checkIfFileExists;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllProsecutorsQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAnyQueryOffences;
import static uk.gov.moj.sjp.it.stub.SystemDocumentGeneratorStub.pollDocumentGenerationRequests;
import static uk.gov.moj.sjp.it.stub.SystemDocumentGeneratorStub.stubDocumentGeneratorEndPoint;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.command.builder.AddressBuilder;
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
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class TransparencyReportIT extends BaseIntegrationTest {

    private TransparencyReportHelper transparencyReportHelper = new TransparencyReportHelper();
    private CreateCasePayloadBuilder createCasePayloadBuilder;
    private final UUID caseId1 = randomUUID(), caseId2 = randomUUID();
    private final UUID offenceId1 = randomUUID(), offenceId2 = randomUUID();

    private static final String SJP_EVENTS_TRANSPARENCY_REPORT_REQUESTED = "sjp.events.transparency-report-requested";
    private static final String SJP_EVENTS_TRANSPARENCY_REPORT_GENERATED = "sjp.events.transparency-report-generated";

    @Before
    public void setUp() {
        stubDocumentGeneratorEndPoint();
        stubAllProsecutorsQuery();
        stubAnyQueryOffences();
    }

    @Test
    public void shouldGenerateTransparencyReports() throws IOException {

        final CreateCase.DefendantBuilder defendant1 = defaultDefendant()
                .withRandomLastName();

        final CreateCase.DefendantBuilder defendant2 = defaultDefendant()
                .withRandomLastName()
                .withDefaultShortAddress();

        final CreateCasePayloadBuilder case1 = createCase(caseId1, offenceId1, defendant1);
        final CreateCasePayloadBuilder case2 = createCase(caseId2, offenceId2, defendant2);

        final EventListener eventListener = new EventListener()
                .withMaxWaitTime(50000)
                .subscribe(SJP_EVENTS_TRANSPARENCY_REPORT_REQUESTED, SJP_EVENTS_TRANSPARENCY_REPORT_GENERATED)
                .run(transparencyReportHelper::requestToGenerateTransparencyReport);

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
        validateDocumentGenerationRequest(documentGenerationRequests.get(0), "PendingCasesEnglish", case1);
        validateDocumentGenerationRequest(documentGenerationRequests.get(1), "PendingCasesWelsh", case2);

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
                                                   final String templateName,
                                                   final CreateCasePayloadBuilder casePayloadBuilder) {
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
                .filter((eachCase) -> eachCase.get("defendantName").equals(getShortDefendantName(casePayloadBuilder.getDefendantBuilder())))
                .collect(toList());
        assertThat(filteredReadyCases.size(), is(1));

        final JSONObject readyCase = filteredReadyCases.get(0);

        final AddressBuilder address = casePayloadBuilder.getDefendantBuilder().getAddressBuilder();

        final String expectedCounty = getCounty(address);
        final String expectedTown = getTown(address);
        final String expectedPostcode = getPostcode(address);
        final String expectedProsecutorName = casePayloadBuilder.getProsecutingAuthority().name();
        final String expectedOffenceTitle = DEFAULT_OFFENCE_TITLE;

        assertThat(readyCase.optString("county", null), is(expectedCounty));
        assertThat(readyCase.optString("town", null), is(expectedTown));
        assertThat(readyCase.optString("postcode", null), is(expectedPostcode));
        assertThat(readyCase.optString("offenceTitle"), is(expectedOffenceTitle));
        assertThat(readyCase.optString("prosecutorName"), is(expectedProsecutorName));
    }

    private static String getTown(final AddressBuilder addressBuilder) {
        return isNotEmpty(addressBuilder.getAddress5()) ? addressBuilder.getAddress4() : addressBuilder.getAddress3();
    }

    private static String getCounty(final AddressBuilder addressBuilder) {
        return isNotEmpty(addressBuilder.getAddress5()) ? addressBuilder.getAddress5() : addressBuilder.getAddress4();
    }

    private static String getPostcode(final AddressBuilder addressBuilder) {
        return StringUtils.substring(StringUtils.defaultString(addressBuilder.getPostcode()), 0, 2);
    }

    private CreateCasePayloadBuilder createCase(final UUID caseId,
                                                final UUID offenceId,
                                                final CreateCase.DefendantBuilder defendantBuilder) {
        final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
        createCasePayloadBuilder = CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withOffenceId(offenceId)
                .withPostingDate(postingDate)
                .withDefendantBuilder(defendantBuilder);

        createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(createCasePayloadBuilder.getId());
        return createCasePayloadBuilder;
    }

    private void validateThePdfContent(final String mockedContent) {
        assertThat(mockedContent, equalToCompressingWhiteSpace(transparencyReportHelper.getStubbedContent()));
    }

    private static String getShortDefendantName(final CreateCase.DefendantBuilder defendantBuilder) {
        return StringUtils.substring(defendantBuilder.getFirstName(), 0, 1) + " " + defendantBuilder.getLastName();
    }
}
