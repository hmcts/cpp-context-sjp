package uk.gov.moj.sjp.it.test.ingestor;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.cpp.sjp.event.CaseReceived.EVENT_NAME;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.command.UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.test.ingestor.helper.IngesterHelper.jsonFromString;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexFinderUtil;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.command.UpdateDefendantDetails;
import uk.gov.moj.sjp.it.command.builder.AddressBuilder;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.test.BaseIntegrationTest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonString;

import org.junit.Before;
import org.junit.Test;

public class DefendantDetailUpdatedIngestorIT extends BaseIntegrationTest {
    private final UUID caseIdOne = randomUUID();
    private final Poller poller = new Poller(1200, 1000L);
    private ElasticSearchIndexFinderUtil elasticSearchIndexFinderUtil;

    @Before
    public void setUp() throws IOException {
        final ElasticSearchClient elasticSearchClient = new ElasticSearchClient();
        elasticSearchIndexFinderUtil = new ElasticSearchIndexFinderUtil(elasticSearchClient);
        new ElasticSearchIndexRemoverUtil().deleteAndCreateCaseIndex();
    }

    @Test
    public void shouldIngestCaseReceivedEvent() {
        new EventListener()
                .subscribe(EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(CreateCase.CreateCasePayloadBuilder.withDefaults().withId(caseIdOne)));

        UUID defendantId = UUID.fromString(pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id"));
        updateDefendantDetailsForCaseAndPayload(caseIdOne, defendantId, getDefendantPayloadBuilder());

        final Optional<JsonObject> caseCreatedResponseObject = poller.pollUntilFound(() -> {
            try {
                final JsonObject jsonObject = elasticSearchIndexFinderUtil.findAll("crime_case_index");
                if (jsonObject.getInt("totalResults") == 1 && checkDefendantUpdated(jsonObject)) {
                    return of(jsonObject);
                }
            } catch (final IOException e) {
                fail();
            }
            return empty();
        });

        final JsonObject outputCase = jsonFromString(getJsonArray(caseCreatedResponseObject.get(), "index").get().getString(0));
        final JsonObject defendant = (JsonObject) outputCase.getJsonArray("parties").get(0);

        assertThat(getStringFromJson(defendant, "_party_type"), is("defendant"));
        assertThat(getStringFromJson(defendant, "title"), is("Mr"));
        assertThat(getStringFromJson(defendant, "firstName"), is("Jonathan"));
        assertThat(getStringFromJson(defendant, "lastName"), is("Alpanso"));
        assertThat(getStringFromJson(defendant, "dateOfBirth"), is("1981-08-16"));
        assertThat(getStringFromJson(defendant, "gender"), is("Female"));
        assertThat(getStringFromJson(defendant, "addressLines"), is("14 Shaftesbury Road Croydon Wales US New London"));
        assertThat(getStringFromJson(defendant, "postCode"), is("IG6 1JY"));
    }

    private UpdateDefendantDetails.DefendantDetailsPayloadBuilder getDefendantPayloadBuilder() {
        AddressBuilder addressBuilder = AddressBuilder.withDefaults()
                .withAddress1("14 Shaftesbury Road")
                .withAddress2("Croydon")
                .withAddress3("Wales")
                .withAddress4("US")
                .withAddress5("New London")
                .withPostcode("IG6 1JY");

        return UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults()
                .withTitle("Mr")
                .withFirstName("Jonathan")
                .withLastName("Alpanso")
                .withDateOfBirth(LocalDate.of(1981, 8, 16))
                .withGender(Gender.FEMALE)
                .withAddress(addressBuilder);
    }

    private String getStringFromJson(final JsonObject transformedJson, final String child) {
        return transformedJson.getString(child);
    }

    private boolean checkDefendantUpdated(final JsonObject jsonObject) {
        return ((JsonString) jsonObject.getJsonArray("index").get(0)).getString().indexOf("Jonathan") > -1;
    }
}
