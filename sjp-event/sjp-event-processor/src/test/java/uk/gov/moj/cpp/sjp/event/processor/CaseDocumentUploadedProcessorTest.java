package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.file.api.sender.FileData;
import uk.gov.justice.services.file.api.sender.FileSender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.CaseDocumentUploadedProcessor;
import uk.gov.moj.cpp.sjp.event.processor.listener.EventProcessorConstants;

import java.io.InputStream;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseDocumentUploadedProcessorTest {

    private final String PDF_MIME_TYPE = "application/pdf";
    private final String ORIGINAL_FILE_NAME = "filename.pdf";
    private final String DOCUMENT_TYPE = "PLEA";

    @InjectMocks
    private CaseDocumentUploadedProcessor processor = new CaseDocumentUploadedProcessor();

    @Mock
    private Sender sender;

    @Mock
    private FileService fileService;

    @Mock
    private FileSender fileSender;

    @Spy
    private Enveloper enveloper = createEnveloper();

    @Test
    public void publishes() throws Exception {
        //given
        String caseId = UUID.randomUUID().toString();
        UUID documentReference = UUID.randomUUID();

        InputStream mockedInputStream = givenFileServiceReturnsValidFileReference(documentReference);

        givenFileSenderAcceptsTheFile(mockedInputStream);

        //when
        final JsonEnvelope envelope = prepareCaseDocumentUploadedEnvelope(caseId, documentReference, DOCUMENT_TYPE);

        processor.handleCaseDocumentUploaded(envelope);

        //then
        ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        final JsonEnvelope capturedEnvelope = envelopeArgumentCaptor.getValue();

        assertThat(capturedEnvelope,
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope)
                                .withName("lifecycle.command.add-case-material"),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId)),
                                withJsonPath("$.material.id", equalTo(documentReference.toString())),
                                withJsonPath("$.material.mimeType", equalTo(PDF_MIME_TYPE)),
                                withJsonPath("$.material.originalFileName", equalTo(ORIGINAL_FILE_NAME)),
                                withJsonPath("$.material.documentType", equalTo(DOCUMENT_TYPE))
                                )
                        )
                ));

        verify(sender).send(envelopeArgumentCaptor.capture());
        final JsonEnvelope capturedPublicEventEnvelope = envelopeArgumentCaptor.getValue();

        assertThat(capturedPublicEventEnvelope,
                jsonEnvelope(
                        withMetadataEnvelopedFrom(envelope)
                                .withName("public.structure.case-document-uploaded"),
                        payload().isJson(
                                withJsonPath("$.documentId", equalTo(documentReference.toString()))
                        )
                ));

    }

    private void givenFileSenderAcceptsTheFile(InputStream mockedInputStream) {
        String fileId = UUID.randomUUID().toString();

        when(fileSender.send(ORIGINAL_FILE_NAME, mockedInputStream)).thenReturn(
          new FileData(fileId, PDF_MIME_TYPE)
        );
    }

    private InputStream givenFileServiceReturnsValidFileReference(UUID documentReference) throws FileServiceException {
        JsonObject metadata = Json.createObjectBuilder()
                .add(EventProcessorConstants.FILENAME, ORIGINAL_FILE_NAME)
                .build();

        InputStream mockedInputStream = mock(InputStream.class);
        when(fileService.retrieve(documentReference)).thenReturn(of(
                new FileReference(documentReference, metadata, mockedInputStream)
        ));
        return mockedInputStream;
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void throwsExceptionWhenUnableToFindFile() throws Exception {

        //given
        String caseId = UUID.randomUUID().toString();
        UUID documentReference = UUID.randomUUID();
        String documentType = DOCUMENT_TYPE;

        when(fileService.retrieve(documentReference)).thenReturn(empty());

        //expect
        exception.expect(IllegalStateException.class);
        exception.expectMessage("No file with id: " + documentReference.toString());

        //when
        final JsonEnvelope envelope = prepareCaseDocumentUploadedEnvelope(caseId, documentReference, documentType);

        processor.handleCaseDocumentUploaded(envelope);

        //then
        verify(sender, never()).sendAsAdmin(any());
    }

    @Test
    public void throwsExceptionWhenThereAreIssuesWithFileService() throws Exception {
        //given
        String caseId = UUID.randomUUID().toString();
        UUID documentReference = UUID.randomUUID();
        String documentType = DOCUMENT_TYPE;

        when(fileService.retrieve(documentReference)).thenThrow(new FileServiceException("exception in test"));

        //expect
        exception.expect(IllegalStateException.class);
        exception.expectMessage("uk.gov.justice.services.fileservice.api.FileServiceException: exception in test");

        //when
        final JsonEnvelope envelope = prepareCaseDocumentUploadedEnvelope(caseId, documentReference, documentType);

        processor.handleCaseDocumentUploaded(envelope);

        //then
        verify(sender, never()).sendAsAdmin(any());
    }

    private JsonEnvelope prepareCaseDocumentUploadedEnvelope(String caseId, UUID documentReference, String documentType) {
        JsonObject payload = Json.createObjectBuilder()
                .add("caseId", caseId)
                .add("documentReference", documentReference.toString())
                .add("documentType", documentType).build();

        return createEnvelope("structure.events.case-document-uploaded",
                payload);
    }
}
