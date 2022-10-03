package uk.gov.moj.cpp.sjp.query.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;
import uk.gov.moj.cpp.sjp.query.api.decorator.DecisionDecorator;
import uk.gov.moj.cpp.sjp.query.api.decorator.DocumentMetadataDecorator;
import uk.gov.moj.cpp.sjp.query.api.decorator.OffenceDecorator;
import uk.gov.moj.cpp.sjp.query.service.ReferenceDataService;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpQueryCaseWithDocumentMetadataApiTest {

    private final UUID caseId = UUID.randomUUID();
    private final UUID documentId1 = UUID.randomUUID();
    private final UUID documentId2 = UUID.randomUUID();
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Requester requester;

    @Mock
    private DocumentMetadataDecorator documentMetadataDecorator;

    @Mock
    private OffenceDecorator offenceDecorator;

    @Mock
    private DecisionDecorator decisionDecorator;

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private SjpQueryApi sjpQueryApi;

    public static JsonEnvelopeMatcher queryCaseEnvelope(final JsonEnvelope sourceEnvelope, final UUID caseId) {
        return jsonEnvelope(withMetadataEnvelopedFrom(sourceEnvelope).withName("sjp.query.case"),
                payloadIsJson(withJsonPath("$.caseId", equalTo(caseId.toString()))));
    }

    @Test
    public void shouldReturnCasePayloadWithDocumentMetadata() {
        final JsonEnvelope requestEnvelope = createEnvelope("sjp.query.case-with-document-metadata",
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .build());

        final JsonEnvelope caseEnvelope = createEnvelope("sjp.query.case",
                createObjectBuilder()
                        .add("id", caseId.toString())
                        .add("caseDocuments", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("documentId", documentId1.toString())
                                ).add(createObjectBuilder()
                                        .add("documentId", documentId2.toString()))
                        )
                        .build());

        when(requester.request(argThat(queryCaseEnvelope(requestEnvelope, caseId)))).thenReturn(caseEnvelope);

        final JsonObject caseJsonObjectWithDecoratedPayload = getCaseJsonObjectWithDecoratedPayload();
        when(decisionDecorator.decorate(any(), any(), any())).thenReturn(caseEnvelope.payloadAsJsonObject());
        when(documentMetadataDecorator.decorateDocumentsForACase(caseEnvelope.payloadAsJsonObject(), requestEnvelope))
                .thenReturn(caseJsonObjectWithDecoratedPayload);

        final JsonEnvelope caseWithDocumentMetadata = sjpQueryApi.findCaseWithDocumentMetadata(requestEnvelope);
        assertThat(caseWithDocumentMetadata.payloadAsJsonObject(), equalTo(caseJsonObjectWithDecoratedPayload));
    }

    private JsonObject getCaseJsonObjectWithDecoratedPayload() {
        return createObjectBuilder()
                .add("id", caseId.toString())
                .add("caseDocuments", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("documentId", documentId1.toString())
                                .add("metadata", createObjectBuilder())
                        ).add(createObjectBuilder()
                                .add("documentId", documentId2.toString())
                                .add("metadata", createObjectBuilder())
                        )
                )
                .build();
    }

}
