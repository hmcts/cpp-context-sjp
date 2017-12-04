package uk.gov.moj.cpp.sjp.query.controller;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.sjp.query.controller.service.UserAndGroupsService;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FindCaseControllerTest {
    @InjectMocks
    private SjpQueryController controller;

    @Mock
    private Requester requester;

    @Mock
    private UserAndGroupsService userAndGroupsService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor;

    @Test
    public void findCase() {
        String userId = UUID.randomUUID().toString();
        String caseId = UUID.randomUUID().toString();
        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUID("sjp.query.case").withUserId(userId),
                createObjectBuilder()
                        .add("caseId", caseId)
                        .build());

        // This is big case object, not relevant for the testing of this controller
        final JsonEnvelope response = envelope().build();

        when(userAndGroupsService.isSjpProsecutorUserGroupOnly(query)).thenReturn(Boolean.FALSE);
        when(requester.request(jsonEnvelopeCaptor.capture())).thenReturn(response);

        final JsonEnvelope aCase = controller.findCase(query);
        final JsonEnvelope requestEnvelope = jsonEnvelopeCaptor.getValue();

        assertThat(requestEnvelope, is(query));
        assertThat(aCase, is(response));
    }

    @Test
    public void findCaseWithFilteringForSjpProsecutor() {
        String userId = UUID.randomUUID().toString();
        String caseId = UUID.randomUUID().toString();
        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUID("sjp.query.case").withUserId(userId),
                createObjectBuilder()
                        .add("caseId", caseId)
                        .build());

        // This is big case object, not relevant for the testing of this controller
        final JsonEnvelope response = envelope().build();

        when(userAndGroupsService.isSjpProsecutorUserGroupOnly(query)).thenReturn(Boolean.TRUE);
        when(requester.request(jsonEnvelopeCaptor.capture())).thenReturn(response);

        final JsonEnvelope aCase = controller.findCase(query);
        final JsonEnvelope requestEnvelope = jsonEnvelopeCaptor.getValue();

        assertThat(requestEnvelope, jsonEnvelope(
                withMetadataEnvelopedFrom(query).withName("sjp.query.case-filter-other-and-financial-means-documents"),
                JsonEnvelopePayloadMatcher.payloadIsJson(withJsonPath("$.caseId", is(caseId))
                )));
        assertThat(aCase, is(response));
    }

    @Test
    public void findCaseDocuments() {
        String userId = UUID.randomUUID().toString();
        String caseId = UUID.randomUUID().toString();
        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUID("sjp.query.case-documents").withUserId(userId),
                createObjectBuilder()
                        .add("caseId", caseId)
                        .build());

        // This is big case object, not relevant for the testing of this controller
        final JsonEnvelope response = envelope().build();

        when(userAndGroupsService.isSjpProsecutorUserGroupOnly(query)).thenReturn(Boolean.FALSE);
        when(requester.request(jsonEnvelopeCaptor.capture())).thenReturn(response);

        final JsonEnvelope aCase = controller.findCaseDocuments(query);
        final JsonEnvelope requestEnvelope = jsonEnvelopeCaptor.getValue();

        assertThat(requestEnvelope, is(query));
        assertThat(aCase, is(response));
    }

    @Test
    public void findCaseDocumentsWithFilteringForSjpProsecutor() {
        String userId = UUID.randomUUID().toString();
        String caseId = UUID.randomUUID().toString();
        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUID("sjp.query.case-documents").withUserId(userId),
                createObjectBuilder()
                        .add("caseId", caseId)
                        .build());

        // This is big case object, not relevant for the testing of this controller
        final JsonEnvelope response = envelope().build();

        when(userAndGroupsService.isSjpProsecutorUserGroupOnly(query)).thenReturn(Boolean.TRUE);
        when(requester.request(jsonEnvelopeCaptor.capture())).thenReturn(response);

        final JsonEnvelope aCase = controller.findCaseDocuments(query);
        final JsonEnvelope requestEnvelope = jsonEnvelopeCaptor.getValue();

        assertThat(requestEnvelope, jsonEnvelope(
                withMetadataEnvelopedFrom(query).withName("sjp.query.case-documents-filter-other-and-financial-means"),
                JsonEnvelopePayloadMatcher.payloadIsJson(withJsonPath("$.caseId", is(caseId))
                )));
        assertThat(aCase, is(response));
    }
}
