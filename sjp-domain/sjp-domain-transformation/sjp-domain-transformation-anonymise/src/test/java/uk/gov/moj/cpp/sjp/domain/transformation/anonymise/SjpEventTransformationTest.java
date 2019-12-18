package uk.gov.moj.cpp.sjp.domain.transformation.anonymise;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.justice.tools.eventsourcing.anonymization.util.FileUtil;

import java.io.StringReader;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;


import com.jayway.restassured.path.json.JsonPath;
import org.junit.Test;

public class SjpEventTransformationTest {

    private AnonymiseUtil anonymiseUtil;
    private JsonPath inputJsonPath;
    private JsonObject anonymisedJsonObject;

    public void initialize(final String eventName) {
        anonymiseUtil = new AnonymiseUtil().apply(eventName);
        inputJsonPath = anonymiseUtil.getInputJsonPath();
        anonymisedJsonObject = anonymiseUtil.getAnonymisedJsonObject();
    }

    @Test
    public void sjp_events_case_received() {
        initialize("sjp.events.case-received");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.urn", is(inputJsonPath.getString("urn"))),
                        withJsonPath("$.enterpriseId", is(inputJsonPath.getString("enterpriseId"))),
                        withJsonPath("$.prosecutingAuthority", is(inputJsonPath.getString("prosecutingAuthority"))),
                        withJsonPath("$.defendant.offences[*].libraOffenceCode", is(inputJsonPath.getList("defendant.offences.libraOffenceCode"))),
                        withJsonPath("$.defendant.gender", is(inputJsonPath.getString("defendant.gender"))),
                        withJsonPath("$.defendant.title", is(inputJsonPath.getString("defendant.title")))
                        )
                ));
    }

    @Test
    public void sjp_events_defendant_details_moved_from_people() {
        initialize("sjp.events.defendant-details-moved-from-people");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.gender", is(inputJsonPath.getString("gender"))),
                        withJsonPath("$.title", is(inputJsonPath.getString("title")))
                        )
                ));
    }

    @Test
    public void sjp_events_financial_means_updated() {
        initialize("sjp.events.financial-means-updated");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.outgoings[*].description", is(inputJsonPath.getList("outgoings.description"))),
                        withJsonPath("$.outgoings[*].amount", is(inputJsonPath.getList("outgoings.amount")))
                        )
                ));
    }

    @Test
    public void sjp_events_sjp_case_created() {
        initialize("sjp.events.sjp-case-created");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.urn", is(inputJsonPath.getString("urn"))),
                        withJsonPath("$.prosecutingAuthority", is(inputJsonPath.getString("prosecutingAuthority"))),
                        withJsonPath("$.initiationCode", is(inputJsonPath.getString("initiationCode"))),
                        withJsonPath("$.ptiUrn", is(inputJsonPath.getString("ptiUrn"))),
                        withJsonPath("$.summonsCode", is(inputJsonPath.getString("summonsCode")))
                        )
                ));
    }

    @Test
    public void sjp_events_online_plea_received() {
        initialize("sjp.events.online-plea-received");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.urn", is(inputJsonPath.getString("urn"))),
                        withJsonPath("$.financialMeans.income.frequency", is(inputJsonPath.getString("financialMeans.income.frequency")))
                        )
                ));
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {
        JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }

    private static class AnonymiseUtil {
        private JsonPath inputJsonPath;
        private JsonObject inputJsonObject;
        private JsonObject anonymisedJsonObject;

        public JsonPath getInputJsonPath() {
            return inputJsonPath;
        }

        public JsonObject getAnonymisedJsonObject() {
            return anonymisedJsonObject;
        }

        public AnonymiseUtil apply(String eventName) {
            String fileContentsAsString = FileUtil.getFileContentsAsString(eventName + "/input.json");
            inputJsonPath = JsonPath.from(fileContentsAsString);

            inputJsonObject = jsonFromString(fileContentsAsString);
            final JsonEnvelope jsonEnvelope = EnvelopeFactory.createEnvelope(eventName, inputJsonObject);
            SjpEventTransformation st = new SjpEventTransformation();
            Stream<JsonEnvelope> apply = st.apply(jsonEnvelope);
            JsonEnvelope envelope = apply.findFirst().get();
            anonymisedJsonObject = envelope.payloadAsJsonObject();
            return this;
        }
    }
}
