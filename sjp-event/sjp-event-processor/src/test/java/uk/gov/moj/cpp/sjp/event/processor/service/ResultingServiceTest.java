package uk.gov.moj.cpp.sjp.event.processor.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResultingServiceTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Requester requester;

    @InjectMocks
    private ResultingService resultingService;

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

    @Test
    public void shouldReturnCaseDecisions() {
        final JsonObject responsePayload = createObjectBuilder().add("decisions", createArrayBuilder()).build();
        final JsonEnvelope queryResponse = envelopeFrom(metadataWithRandomUUIDAndName(), responsePayload);

        final UUID caseId = randomUUID();
        when(requestCaseDecisions(caseId)).thenReturn(queryResponse);

        final JsonObject caseDecisions = resultingService.getCaseDecisions(caseId.toString(), envelope);
        assertThat(caseDecisions, is(responsePayload));
    }

    private Object requestCaseDecisions(final UUID caseId) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("resulting.query.case-decisions"),
                payloadIsJson(withJsonPath("$.caseId", equalTo(caseId.toString()))))));
    }

}
