package uk.gov.moj.sjp.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static uk.gov.moj.sjp.it.stub.StubHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UnifiedSearchStub {

    public static final String SJP_ANY_OPEN_STATUS = "NO_PLEA_RECEIVED";
    public static final String SJP_REFERRAL_STATUS = "REFERRED_FOR_COURT_HEARING";

    private static final String SEARCH_QUERY = "/unifiedsearchquery-service/query/api/rest/unifiedsearchquery/cases";
    private static final String SEARCH_QUERY_TYPE = "application/vnd.unifiedsearch.query.cases+json";
    private static final String SERVICE_NAME = "unifiedsearchquery-service";

    public static void stubUnifiedSearchQueryForCases(UUID potentialCaseId,
                                                      String potentialCaseRef) {
        InternalEndpointMockUtils.stubPingFor(SERVICE_NAME);

        final String sjpOpenCase =
                createSingleCaseUnifiedSearchResult(randomUUID(),
                        "",
                        true,
                        SJP_ANY_OPEN_STATUS,
                        5);
        stubFor(get(urlPathMatching(SEARCH_QUERY))
                .inScenario("DEFENDANT_POTENTIAL_CASES")
                .withQueryParam("partyDateOfBirth", matching("1980-10-10"))
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(sjpOpenCase)
                )
        );
        waitForStubToBeReady(SEARCH_QUERY + "?partyDateOfBirth=1980-10-10", SEARCH_QUERY_TYPE);

        final String sjpOpenCase2 =
                createAndInterpolateSingleCase(potentialCaseId,
                        potentialCaseRef,
                        true,
                        SJP_ANY_OPEN_STATUS,
                        5);
        final String multiCasesResult = createMultiCaseUnifiedSearchResult(1, sjpOpenCase2);
        stubFor(get(urlPathMatching(SEARCH_QUERY))
                .inScenario("DEFENDANT_POTENTIAL_CASES")
                .withQueryParam("partyDateOfBirth", matching("1980-10-15"))
                .willReturn(aResponse()
                        .withStatus(OK.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(multiCasesResult)
                )
        );
        waitForStubToBeReady(SEARCH_QUERY + "?partyDateOfBirth=1980-10-15", SEARCH_QUERY_TYPE);
    }

    private static String createSingleCaseUnifiedSearchResult(UUID caseId,
                                                              String caseRef,
                                                              boolean sjp,
                                                              String status,
                                                              int dateOffset) {
        String searchResult = "{ \n" +
                "  \"totalResults\": 1, \n" +
                "  \"cases\": [\n" +
                createAndInterpolateSingleCase(caseId, caseRef, sjp, status, dateOffset) +
                "  ]\n" +
                "}\n";

        return searchResult;
    }

    private static String createMultiCaseUnifiedSearchResult(int numOfCases, String... cases) {
        final String multiCases = Arrays.stream(cases).collect(Collectors.joining(","));
        String searchResult = "{ \n" +
                "  \"totalResults\": " + numOfCases + ", \n" +
                "  \"cases\": [\n" +
                multiCases +
                "  ]\n" +
                "}\n";

        return searchResult;
    }

    private static String createAndInterpolateSingleCase(UUID caseId,
                                                         String caseRef,
                                                         boolean sjp,
                                                         String status,
                                                         int dateOffset) {
        LocalDate today = LocalDate.now();
        List<String> hearingDates = new LinkedList<>();
        for (int i = 0; i < 5; i++) {
            hearingDates.add(today.minusDays(dateOffset++).toString());
        }
        final String hearingDateStr = hearingDates.stream().collect(Collectors.joining("\",\n\""));
        String caseResult = "    {\n" +
                "      \"caseId\": \"%s\",\n" +
                "      \"caseReference\": \"%s\",\n" +
                "      \"sjp\": %s,\n" +
                "      \"crownCourt\": true,\n" +
                "      \"magistrateCourt\": true,\n" +
                "      \"sjpNoticeServed\": \"2019-05-07\",\n" +
                "      \"caseStatus\": \"%s\",\n" +
                "      \"caseType\": \"PROSECUTION\",\n" +
                "      \"parties\": [\n" +
                "        {\n" +
                "          \"firstName\": \"%s\",\n" +
                "          \"middleName\": \"\",\n" +
                "          \"lastName\": \"%s\",\n" +
                "          \"partyType\": \"APPLICANT\",\n" +
                "          \"organisationName\": \"Some Org Name\",\n" +
                "          \"dateOfBirth\": \"%s\",\n" +
                "          \"addressLines\": \"%s\",\n" +
                "          \"postCode\": \"%s\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"hearings\": [\n" +
                "        {\n" +
                "          \"hearingId\": \"70baba30-f45c-4c1e-971e-4581fc2ec30b\",\n" +
                "          \"courtId\": \"b58bdc19-2fb8-4841-bff3-b99f3442c646\",\n" +
                "          \"courtCentreName\": \"Liverpool Magistrates Court\",\n" +
                "          \"hearingTypeId\": \"79bdc7ce-b00b-4d91-b74c-f41aee572a1c\",\n" +
                "          \"hearingTypeLabel\": \"HEARING TYPE LABEL 1\",\n" +
                "          \"hearingDates\": [\n" +
                "            \"%s\"\n" +
                "          ],\n" +
                "          \"isBoxHearing\": true,\n" +
                "          \"isVirtualBoxHearing\": true,\n" +
                "          \"hearingDay\": [\n" +
                "            {\n" +
                "              \"sittingDay\": \"2019-01-22T10:00:00Z\",\n" +
                "              \"listingSequence\": 1,\n" +
                "              \"listedDurationMinutes\": 60\n" +
                "            }\n" +
                "          ],\n" +
                "          \"jurisdictionType\": \"4e2bddef-9797-424b-980c-95c467b84e86\",\n" +
                "          \"judiciaryTypes\": []\n" +
                "        }\n" +
                "      ],\n" +
                "      \"applications\": [\n" +
                "        {\n" +
                "          \"applicationId\": \"ab746921-d839-4867-bcf9-b41db8ebc853\",\n" +
                "          \"applicationReference\": \"CJ03510\",\n" +
                "          \"applicationType\": \"Application within criminal proceedings\",\n" +
                "          \"receivedDate\": \"2019-01-01\",\n" +
                "          \"decisionDate\": \"2019-04-07\",\n" +
                "          \"dueDate\": \"2019-04-08\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n";


        caseResult = String.format(caseResult,
                caseId,
                caseRef,
                sjp,
                status,
                "David",
                "LLOYD",
                "1980-07-15",
                "14 Tottenham Court Road",
                "W1T 1JY",
                hearingDateStr);

        return caseResult;
    }
}
