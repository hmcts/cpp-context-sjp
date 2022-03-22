package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.defaultCaseBuilder;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.UnifiedSearchStub.stubUnifiedSearchQueryForCases;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

/**
 * Integration test to find if defendant has potential cases using unified search query
 */
public class DefendantPotentialCaseIT extends BaseIntegrationTest {

    private static final String SJP_EVENT = "sjp.event";
    private static final String CASE_REF = "Potential case reference";
    private final MessageProducerClient privateEventsProducer = new MessageProducerClient();
    private UUID randomCaseId;

    @Before
    public void before() {
        randomCaseId = UUID.randomUUID();
        stubUnifiedSearchQueryForCases(randomCaseId, CASE_REF);
    }

    @Test
    public void shouldReturnFalseForPotentialCases() {
        UUID caseId =  UUID.randomUUID();
        UUID defendantId =  UUID.randomUUID();
        privateEventsProducer.startProducer(SJP_EVENT);
        final ProsecutingAuthority prosecutingAuthority = TFL;
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        final CreateCase.CreateCasePayloadBuilder createCase = createCase(caseId,
                defendantId, prosecutingAuthority, LocalDate.of(1980, 11, 10));

        final Optional<JsonEnvelope> caseReceivedEvent = new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCase))
                .popEvent(CaseReceived.EVENT_NAME);
        assertTrue(caseReceivedEvent.isPresent());

        final boolean hasPotentialCase = CasePoller.pollUntilCaseByIdIsOk(caseId).get("hasPotentialCase");
        assertFalse(hasPotentialCase);
    }

    @Test
    public void shouldReturnTrueForPotentialCases()  {
        UUID caseId =  UUID.randomUUID();
        UUID defendantId = randomUUID();

        privateEventsProducer.startProducer(SJP_EVENT);
        final ProsecutingAuthority prosecutingAuthority = TFL;
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        final CreateCase.CreateCasePayloadBuilder createCase = createCase(caseId,
                defendantId,
                prosecutingAuthority,
                LocalDate.of(1980, 10, 10));

        final Optional<JsonEnvelope> caseReceivedEvent = new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCase))
                .popEvent(CaseReceived.EVENT_NAME);
        assertTrue(caseReceivedEvent.isPresent());

        final boolean hasPotentialCase = CasePoller.pollUntilCaseByIdIsOk(caseId).get("hasPotentialCase");
        assertTrue(hasPotentialCase);
    }

    @Test
    public void shouldFindPotentialCases()  {
        UUID caseId = randomCaseId;
        UUID defendantId =  UUID.randomUUID();
        privateEventsProducer.startProducer(SJP_EVENT);

        final ProsecutingAuthority prosecutingAuthority = TFL;
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        final CreateCase.CreateCasePayloadBuilder createCase = createCase(caseId,
                defendantId,
                prosecutingAuthority,
                LocalDate.of(1980, 10, 15));
        final Optional<JsonEnvelope> caseReceivedEvent = new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCase))
                .popEvent(CaseReceived.EVENT_NAME);
        assertTrue(caseReceivedEvent.isPresent());

        final String potentialCasesResponsePayload = CasePoller.pollUntilPotentialCasesByDefendantIdIsOk(defendantId);
        final JsonObject potentialCases = responseToJsonObject(potentialCasesResponsePayload);
        final JsonArray sjpOpenCases = potentialCases.getJsonArray("sjpOpenCases");
        assertEquals(1, sjpOpenCases.size());

        final JsonObject sjpOpenCase = sjpOpenCases.getJsonObject(0);
        assertEquals(caseId.toString(), sjpOpenCase.getString("caseId"));
        assertEquals(CASE_REF, sjpOpenCase.getString("caseRef"));
        assertEquals(LocalDate.of(2015, 12, 2).toString(), sjpOpenCase.getString("postingOrHearingDate"));

        final JsonArray offenceTitles = sjpOpenCase.getJsonArray("offenceTitles");
        assertEquals(1, offenceTitles.size());
        assertEquals("Committed some offence", offenceTitles.getString(0));
    }

    private CreateCase.CreateCasePayloadBuilder createCase(UUID caseId,
                                                           UUID defendantId,
                                                           ProsecutingAuthority prosecutingAuthority,
                                                           LocalDate dateOfBirth) {
        return defaultCaseBuilder()
                .withDefendantDateOfBirth(dateOfBirth)
                .withId(caseId)
                .withProsecutingAuthority(prosecutingAuthority)
                .withDefendantId(defendantId);
    }

    private JsonObject responseToJsonObject(String response) {
        return Json.createReader(new StringReader(response)).readObject();
    }
}
