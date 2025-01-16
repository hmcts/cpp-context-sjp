package uk.gov.moj.sjp.it.test.ingestor;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.Constants.SJP_EVENT;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.test.ingestor.helper.CasePredicate.casePayloadContains;
import static uk.gov.moj.sjp.it.test.ingestor.helper.ElasticSearchQueryHelper.getCaseFromElasticSearch;
import static uk.gov.moj.sjp.it.test.ingestor.helper.ElasticSearchQueryHelper.getCaseFromElasticSearchWithPredicate;
import static uk.gov.moj.sjp.it.test.ingestor.helper.IngesterHelper.buildEnvelope;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.framework.util.ViewStoreCleaner;
import uk.gov.moj.sjp.it.test.BaseIntegrationTest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefendantDetailsMovedFromPeopleIngestorIT extends BaseIntegrationTest {

    private final static String STUB_DATA_SJP_EVENTS_DEFENDANT_DETAILS_MOVED_FROM_PEOPLE = "stub-data/sjp.events.defendant-details-moved-from-people.json";
    private final static String SJP_EVENTS_DEFENDANT_DETAILS_MOVED_FROM_PEOPLE = "sjp.events.defendant-details-moved-from-people";

    private final MessageProducerClient privateEventsProducer = new MessageProducerClient();

    private final UUID caseId = randomUUID();
    private final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();
    private static final String NATIONAL_COURT_CODE = "1080";

    @BeforeEach
    public void setUp() throws IOException {
        privateEventsProducer.startProducer(SJP_EVENT);
        new ElasticSearchIndexRemoverUtil().deleteAndCreateCaseIndex();
    }

    @AfterEach
    public void tearDown() {
        viewStoreCleaner.cleanDataInViewStore(caseId);
        privateEventsProducer.close();
    }

    @Test
    public void shouldIngestDefendantDetailsMovedFromPeopleEvent() {
        final UUID defendantId = randomUUID();

        final CreateCase.CreateCasePayloadBuilder createCase = getCreateCasePayloadBuilder(caseId, defendantId);
        stubEnforcementAreaByPostcode(createCase.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, "TestRegion");

        //Create case in index
        setUpCaseAndDefendants(createCase);

        //Raise defendantDetailsMovedFromPeopleEvent
        publishDefendantDetailsMovedFromPeopleEvent(caseId.toString(), defendantId.toString());

        //Retrieve CaseDetails with updated DefendantDetails from people
        final JsonObject jsonObject = getCaseFromElasticSearchWithPredicate(casePayloadContains("SMITH"), caseId.toString());

        //Verify CaseDetails returned have party/DefendantDetails updated
        verifyCaseDefendantDetailUpdated(createCase, jsonObject, defendantId.toString());
    }

    private void setUpCaseAndDefendants(final CreateCase.CreateCasePayloadBuilder createCase) {
        createCaseForPayloadBuilder(createCase);

        final JsonObject outputCase = getCaseFromElasticSearch(createCase.getId().toString());
        assertThat(createCase.getId().toString(), is(outputCase.getString("caseId")));
    }

    private CreateCase.CreateCasePayloadBuilder getCreateCasePayloadBuilder(final UUID caseId, final UUID defendantId) {
        return withDefaults()
                .withId(caseId)
                .withProsecutingAuthority(TFL)
                .withDefendantId(defendantId)
                .withPostingDate(LocalDate.now());//required or status will randomly change to NO_PLEA_RECEIVED_WAITING_FOR_DECISION
    }

    private void publishDefendantDetailsMovedFromPeopleEvent(final String caseId, final String defendantId) {

        final String payloadDefendantDetailsMovedFromPeople = getPayload(STUB_DATA_SJP_EVENTS_DEFENDANT_DETAILS_MOVED_FROM_PEOPLE)
                .replace("CASE_ID", caseId)
                .replace("DEFENDANT_ID", defendantId);

        final JsonEnvelope jsonEnvelopeForDefendantDetailsMovedFromPeople = buildEnvelope(payloadDefendantDetailsMovedFromPeople, SJP_EVENTS_DEFENDANT_DETAILS_MOVED_FROM_PEOPLE);

        privateEventsProducer.sendMessage(SJP_EVENTS_DEFENDANT_DETAILS_MOVED_FROM_PEOPLE, jsonEnvelopeForDefendantDetailsMovedFromPeople);
    }

    private void verifyCaseDefendantDetailUpdated(final CreateCase.CreateCasePayloadBuilder createCase, final JsonObject jsonObject, final String defendantId) {
        final JsonObject defendant = (JsonObject) jsonObject.getJsonArray("parties").get(0);
        final JsonArray aliases = defendant.getJsonArray("aliases");

        with(jsonObject.toString())
                .assertThat("caseId", is(createCase.getId().toString()))
                .assertThat("prosecutingAuthority", is(createCase.getProsecutingAuthority().name()))
                .assertThat("caseReference", is(createCase.getUrn()))
                .assertThat("caseStatus", is("NO_PLEA_RECEIVED"))
                .assertThat("_case_type", is("PROSECUTION"))
                .assertThat("_is_crown", is(false))
                .assertThat("_is_magistrates", is(false))
                .assertThat("_is_sjp", is(true))
                .assertThat("_is_charging", is(false))
                .assertThat("parties[0]._party_type", is("DEFENDANT"))
                .assertThat("parties[0].firstName", is("David"))
                .assertThat("parties[0].lastName", is("SMITH")) //updated lastName
                .assertThat("parties[0].gender", is("Male"))
                .assertThat("parties[0].dateOfBirth", is("1980-07-15"))
                .assertThat("parties[0].addressLines", is("14 Shaftesbury Road London England UK Greater London")) //updated addressLines
                .assertThat("parties[0].postCode", is("EC2 2HJ")) //updated postCode
                .assertThat("parties[0].partyId", is(defendantId));
        assertAliases(aliases);
    }

    private void assertAliases(final JsonArray aliases) {

        for (int i = 0; i < aliases.size(); i++) {
            final JsonObject alias = (JsonObject) aliases.get(i);
            assertThat(alias.getString("title"), anyOf(equalTo("Mr"), equalTo("Dr")));
            assertThat(alias.getString("firstName"), equalTo("David"));
            assertThat(alias.getString("lastName"), anyOf(equalTo("LLOYD"), equalTo("SMITH")));
        }
    }
}
