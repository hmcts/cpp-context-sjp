package uk.gov.moj.cpp.sjp.query.api;

import static java.time.ZonedDateTime.now;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseAssignmentRestrictionApiTest {

    @Mock
    private Requester requester;

    @InjectMocks
    private CaseAssignmentRestrictionApi caseAssignmentRestrictionApi;

    @Test
    public void shouldHandleQuery() {
        assertThat(CaseAssignmentRestrictionApi.class, isHandlerClass(Component.QUERY_API)
                .with(method("getCaseAssignmentRestriction").thatHandles("sjp.query.case-assignment-restriction").withRequesterPassThrough()));
    }

    @Test
    public void shouldReturnCaseAssignmentRestriction() {
        final JsonEnvelope requestEnvelope = createEnvelope("sjp.query.case-assignment-restriction",
                createObjectBuilder()
                        .add("prosecutingAuthority", "TVL")
                        .build());
        final String id = UUID.randomUUID().toString();
        final String dateTimeCreated = now().toString();
        JsonEnvelope mockResponse = envelope()
                .with(metadataWithRandomUUID("sjp.query.case-assignment-restriction"))
                .withPayloadOf("TVL", "prosecutingAuthority")
                .withPayloadOf(id, "id")
                .withPayloadOf(dateTimeCreated, "dateTimeCreated")
                .withPayloadOf(new String[] {"1234"}, "exclude")
                .withPayloadOf(new String[] {"9876"}, "includeOnly")
                .build();

        when(requester.request(requestEnvelope)).thenReturn(mockResponse);
        final JsonObject response = caseAssignmentRestrictionApi.getCaseAssignmentRestriction(requestEnvelope).payloadAsJsonObject();
        assertThat(response.getString("prosecutingAuthority"), equalTo("TVL"));
        assertThat(response.getString("id"), equalTo(id));
        assertThat(response.getString("dateTimeCreated"), equalTo(dateTimeCreated));
        assertThat(response.getJsonArray("exclude"), equalTo(createArrayBuilder().add("1234").build()));
        assertThat(response.getJsonArray("includeOnly"), equalTo(createArrayBuilder().add("9876").build()));
    }
}
