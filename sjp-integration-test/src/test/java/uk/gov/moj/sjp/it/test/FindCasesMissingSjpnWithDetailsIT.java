package uk.gov.moj.sjp.it.test;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.moj.sjp.it.stub.AuthorisationServiceStub.stubEnableAllCapabilities;

import uk.gov.moj.sjp.it.helper.AbstractTestHelper;
import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;
import uk.gov.moj.sjp.it.helper.CaseSjpHelper;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FindCasesMissingSjpnWithDetailsIT extends AbstractTestHelper {

    private List<CaseSjpHelper> sjpCases;
    private List<CaseSjpHelper> sjpCasesWithSjpn;
    private List<CaseSjpHelper> sjpCasesWithoutSjpn;

    private List<CaseSjpHelper> sjpCasesYoungerThan21Days;
    private List<CaseSjpHelper> sjpCasesOlderThan21Days;

    private List<CaseSjpHelper> sjpCasesYoungerThan21DaysWithSjpn;
    private List<CaseSjpHelper> sjpCasesYoungerThan21DaysWithoutSjpn;
    private List<CaseSjpHelper> combinedSjpCasesYoungerThan21Days;

    private List<CaseSjpHelper> sjpCasesOlderThan21DaysWithSjpn;
    private List<CaseSjpHelper> sjpCasesOlderThan21DaysWithoutSjpn;
    private List<CaseSjpHelper> combinedSjpCasesOlderThan21Days;

    private List<CaseDocumentHelper> sjpnDocuments;

    @Before
    public void init() {
        stubEnableAllCapabilities();
        final LocalDate now = LocalDate.now();
        sjpCasesYoungerThan21Days = Arrays.asList(new CaseSjpHelper(now.minusDays(21)), new CaseSjpHelper(now.minusDays(20)), new CaseSjpHelper(now.minusDays(19)));
        sjpCasesOlderThan21Days = Arrays.asList(new CaseSjpHelper(now.minusDays(22)), new CaseSjpHelper(now.minusDays(23)), new CaseSjpHelper(now.minusDays(24)));

        sjpCasesYoungerThan21DaysWithSjpn = sjpCasesYoungerThan21Days.subList(0, 1);
        sjpCasesYoungerThan21DaysWithoutSjpn = sjpCasesYoungerThan21Days.subList(1, sjpCasesYoungerThan21Days.size());
        combinedSjpCasesYoungerThan21Days = Stream.concat(sjpCasesYoungerThan21DaysWithSjpn.stream(), sjpCasesYoungerThan21DaysWithoutSjpn.stream()).collect(toList());

        sjpCasesOlderThan21DaysWithSjpn = sjpCasesOlderThan21Days.subList(0, 1);
        sjpCasesOlderThan21DaysWithoutSjpn = sjpCasesOlderThan21Days.subList(1, sjpCasesOlderThan21Days.size());
        combinedSjpCasesOlderThan21Days = Stream.concat(sjpCasesOlderThan21DaysWithSjpn.stream(), sjpCasesOlderThan21DaysWithoutSjpn.stream()).collect(toList());

        sjpCasesWithSjpn = Stream.concat(sjpCasesYoungerThan21DaysWithSjpn.stream(), sjpCasesOlderThan21DaysWithSjpn.stream()).collect(toList());
        sjpCasesWithoutSjpn = Stream.concat(sjpCasesYoungerThan21DaysWithoutSjpn.stream(), sjpCasesOlderThan21DaysWithoutSjpn.stream()).collect(toList());

        sjpnDocuments = sjpCasesWithSjpn.stream()
                .map(caseHelper -> new CaseDocumentHelper(caseHelper.getCaseId())).collect(toList());

        sjpCases = Stream.concat(combinedSjpCasesYoungerThan21Days.stream(), combinedSjpCasesOlderThan21Days.stream()).collect(toList());
    }

    @After
    public void tearDown() {
        sjpCases.forEach(CaseSjpHelper::close);
    }

    @Test
    public void findCasesMissingSjpnWithDetails() {

        final JsonObject initialCasesWithIds = getCasesMissingSjpn();
        final JsonObject initialCasesOlderThan21DaysWithId = getCasesMissingSjpnPostedDaysAgo(21);


        createCasesAndDocumentsAndPeople();

        final JsonObject casesWithIds = getCasesMissingSjpn();
        final JsonObject casesOlderThan21DaysWithId = getCasesMissingSjpnPostedDaysAgo(21);

        assertThat((casesWithIds.getInt("count") - (initialCasesWithIds.getInt("count"))), equalTo(sjpCasesWithoutSjpn.size()));
        assertThat((casesOlderThan21DaysWithId.getInt("count")) - (initialCasesOlderThan21DaysWithId.getInt("count")), equalTo(sjpCasesOlderThan21DaysWithoutSjpn.size()));

    }

    private void createCasesAndDocumentsAndPeople() {
        sjpCases.forEach(CaseSjpHelper::createAndVerifyCase);
        sjpnDocuments.forEach(CaseDocumentHelper::addDocumentAndVerifyAdded);
    }

    private JsonObject getCasesMissingSjpn() {
        return getCasesMissingSjpnHelper(getReadUrl("/cases-missing-sjpn-with-details"));
    }

    private JsonObject getCasesMissingSjpnPostedDaysAgo(int postedDaysAgo) {
        String url = getReadUrl("/cases-missing-sjpn-with-details") + String.format("/?daysSincePosting=%d", postedDaysAgo);
        return getCasesMissingSjpnHelper(url);
    }

    private JsonObject getCasesMissingSjpnHelper(String url) {
        Response response = makeGetCall(url, "application/vnd.sjp.query.cases-missing-sjpn-with-details+json");
        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));
        return Json.createReader(new StringReader(response.readEntity(String.class))).readObject();
    }
}
