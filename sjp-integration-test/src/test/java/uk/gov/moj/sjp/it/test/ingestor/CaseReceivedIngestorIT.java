package uk.gov.moj.sjp.it.test.ingestor;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonArray;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.cpp.sjp.event.CaseReceived.EVENT_NAME;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.test.ingestor.helper.AddressVerificationHelper.addressLinesFrom;
import static uk.gov.moj.sjp.it.test.ingestor.helper.IngesterHelper.jsonFromString;

import uk.gov.justice.services.test.utils.core.messaging.Poller;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchClient;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexFinderUtil;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.test.BaseIntegrationTest;

import java.io.IOException;
import java.util.Optional;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

public class CaseReceivedIngestorIT extends BaseIntegrationTest {

    private ElasticSearchIndexFinderUtil elasticSearchIndexFinderUtil;
    private final Poller poller = new Poller(1200, 1000L);

    @Before
    public void setUp() throws IOException {
        final ElasticSearchClient elasticSearchClient = new ElasticSearchClient();
        elasticSearchIndexFinderUtil = new ElasticSearchIndexFinderUtil(elasticSearchClient);
        new ElasticSearchIndexRemoverUtil().deleteAndCreateCaseIndex();
    }

    @Test
    public void shouldIngestCaseReceivedEvent() {
        final CreateCasePayloadBuilder createCase = CreateCasePayloadBuilder
                .withDefaults()
                .withId(randomUUID())
                .withProsecutingAuthority(TFL)
                .withDefendantId(randomUUID());

        new EventListener()
                .subscribe(EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(createCase))
                .popEvent(EVENT_NAME);

        final Optional<JsonObject> caseCreatedResponseObject = poller.pollUntilFound(() -> {
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

        final JsonObject outputCase = jsonFromString(getJsonArray(caseCreatedResponseObject.get(), "index").get().getString(0));

        //Case
        assertThat(createCase.getId().toString(), is(outputCase.getString("caseId")));
        assertThat(createCase.getUrn(), is(outputCase.getString("caseReference")));
        assertThat(createCase.getPostingDate().toString(), is(outputCase.getString("sjpNoticeServed")));
        assertThat(createCase.getProsecutingAuthority().name(), is(outputCase.getString("prosecutingAuthority")));
        assertThat(outputCase.getString("caseStatus"), is("NO_PLEA_RECEIVED"));
        assertThat(outputCase.getString("_case_type"), is("PROSECUTION"));
        assertThat(outputCase.getBoolean("_is_sjp"), is(true));
        assertThat(outputCase.getBoolean("_is_magistrates"), is(false));
        assertThat(outputCase.getBoolean("_is_crown"), is(false));
        assertThat(outputCase.getBoolean("_is_charging"), is(false));

        //Party
        final JsonObject outputParties = (JsonObject) outputCase.getJsonArray("parties").get(0);
        final CreateCase.DefendantBuilder defendantBuilder = createCase.getDefendantBuilder();

        assertThat(defendantBuilder.getId().toString(), is(outputParties.getString("partyId")));
        assertThat(defendantBuilder.getFirstName(), is(outputParties.getString("firstName")));
        assertThat(defendantBuilder.getLastName(), is(outputParties.getString("lastName")));
        assertThat(defendantBuilder.getTitle(), is(outputParties.getString("title")));
        assertThat(defendantBuilder.getDateOfBirth().toString(), is(outputParties.getString("dateOfBirth")));
        assertThat(defendantBuilder.getGender().toString(), is(outputParties.getString("gender")));
        assertThat(outputParties.getString("_party_type"), is("defendant"));

        final JsonObject aliases = outputParties.getJsonArray("aliases").getJsonObject(0);
        assertAliases(aliases, defendantBuilder);

        //Address
        assertThat(addressLinesFrom(defendantBuilder), is(outputParties.getString("addressLines")));
        assertThat(defendantBuilder.getAddressBuilder().getPostcode(), is(outputParties.getString("postCode")));
    }

    private void assertAliases(final JsonObject aliases, final CreateCase.DefendantBuilder defendantBuilder) {
        assertThat(defendantBuilder.getTitle(), is(aliases.getString("title")));
        assertThat(defendantBuilder.getFirstName(), is(aliases.getString("firstName")));
        assertThat(defendantBuilder.getLastName(), is(aliases.getString("lastName")));
    }
}
