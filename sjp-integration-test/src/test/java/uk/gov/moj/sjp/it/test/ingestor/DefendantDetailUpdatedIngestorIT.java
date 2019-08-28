package uk.gov.moj.sjp.it.test.ingestor;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.cpp.sjp.event.CaseReceived.EVENT_NAME;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.command.UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.test.ingestor.helper.ElasticSearchQueryHelper.getPoller;
import static uk.gov.moj.sjp.it.test.ingestor.helper.IngesterHelper.jsonFromString;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexFinderUtil;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.command.UpdateDefendantDetails;
import uk.gov.moj.sjp.it.command.builder.AddressBuilder;
import uk.gov.moj.sjp.it.framework.util.ViewStoreCleaner;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.test.BaseIntegrationTest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DefendantDetailUpdatedIngestorIT extends BaseIntegrationTest {

    private static final String LABEL_TITLE = "title";
    private static final String LABEL_FIRST_NAME = "firstName";
    private static final String _LABEL_LAST_NAME = "lastName";
    private static final String INDEX_LABEL = "index";
    private static final String TITLE = "Mr";
    private static final String FIRST_NAME = "Jonathan";
    private static final String LAST_NAME = "Alpanso";
    private static final String POST_CODE = "IG6 1JY";

    private final UUID caseIdOne = randomUUID();
    private final Poller poller = getPoller();
    private ElasticSearchIndexFinderUtil elasticSearchIndexFinderUtil;
    private final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();

    @Before
    public void setUp() throws IOException {
        final ElasticSearchClient elasticSearchClient = new ElasticSearchClient();
        elasticSearchIndexFinderUtil = new ElasticSearchIndexFinderUtil(elasticSearchClient);
        new ElasticSearchIndexRemoverUtil().deleteAndCreateCaseIndex();
    }

    @After
    public void cleanDatabase() {
        viewStoreCleaner.cleanDataInViewStore(caseIdOne);
    }

    @Test
    public void should_Ingest_Update_Defendant_Details() {

        pushDefendantDetailsUpdatedEvent(getDefendantPayloadBuilder());

        final Optional<JsonObject> caseCreatedResponseObject = queryElasticSearch(FIRST_NAME);

        final JsonObject outputCase = jsonFromString(getJsonArray(caseCreatedResponseObject.get(), INDEX_LABEL).get().getString(0));
        final JsonObject defendant = (JsonObject) outputCase.getJsonArray("parties").get(0);
        final JsonArray aliases = defendant.getJsonArray("aliases");

        assertThat(aliases.size(), is(2));
        assertThat(defendant.getString("_party_type"), is("DEFENDANT"));
        assertThat(defendant.getString(LABEL_TITLE), is(TITLE));
        assertThat(defendant.getString(LABEL_FIRST_NAME), is(FIRST_NAME));
        assertThat(defendant.getString(_LABEL_LAST_NAME), is(LAST_NAME));
        assertThat(defendant.getString("dateOfBirth"), is("1981-08-16"));
        assertThat(defendant.getString("gender"), is("Female"));
        assertThat(defendant.getString("addressLines"), is("14 Shaftesbury Road Croydon Wales US New London"));
        assertThat(defendant.getString("postCode"), is(POST_CODE));
        assertAliases(aliases);

    }

    @Test
    public void should_Ingest_Update_Defendant_Details_With_No_Name_Change() {
        final UpdateDefendantDetails.DefendantDetailsPayloadBuilder builder = UpdateDefendantDetails.DefendantDetailsPayloadBuilder
                .withDefaults()
                .withDateOfBirth(LocalDate.of(1911, 8, 16))
                .withLastName("LLOYD");

        pushDefendantDetailsUpdatedEvent(builder);

        final Optional<JsonObject> caseCreatedResponseObject = queryElasticSearch("1911-08-16");

        final JsonObject outputCase = jsonFromString(getJsonArray(caseCreatedResponseObject.get(), INDEX_LABEL).get().getString(0));
        final JsonObject defendant = (JsonObject) outputCase.getJsonArray("parties").get(0);
        final JsonArray aliases = defendant.getJsonArray("aliases");

        assertThat(aliases.size(), is(1));
    }

    private void pushDefendantDetailsUpdatedEvent(final UpdateDefendantDetails.DefendantDetailsPayloadBuilder builder) {
        new EventListener()
                .subscribe(EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(CreateCase.CreateCasePayloadBuilder.withDefaults().withId(caseIdOne)));

        final UUID defendantId = UUID.fromString(pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id"));
        updateDefendantDetailsForCaseAndPayload(caseIdOne, defendantId, builder);
    }

    private Optional<JsonObject> queryElasticSearch(final String criteria) {

        return poller.pollUntilFound(() -> {
            try {
                final JsonObject jsonObject = elasticSearchIndexFinderUtil.findAll("crime_case_index");
                if (jsonObject.getInt("totalResults") == 1 && checkDefendantUpdated(jsonObject, criteria)) {
                    return of(jsonObject);
                }
            } catch (final IOException e) {
                fail();
            }
            return empty();
        });
    }

    private void assertAliases(final JsonArray aliases) {

        for (int i = 0; i < aliases.size(); i++) {
            final JsonObject alias = (JsonObject) aliases.get(i);
            assertThat(alias.getString(LABEL_TITLE), anyOf(equalTo(TITLE)));
            assertThat(alias.getString(LABEL_FIRST_NAME), anyOf(equalTo(FIRST_NAME), equalTo("David")));
            assertThat(alias.getString(_LABEL_LAST_NAME), anyOf(equalTo(LAST_NAME), equalTo("LLOYD")));
        }
    }


    private UpdateDefendantDetails.DefendantDetailsPayloadBuilder getDefendantPayloadBuilder() {
        AddressBuilder addressBuilder = AddressBuilder.withDefaults()
                .withAddress1("14 Shaftesbury Road")
                .withAddress2("Croydon")
                .withAddress3("Wales")
                .withAddress4("US")
                .withAddress5("New London")
                .withPostcode(POST_CODE);

        return UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults()
                .withTitle(TITLE)
                .withFirstName(FIRST_NAME)
                .withLastName(LAST_NAME)
                .withDateOfBirth(LocalDate.of(1981, 8, 16))
                .withGender(Gender.FEMALE)
                .withAddress(addressBuilder);
    }

    private boolean checkDefendantUpdated(final JsonObject jsonObject, final String criteria) {
        return ((JsonString) jsonObject.getJsonArray(INDEX_LABEL).get(0)).getString().indexOf(criteria) > -1;
    }
}
