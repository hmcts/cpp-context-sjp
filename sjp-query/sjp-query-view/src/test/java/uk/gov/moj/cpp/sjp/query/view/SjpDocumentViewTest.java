package uk.gov.moj.cpp.sjp.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueNullMatcher.isJsonValueNull;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentView;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;

import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpDocumentViewTest {

    private static final String CASE_DOCUMENT_QUERY_NAME = "sjp.query.case-document";

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private CaseService caseService;

    @InjectMocks
    private SjpDocumentView sjpDocumentView;

    private final UUID caseId = randomUUID();
    private final UUID documentId = randomUUID();

    final JsonEnvelope queryEnvelope = envelope()
            .with(metadataWithRandomUUID(CASE_DOCUMENT_QUERY_NAME))
            .withPayloadOf(caseId.toString(), "caseId")
            .withPayloadOf(documentId.toString(), "documentId")
            .build();

    @Test
    public void shouldGetCaseDocument() {
        final CaseDocumentView caseDocumentView = new CaseDocumentView(randomUUID(), randomUUID(), "TEST", 1);

        when(caseService.findCaseDocument(caseId, documentId)).thenReturn(Optional.of(caseDocumentView));

        assertThat(sjpDocumentView.findCaseDocument(queryEnvelope), jsonEnvelope(
                metadata().withName(CASE_DOCUMENT_QUERY_NAME),
                payload().isJson(allOf(
                        withJsonPath("$.caseDocument.id", is(caseDocumentView.getId().toString())),
                        withJsonPath("$.caseDocument.materialId", is(caseDocumentView.getMaterialId().toString())),
                        withJsonPath("$.caseDocument.documentType", is(caseDocumentView.getDocumentType())),
                        withJsonPath("$.caseDocument.documentNumber", is(caseDocumentView.getDocumentNumber()))
                )))
        );
    }

    @Test
    public void shouldReturnNullJsonWhenCaseDocumentNotFound() {
        when(caseService.findCaseDocument(caseId, documentId)).thenReturn(Optional.empty());

        assertThat(sjpDocumentView.findCaseDocument(queryEnvelope), jsonEnvelope(
                metadata().withName(CASE_DOCUMENT_QUERY_NAME),
                payload().isJsonValue(isJsonValueNull()))
        );
    }
}
