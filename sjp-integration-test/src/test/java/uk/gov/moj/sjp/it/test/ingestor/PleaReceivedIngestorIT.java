package uk.gov.moj.sjp.it.test.ingestor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubCountryByPostcodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.test.ingestor.helper.ElasticSearchQueryHelper.getCaseFromElasticSearch;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;

import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.PleasSet;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.command.builder.AddressBuilder;
import uk.gov.moj.sjp.it.framework.util.ViewStoreCleaner;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.PleadOnlineHelper;
import uk.gov.moj.sjp.it.test.BaseIntegrationTest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PleaReceivedIngestorIT extends BaseIntegrationTest {
    private static final String ONLINE_PLEA_PAYLOAD = "raml/json/sjp.command.plead-online__not-guilty_for_ingester.json";

    private EventListener eventListener = new EventListener();


    private CreateCasePayloadBuilder casePayloadBuilder;
    private CreateCase.DefendantBuilder defendantBuilder;

    private final UUID caseIdOne = randomUUID();
    private final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();
    private static final String NATIONAL_COURT_CODE = "1080";


    @After
    public void cleanDatabase() {
        viewStoreCleaner.cleanDataInViewStore(caseIdOne);
    }

    @Before
    public void setUp() throws IOException {
        new ElasticSearchIndexRemoverUtil().deleteAndCreateCaseIndex();

        final AddressBuilder addressBuilder = AddressBuilder.withDefaults().withPostcode("W1T 1JY");
        defendantBuilder = CreateCase.DefendantBuilder.defaultDefendant().withAddressBuilder(addressBuilder)
                .withFirstName("Johannes")
                .withLastName("Diamonds").withAddressBuilder(addressBuilder);
        casePayloadBuilder = CreateCasePayloadBuilder.withDefaults().withId(caseIdOne).
                withPostingDate(LocalDate.now())
                .withDefendantBuilder(defendantBuilder);
        final ProsecutingAuthority prosecutingAuthority = casePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubEnforcementAreaByPostcode(addressBuilder.getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, "TestRegion");

        createCaseForPayloadBuilder(this.casePayloadBuilder);

        pollUntilCaseByIdIsOk(casePayloadBuilder.getId());

        stubCountryByPostcodeQuery("W1T 1JY", "England");
    }

    @Test
    public void shouldIngestCaseReceivedEvent() {
        pleadOnline();

        final JsonObject outputCase = getCaseFromElasticSearch(casePayloadBuilder.getId().toString());

        verifyElasticSearchResponse(outputCase);
    }

    private void verifyElasticSearchResponse(final JsonObject casePayload) {

        assertThat(casePayload.getString("caseId"), is(casePayloadBuilder.getId().toString()));
        final JsonObject firstParty = casePayload.getJsonArray("parties").getJsonObject(0);
        assertThat(firstParty.getString("partyId"), is(casePayloadBuilder.getDefendantBuilder().getId().toString()));
        assertThat(firstParty.getString("firstName"), is(defendantBuilder.getFirstName()));
        assertThat(firstParty.getString("lastName"), is(defendantBuilder.getLastName()));
        assertThat(firstParty.getString("dateOfBirth"), is(defendantBuilder.getDateOfBirth().toString()));
        assertThat(firstParty.getString("postCode"), is(defendantBuilder.getAddressBuilder().getPostcode()));
        assertThat(firstParty.getString("addressLines"), is(buildAddressLines(defendantBuilder.getAddressBuilder())));

    }

    private String buildAddressLines(final AddressBuilder addressBuilder) {
        return String.join(" ", addressBuilder.getAddress1(), addressBuilder.getAddress2(), addressBuilder.getAddress3(), addressBuilder.getAddress4(), addressBuilder.getAddress5());
    }

    private JSONObject pleadOnline() {

        final JSONObject pleaPayload = getOnlinePleaPayload();

        final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(casePayloadBuilder.getId());

        eventListener
                .subscribe(PleasSet.EVENT_NAME)
                .run(() -> pleadOnlineHelper.pleadOnline(pleaPayload.toString()));

        return pleaPayload;
    }

    private JSONObject getOnlinePleaPayload() {
        final String templateRequest = getPayload(ONLINE_PLEA_PAYLOAD);

        final JSONObject jsonObject = new JSONObject(templateRequest);
        jsonObject.getJSONArray("offences")
                .getJSONObject(0)
                .put("id", casePayloadBuilder.getOffenceId().toString());

        return jsonObject;
    }
}
