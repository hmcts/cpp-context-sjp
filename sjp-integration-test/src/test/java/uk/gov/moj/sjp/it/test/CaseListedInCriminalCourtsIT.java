package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.util.FileUtil.getFileContentAsJson;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

public class CaseListedInCriminalCourtsIT extends BaseIntegrationTest {

    private final UUID caseId = randomUUID();

    @Before
    public void setUp() {
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults().withId(caseId);
        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "DEFENDANT_REGION");

        new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder))
                .popEvent(CaseReceived.EVENT_NAME);
    }

    @Test
    public void shouldUpdateCaseListedCriminalCourts() {
        final String hearingCourtName = "Carmarthen Magistrates' Court";
        final ZonedDateTime hearingTime = now();
        final JsonObject payload = getFileContentAsJson("CaseListedInCriminalCourtsIT/case-listed-in-criminal-courts.json",
                ImmutableMap.<String, Object>builder()
                        .put("prosecutionCaseId", caseId)
                        .put("name", hearingCourtName)
                        .put("sittingDay", ZonedDateTimes.toString(hearingTime))
                        .build());

        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("public.progression.prosecution-cases-referred-to-court", payload);
        }

        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.listedInCriminalCourts", is(true)),
                withJsonPath("$.hearingCourtName", is(hearingCourtName)),
                withJsonPath("$.hearingTime", is(ZonedDateTimes.toString(hearingTime)))));
    }
}
