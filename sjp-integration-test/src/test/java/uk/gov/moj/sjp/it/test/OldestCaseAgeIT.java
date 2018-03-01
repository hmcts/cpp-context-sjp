package uk.gov.moj.sjp.it.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makeGetCall;

import uk.gov.moj.sjp.it.helper.CaseSjpHelper;
import uk.gov.moj.sjp.it.stub.AuthorisationServiceStub;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

public class OldestCaseAgeIT extends BaseIntegrationTest {

    private long oldestCaseAge;

    @Before
    public void setup() {
        AuthorisationServiceStub.stubEnableAllCapabilities();
        //TODO: This assumes that cases this old won't be created anywhere else
        final LocalDate postingDate = LocalDate.of(2000, 1, 1);
        try (final CaseSjpHelper caseSjpHelper = new CaseSjpHelper(postingDate)) {
            caseSjpHelper.createAndVerifyCase();
            oldestCaseAge = ChronoUnit.DAYS.between(postingDate, LocalDate.now());
        }
    }

    @Test
    public void shouldGetOldestCaseAge() {
        final Response response = makeGetCall("/cases/oldest-age",
                "application/vnd.sjp.query.oldest-case-age+json");

        try (final JsonReader jsonReader = Json.createReader(new StringReader(response.readEntity(String.class)))) {
            final JsonObject body = jsonReader.readObject();
            assertThat(body.getInt("oldestCaseAge"), equalTo((int)oldestCaseAge));
        }
    }

}
