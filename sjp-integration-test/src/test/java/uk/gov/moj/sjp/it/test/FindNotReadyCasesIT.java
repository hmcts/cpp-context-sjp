package uk.gov.moj.sjp.it.test;


import static java.lang.Integer.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.moj.sjp.it.stub.AuthorisationServiceStub.stubEnableAllCapabilities;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makeGetCall;

import uk.gov.moj.sjp.it.command.CreateCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("TODO: ATCM-2683 created to fix the test as it's not testing anything")
public class FindNotReadyCasesIT extends BaseIntegrationTest {

    private List<CreateCase.CreateCasePayloadBuilder> youngCases;
    private List<CreateCase.CreateCasePayloadBuilder> midCases;

    @Before
    public void init() {
        stubEnableAllCapabilities();

        //TODO ATCM-2683 created to fix the test as it's not testing anything
        youngCases = new ArrayList<>();
        midCases = new ArrayList<>();
    }

    @Test
    public void findNotReadyCasesGroupedByAgeRanges() {
        final JsonPath notReadyCases = getNotReadyCases();

        youngCases.forEach(CreateCase::createCaseForPayloadBuilder);
        midCases.forEach(CreateCase::createCaseForPayloadBuilder);

        final JsonPath updatedNotReadyCases = getNotReadyCases();

        assertThat(getCasesCount(updatedNotReadyCases, 0, 20),
                equalTo(getCasesCount(notReadyCases, 0, 20) + youngCases.size()));
        assertThat(getCasesCount(updatedNotReadyCases, 21, 27),
                equalTo(getCasesCount(notReadyCases, 21, 27) + midCases.size()));
    }

    private Integer getCasesCount(final JsonPath notReadyCases, final Integer ageFrom, final Integer ageTo) {
        return Optional.ofNullable(notReadyCases.getMap("caseCountsByAgeRanges.find { it.ageFrom == " + ageFrom + " && it.ageTo == " + ageTo + " }"))
                .map(range -> valueOf(range.get("casesCount").toString())).orElse(0);
    }

    private JsonPath getNotReadyCases() {
        final Response response = makeGetCall("/cases/not-ready-grouped-by-age", "application/vnd.sjp.query.not-ready-cases-grouped-by-age+json");
        return JsonPath.with(response.readEntity(String.class));
    }
}
