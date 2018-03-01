package uk.gov.moj.sjp.it.test;


import static java.util.Collections.disjoint;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.sjp.it.stub.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makeGetCall;

import uk.gov.moj.sjp.it.helper.AbstractCaseHelper;
import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;
import uk.gov.moj.sjp.it.helper.CaseSjpHelper;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FindCasesMissingSjpnIT extends BaseIntegrationTest {
    private List<CaseSjpHelper> sjpCases;
    private List<CaseSjpHelper> sjpCasesWithoutSjpn;
    private List<CaseSjpHelper> sjpCasesWithSjpn;
    private List<CaseDocumentHelper> sjpnDocuments;

    private List<CaseSjpHelper> sjpCasesYoungerThan3Days;
    private List<CaseSjpHelper> sjpCasesOlderThan3Days;


    // TODO: close Helpers

    @Before
    public void init() {
        stubEnableAllCapabilities();
        final LocalDate now = LocalDate.now();
        sjpCasesYoungerThan3Days = Arrays.asList(new CaseSjpHelper(now.minusDays(1)), new CaseSjpHelper(now.minusDays(2)), new CaseSjpHelper(now.minusDays(3)));
        sjpCasesOlderThan3Days = Arrays.asList(new CaseSjpHelper(now.minusDays(4)), new CaseSjpHelper(now.minusDays(5)), new CaseSjpHelper(now.minusDays(6)));

        sjpCases = Stream.concat(sjpCasesYoungerThan3Days.stream(), sjpCasesOlderThan3Days.stream()).collect(toList());

        sjpCasesWithSjpn = sjpCases.subList(0, 2);
        sjpCasesWithoutSjpn = sjpCases.subList(2, sjpCases.size());

        sjpnDocuments = sjpCasesWithSjpn.stream()
                .map(caseHelper -> new CaseDocumentHelper(caseHelper.getCaseId())).collect(toList());
    }

    @After
    public void tearDown() {
        sjpCases.forEach(CaseSjpHelper::close);
        sjpnDocuments.forEach(CaseDocumentHelper::close);
    }

    @Test
    public void findCasesMissingSjpn() throws IOException {
        int casesMissingSjpnCount = getCasesMissingSjpn().getInt("count");
        int casesOlderThan3DaysMissingSjpnCount = getCasesMissingSjpnPostedDaysAgo(3).getInt("count");
        int expectedCasesMissingSjpnCount = casesMissingSjpnCount + sjpCasesWithoutSjpn.size();
        int expectedCasesOlderThan3DaysMissingSjpnCount = casesOlderThan3DaysMissingSjpnCount + sjpCasesOlderThan3Days.size();

        createCasesAndDocuments();

        final JsonObject casesWithIds = getCasesMissingSjpn();
        final JsonObject casesWithoutIds = getCasesMissingSjpn(0);
        final JsonObject casesOlderThan3DaysWithId = getCasesMissingSjpnPostedDaysAgo(3);
        final JsonObject casesOlderThan3DaysWithoutId = getCasesMissingSjpnPostedDaysAgo(3, 0);
        final List<String> actualCaseIds = extractCaseIds(casesWithIds);
        final List<String> actualCasesOlderThan3DaysIds = extractCaseIds(casesOlderThan3DaysWithId);

        assertTrue(actualCaseIds.containsAll(extractCaseIds(sjpCasesWithoutSjpn)));
        assertTrue(disjoint(actualCaseIds, extractCaseIds(sjpCasesWithSjpn)));
        assertThat(casesWithoutIds.getJsonArray("ids"), empty());
        assertThat(casesWithIds.getInt("count"), equalTo(expectedCasesMissingSjpnCount));
        assertThat(casesWithoutIds.getInt("count"), equalTo(expectedCasesMissingSjpnCount));

        assertTrue(actualCasesOlderThan3DaysIds.containsAll(extractCaseIds(sjpCasesOlderThan3Days)));
        assertTrue(disjoint(actualCasesOlderThan3DaysIds, extractCaseIds(sjpCasesWithSjpn)));
        assertTrue(disjoint(actualCasesOlderThan3DaysIds, extractCaseIds(sjpCasesYoungerThan3Days)));
        assertThat(casesOlderThan3DaysWithoutId.getJsonArray("ids"), empty());
        assertThat(casesOlderThan3DaysWithId.getInt("count"), equalTo(expectedCasesOlderThan3DaysMissingSjpnCount));
        assertThat(casesOlderThan3DaysWithoutId.getInt("count"), equalTo(expectedCasesOlderThan3DaysMissingSjpnCount));
    }

    private void createCasesAndDocuments() {
        sjpCases.forEach(CaseSjpHelper::createAndVerifyCase);
        sjpnDocuments.forEach(CaseDocumentHelper::addDocumentAndVerifyAdded);
    }

    private JsonObject getCasesMissingSjpn() {
        return getCasesMissingSjpnHelper("/cases-missing-sjpn");
    }

    private JsonObject getCasesMissingSjpn(int limit) {
        String url = "/cases-missing-sjpn" + String.format("/?limit=%d", limit);
        return getCasesMissingSjpnHelper(url);
    }

    private JsonObject getCasesMissingSjpnPostedDaysAgo(int postedDaysAgo) {
        String url = "/cases-missing-sjpn" + String.format("/?daysSincePosting=%d", postedDaysAgo);
        return getCasesMissingSjpnHelper(url);
    }

    private JsonObject getCasesMissingSjpnPostedDaysAgo(int postedDaysAgo, int limit) {
        String url = "/cases-missing-sjpn" + String.format("/?daysSincePosting=%d&limit=%d", postedDaysAgo, limit);
        return getCasesMissingSjpnHelper(url);
    }

    private JsonObject getCasesMissingSjpnHelper(String url) {
        Response response = makeGetCall(url, "application/vnd.sjp.query.cases-missing-sjpn+json");
        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));
        return Json.createReader(new StringReader(response.readEntity(String.class))).readObject();
    }

    private List<String> extractCaseIds(List<? extends AbstractCaseHelper> cases) {
        return cases.stream()
                .map(AbstractCaseHelper::getCaseId)
                .collect(toList());
    }

    private List<String> extractCaseIds(final JsonObject cases) {
        return cases.getJsonArray("ids").getValuesAs(JsonString.class).stream().map(JsonString::getString).collect(toList());
    }
}
