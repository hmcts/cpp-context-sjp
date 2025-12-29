package uk.gov.moj.sjp.it.test;

import static java.time.LocalDate.*;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.defaultCaseBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollForCase;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilPotentialCasesByDefendantIdIsOk;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.UnifiedSearchStub.stubUnifiedSearchQueryForCases;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.jayway.jsonpath.matchers.JsonPathMatchers;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration test to find if defendant has potential cases using unified search query
 */
public class DefendantPotentialCaseIT extends BaseIntegrationTest {

    private static final String CASE_REF = "Potential case reference";

    private UUID randomCaseId;

    @BeforeEach
    public void before() {
        randomCaseId = randomUUID();
        stubUnifiedSearchQueryForCases(randomCaseId, CASE_REF);
    }


    @Test
    public void shouldReturnFalseForPotentialCases() {
        UUID caseId = randomUUID();
        UUID defendantId = randomUUID();
        final ProsecutingAuthority prosecutingAuthority = TFL;
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        final CreateCase.CreateCasePayloadBuilder createCase = createCase(caseId,
                defendantId, prosecutingAuthority, of(1980, 11, 10));

        createCaseForPayloadBuilder(createCase);
        pollForCase(caseId, new Matcher[]{JsonPathMatchers.withJsonPath("$.hasPotentialCase", is(false))});
    }

    @Test
    public void shouldFindPotentialCases() {
        UUID caseId = randomCaseId;
        UUID defendantId = randomUUID();

        final ProsecutingAuthority prosecutingAuthority = TFL;
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        final CreateCase.CreateCasePayloadBuilder createCase = createCase(caseId,
                defendantId,
                prosecutingAuthority,
                of(1980, 10, 15));
        final Optional<JsonEnvelope> caseReceivedEvent = new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(createCase))
                .popEvent(CaseReceived.EVENT_NAME);
        assertTrue(caseReceivedEvent.isPresent());

        final String potentialCasesResponsePayload = pollUntilPotentialCasesByDefendantIdIsOk(defendantId);
        final JsonObject potentialCases = responseToJsonObject(potentialCasesResponsePayload);
        final JsonArray sjpOpenCases = potentialCases.getJsonArray("sjpOpenCases");
        assertEquals(1, sjpOpenCases.size());

        final JsonObject sjpOpenCase = sjpOpenCases.getJsonObject(0);
        assertEquals(caseId.toString(), sjpOpenCase.getString("caseId"));
        assertEquals(CASE_REF, sjpOpenCase.getString("caseRef"));
        assertEquals(of(2015, 12, 2).toString(), sjpOpenCase.getString("postingOrHearingDate"));

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
        return JsonObjects.createReader(new StringReader(response)).readObject();
    }
}
