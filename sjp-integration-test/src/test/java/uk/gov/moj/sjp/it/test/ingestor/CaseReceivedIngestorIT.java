package uk.gov.moj.sjp.it.test.ingestor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.cpp.sjp.event.CaseReceived.EVENT_NAME;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.test.ingestor.helper.AddressVerificationHelper.addressLinesFrom;
import static uk.gov.moj.sjp.it.test.ingestor.helper.ElasticSearchQueryHelper.getCaseFromElasticSearch;

import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.framework.util.ViewStoreCleaner;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.test.BaseIntegrationTest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CaseReceivedIngestorIT extends BaseIntegrationTest {

    private final UUID uuid = randomUUID();
    private final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();
    private static final String NATIONAL_COURT_CODE = "1080";


    @Before
    public void setUp() throws IOException {
        new ElasticSearchIndexRemoverUtil().deleteAndCreateCaseIndex();
    }

    @After
    public void cleanDatabase() {
        viewStoreCleaner.cleanDataInViewStore(uuid);
    }

    @Test
    public void shouldIngestCaseReceivedEvent() {
        final CreateCasePayloadBuilder createCase = CreateCasePayloadBuilder
                .withDefaults()
                .withId(uuid)
                .withProsecutingAuthority(TFL)
                .withDefendantId(randomUUID())
                .withPostingDate(LocalDate.now());//required or status will randomly change to NO_PLEA_RECEIVED_WAITING_FOR_DECISION

        stubEnforcementAreaByPostcode(createCase.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, "TestRegion");

        new EventListener()
                .subscribe(EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(createCase))
                .popEvent(EVENT_NAME);

        final JsonObject outputCase = getCaseFromElasticSearch(uuid.toString());

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
        assertThat(outputParties.getString("_party_type"), is("DEFENDANT"));

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
