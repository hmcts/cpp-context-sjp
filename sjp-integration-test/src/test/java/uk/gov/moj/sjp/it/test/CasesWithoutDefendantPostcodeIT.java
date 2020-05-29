package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollCasesWithoutDefendantPostcodeUntileResponseIsJson;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryForAllProsecutors;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder;
import uk.gov.moj.sjp.it.command.CreateCase.OffenceBuilder;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class CasesWithoutDefendantPostcodeIT extends BaseIntegrationTest {

    private final SjpDatabaseCleaner cleaner = new SjpDatabaseCleaner();

    private static final String NATIONAL_COURT_CODE = "1080";

    @Before
    public void setUp() throws Exception {
        cleaner.cleanViewStore();
        stubQueryForAllProsecutors();
        stubRegionByPostcode(NATIONAL_COURT_CODE, "TestRegion");
    }

    @Test
    public void shouldListAllCasesWithoutDefendantPostcode() {
        final UUID caseId1 = randomUUID();
        final String urn1 = "TFL1";
        final LocalDate postingDate1 = LocalDate.now().minusDays(7);
        final UUID caseId2 = randomUUID();
        final String urn2 = "TFL2";
        final LocalDate postingDate2 = LocalDate.now().minusDays(9);
        createCaseWithoutDefendantPostcode(caseId1, urn1, postingDate1);
        createCaseWithoutDefendantPostcode(caseId2, urn2, postingDate2);

        pollCasesWithoutDefendantPostcodeUntileResponseIsJson(allOf(
                withJsonPath("$.cases[0].id", is(caseId1.toString())),
                withJsonPath("$.cases[0].urn", is(urn1)),
                withJsonPath("$.cases[0].firstName", is("David")),
                withJsonPath("$.cases[0].lastName", is("LLOYD")),
                withJsonPath("$.cases[0].postingDate", is(postingDate1.toString())),
                withJsonPath("$.cases[0].prosecutingAuthority", is("Transport for London")),
                withJsonPath("$.cases[1].id", is(caseId2.toString())),
                withJsonPath("$.cases[1].urn", is(urn2)),
                withJsonPath("$.cases[1].firstName", is("David")),
                withJsonPath("$.cases[1].lastName", is("LLOYD")),
                withJsonPath("$.cases[1].postingDate", is(postingDate2.toString())),
                withJsonPath("$.cases[1].prosecutingAuthority", is("Transport for London")),
                withJsonPath("$.results",is(2)),
                withJsonPath("$.pageCount",is(1))
        ));
    }

    private void createCaseWithoutDefendantPostcode(
            final UUID caseId,
            final String urn,
            final LocalDate postingDate) {

        final CreateCasePayloadBuilder createCase = CreateCasePayloadBuilder
                .withDefaults()
                .withUrn(urn)
                .withId(caseId)
                .withOffenceBuilders(
                        OffenceBuilder.withDefaults().withId(randomUUID()),
                        OffenceBuilder.withDefaults().withId(randomUUID())
                )
                .withPostingDate(postingDate);

        createCase.getDefendantBuilder().getAddressBuilder().withPostcode(null);

        final Optional<JsonEnvelope> caseReceivedEvent = new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCase))
                .popEvent(CaseReceived.EVENT_NAME);

        assertThat(caseReceivedEvent.isPresent(), is(true));
    }
}
