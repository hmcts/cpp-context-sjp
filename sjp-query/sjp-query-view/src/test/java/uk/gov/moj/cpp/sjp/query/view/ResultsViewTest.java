package uk.gov.moj.cpp.sjp.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.query.view.service.ResultsService;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResultsViewTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID SESSION_ID = randomUUID();
    private static final UUID RESULT_DEFINITION_ID = randomUUID();
    private static final ZonedDateTime DECISION_SAVED_AT = ZonedDateTime.now();
    private static final UUID OFFENCE1_ID = randomUUID();


    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private ResultsService resultsService;

    @InjectMocks
    private ResultsView resultsView;

    @Test
    public void shouldGetCaseResults() {

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.query.case-results"),
                createObjectBuilder().add("caseId", CASE_ID.toString()).build());

        final JsonObject payload = createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("accountDivisionCode", 77)
                .add("enforcingCourtCode", 828)
                .add("caseDecisions", createArrayBuilder().add(createObjectBuilder()
                        .add("sjpSessionId", SESSION_ID.toString())
                        .add("resultedOn", DECISION_SAVED_AT.toString())
                        .add("offences", createArrayBuilder().add(createObjectBuilder()
                                .add("id", OFFENCE1_ID.toString())
                                .add("verdict", "FOUND_NOT_GUILTY")
                                .add("results", createArrayBuilder().add(createObjectBuilder()
                                        .add("resultDefinitionId", RESULT_DEFINITION_ID.toString())
                                        .add("prompts", createArrayBuilder())))))))
                .build();

        when(resultsService.findCaseResults(envelope)).thenReturn(payload);

        final JsonEnvelope caseResults = resultsView.getCaseResults(envelope);

        assertThat(caseResults, jsonEnvelope(metadata().withName("sjp.query.case-results"),
                payload().isJson(allOf(
                        withJsonPath("$.caseId", is(CASE_ID.toString())),
                        withJsonPath("$.accountDivisionCode", is(77)),
                        withJsonPath("$.enforcingCourtCode", is(828)
                ))
        )));
    }
}
