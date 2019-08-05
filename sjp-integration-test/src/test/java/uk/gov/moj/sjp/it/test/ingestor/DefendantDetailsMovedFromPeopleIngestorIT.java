package uk.gov.moj.sjp.it.test.ingestor;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.cpp.sjp.event.CaseReceived.EVENT_NAME;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.test.ingestor.helper.IngesterHelper.buildEnvelope;
import static uk.gov.moj.sjp.it.test.ingestor.helper.IngesterHelper.jsonFromString;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexFinderUtil;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.test.BaseIntegrationTest;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonString;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DefendantDetailsMovedFromPeopleIngestorIT extends BaseIntegrationTest {

    private final static String STUB_DATA_SJP_EVENTS_DEFENDANT_DETAILS_MOVED_FROM_PEOPLE = "stub-data/sjp.events.defendant-details-moved-from-people.json";
    private final static String SJP_EVENTS_DEFENDANT_DETAILS_MOVED_FROM_PEOPLE = "sjp.events.defendant-details-moved-from-people";
    private static final String SJP_EVENT = "sjp.event";

    private final MessageProducerClient privateEventsProducer = new MessageProducerClient();

    private ElasticSearchIndexFinderUtil elasticSearchIndexFinderUtil;
    private final Poller poller = new Poller(1200, 1000L);

    @Before
    public void setUp() throws IOException {
        privateEventsProducer.startProducer(SJP_EVENT);
        final ElasticSearchClient elasticSearchClient = new ElasticSearchClient();
        elasticSearchIndexFinderUtil = new ElasticSearchIndexFinderUtil(elasticSearchClient);
        new ElasticSearchIndexRemoverUtil().deleteAndCreateCaseIndex();
    }

    @After
    public void tearDown() {
        privateEventsProducer.close();
    }

    @Test
    public void shouldIngestDefendantDetailsMovedFromPeopleEvent() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final CreateCase.CreateCasePayloadBuilder createCase = getCreateCasePayloadBuilder(caseId, defendantId);

        //Create case in index
        setUpCaseAndDefendants(createCase);

        //Raise defendantDetailsMovedFromPeopleEvent
        publishDefendantDetailsMovedFromPeopleEvent(caseId.toString(), defendantId.toString());

        //Retrieve CaseDetails with updated DefendantDetails from people
        final JsonObject jsonObject = retrieveCaseDetailsWithDefendantDetailsUpdatedFromPeople();

        //Verify CaseDetails returned have party/DefendantDetails updated
        verifyCaseDefendantDetailUpdated(createCase, jsonObject, defendantId.toString());
    }

    private void setUpCaseAndDefendants(final CreateCase.CreateCasePayloadBuilder createCase) {
        new EventListener()
                .subscribe(EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(createCase))
                .popEvent(EVENT_NAME);

        final Optional<JsonObject> caseCreatedResponseObject = getCaseIndexDetails();

        final JsonObject outputCase = jsonFromString(getJsonArray(caseCreatedResponseObject.get(), "index").get().getString(0));
        assertThat(createCase.getId().toString(), is(outputCase.getString("caseId")));
    }

    private Optional<JsonObject> getCaseIndexDetails() {
        return poller.pollUntilFound(() -> {
                try {
                    final JsonObject jsonObject = elasticSearchIndexFinderUtil.findAll("crime_case_index");
                    if (jsonObject.getInt("totalResults") == 1) {
                        return of(jsonObject);
                    }
                } catch (final IOException e) {
                    fail();
                }
                return empty();
            });
    }

    private CreateCase.CreateCasePayloadBuilder getCreateCasePayloadBuilder(final UUID caseId, final UUID defendantId) {
        return CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withProsecutingAuthority(TFL)
                .withDefendantId(defendantId);
    }

    private void publishDefendantDetailsMovedFromPeopleEvent(final String caseId, final String defendantId) {
        //stubForUserDetails(tvlUserUid, ProsecutingAuthority.TVL);

        final String payloadDefendantDetailsMovedFromPeople = getPayload(STUB_DATA_SJP_EVENTS_DEFENDANT_DETAILS_MOVED_FROM_PEOPLE)
                .replace("CASE_ID", caseId)
                .replace("DEFENDANT_ID", defendantId);

        final JsonEnvelope jsonEnvelopeForDefendantDetailsMovedFromPeople = buildEnvelope(payloadDefendantDetailsMovedFromPeople, SJP_EVENTS_DEFENDANT_DETAILS_MOVED_FROM_PEOPLE);

        privateEventsProducer.sendMessage(SJP_EVENTS_DEFENDANT_DETAILS_MOVED_FROM_PEOPLE, jsonEnvelopeForDefendantDetailsMovedFromPeople);
    }

    private JsonObject retrieveCaseDetailsWithDefendantDetailsUpdatedFromPeople() {
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

        final JsonObject jsonObject = jsonFromString(getJsonArray(caseCreatedResponseObject.get(), "index").get().getString(0));
        return jsonObject;
    }

    private boolean checkDefendantUpdated(final JsonObject jsonObject) {
        return ((JsonString) jsonObject.getJsonArray("index").get(0)).getString().contains("SMITH");
    }

    private void verifyCaseDefendantDetailUpdated(final CreateCase.CreateCasePayloadBuilder createCase, final JsonObject jsonObject, final String defendantId) {
        with(jsonObject.toString())
                .assertThat("caseId", is(createCase.getId().toString()))
                .assertThat("prosecutingAuthority", is(createCase.getProsecutingAuthority().name()))
                .assertThat("caseReference", is(createCase.getUrn()))
                .assertThat("caseStatus", is("NO_PLEA_RECEIVED"))
                .assertThat("_case_type", is("prosecution"))
                .assertThat("_is_crown", is("false"))
                .assertThat("_is_magistrates", is("false"))
                .assertThat("_is_sjp", is("true"))
                .assertThat("_is_charging", is("false"))
                .assertThat("parties[0]._party_type", is("defendant"))
                .assertThat("parties[0].firstName", is("David"))
                .assertThat("parties[0].lastName", is("SMITH")) //updated lastName
                .assertThat("parties[0].gender", is("Male"))
                .assertThat("parties[0].dateOfBirth", is("1980-07-15"))
                .assertThat("parties[0].addressLines", is("14 Shaftesbury Road London England UK Greater London")) //updated addressLines
                .assertThat("parties[0].postCode", is("EC2 2HJ")) //updated postCode
                .assertThat("parties[0].partyId", is(defendantId));
    }

}
