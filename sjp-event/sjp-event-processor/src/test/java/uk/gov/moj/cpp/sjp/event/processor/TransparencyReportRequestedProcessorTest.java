package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.joda.time.LocalDate.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.utils.PdfHelper;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TransparencyReportRequestedProcessorTest {

    @InjectMocks
    private TransparencyReportRequestedProcessor processor;

    @Mock
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Mock
    private DocumentGeneratorClient documentGeneratorClient;

    @Mock
    private PdfHelper pdfHelper;

    @Mock
    private FileStorer fileStorer;

    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private ReferenceDataOffencesService referenceDataOffencesService;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Captor
    private ArgumentCaptor<JsonObject> payloadForEnglishDocumentGenerationCaptor;

    @Captor
    private ArgumentCaptor<JsonObject> payloadForWelshDocumentGenerationCaptor;

    @Captor
    private ArgumentCaptor<JsonObject> payloadForFileStoreCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> storeTransparencyReportCommandEnvelopeCaptor;


    @Test
    public void shouldCreateTransparencyReport() throws IOException, FileServiceException {
        final String expectedEnglishTemplateName = "PendingCasesEnglish";
        final String expectedWelshTemplateName = "PendingCasesWelsh";
        final UUID systemUserId = randomUUID();

        final UUID englishPdfFileUUID = randomUUID();
        final UUID welshPdfFileUUID = randomUUID();

        final String offenceTitle = "OffenceTitle";
        final String prosecutorName = "TFL";
        final String prosecutorEnglish = "Transport For London";
        final String prosecutorWelsh = "Transport For London - Welsh";
        final byte[] englishPdfInBytes = new byte[]{1, 2, 3};
        final byte[] welshPdfInBytes = new byte[]{1, 2, 4};
        final int englishPdfPageCount = 2;
        final int welshPdfPageCount = 3;
        final Integer numberOfPendingCasesForExport = 9;
        final List<UUID> caseIds = range(0, numberOfPendingCasesForExport)
                .mapToObj(e -> randomUUID()).collect(toList());

        final JsonObject CA03011_referenceDataPayload = createObjectBuilder()
                .add("title", offenceTitle)
                .build();

        when(referenceDataService.getProsecutor(eq(prosecutorName), eq(false), any())).thenReturn(prosecutorEnglish);
        when(referenceDataService.getProsecutor(eq(prosecutorName), eq(true), any())).thenReturn(prosecutorWelsh);
        when(referenceDataOffencesService.getOffenceReferenceData(any(), anyString(), anyString())).thenReturn(CA03011_referenceDataPayload);

        final JsonObject pendingCasesResultPayload = createQueryPendingCasesPayload(caseIds);
        when(requester.request(any())).thenReturn(createEnvelope("sjp.query.pending-cases", pendingCasesResultPayload));
        when(fileStorer.store(any(), any())).thenReturn(englishPdfFileUUID).thenReturn(welshPdfFileUUID);
        when(pdfHelper.getDocumentPageCount(englishPdfInBytes)).thenReturn(englishPdfPageCount);
        when(pdfHelper.getDocumentPageCount(welshPdfInBytes)).thenReturn(welshPdfPageCount);
        when(documentGeneratorClient.generatePdfDocument(any(), eq(expectedEnglishTemplateName), eq(systemUserId))).thenReturn(englishPdfInBytes);
        when(documentGeneratorClient.generatePdfDocument(any(), eq(expectedWelshTemplateName), eq(systemUserId))).thenReturn(welshPdfInBytes);
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));

        final JsonEnvelope privateEventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.transparency-report-requested"),
                createObjectBuilder().build()
        );
        processor.createTransparencyReport(privateEventEnvelope);

        verify(documentGeneratorClient).generatePdfDocument(payloadForEnglishDocumentGenerationCaptor.capture(), eq(expectedEnglishTemplateName), eq(systemUserId));
        verify(documentGeneratorClient).generatePdfDocument(payloadForWelshDocumentGenerationCaptor.capture(), eq(expectedWelshTemplateName), eq(systemUserId));

        final JsonObject payloadForEnglishPdf = payloadForEnglishDocumentGenerationCaptor.getValue();
        final JsonObject payloadForWelshPdf = payloadForWelshDocumentGenerationCaptor.getValue();

        assertPayloadForDocumentGenerator(payloadForEnglishPdf, pendingCasesResultPayload, numberOfPendingCasesForExport, false);
        assertPayloadForDocumentGenerator(payloadForWelshPdf, pendingCasesResultPayload, numberOfPendingCasesForExport, true);
        verify(referenceDataService).getProsecutor(eq(prosecutorName), eq(false), any());
        verify(referenceDataService).getProsecutor(eq(prosecutorName), eq(true), any());

        verify(pdfHelper).getDocumentPageCount(englishPdfInBytes);
        verify(pdfHelper).getDocumentPageCount(welshPdfInBytes);
        verify(fileStorer, times(2)).store(payloadForFileStoreCaptor.capture(), any(ByteArrayInputStream.class));
        assertThat(payloadForFileStoreCaptor.getAllValues().size(), is(2));

        final JsonObject expectedEnglishPdfMetadata = buildFileMetadataJsonObject("transparency-report-english.pdf", englishPdfPageCount, englishPdfInBytes.length, englishPdfFileUUID);
        final JsonObject expectedWelshPdfMetadata = buildFileMetadataJsonObject("transparency-report-welsh.pdf", welshPdfPageCount, welshPdfInBytes.length, welshPdfFileUUID);

        assertPayloadForFileStore(payloadForFileStoreCaptor.getAllValues().get(0), expectedEnglishPdfMetadata);
        assertPayloadForFileStore(payloadForFileStoreCaptor.getAllValues().get(1), expectedWelshPdfMetadata);

        verify(sender).send(storeTransparencyReportCommandEnvelopeCaptor.capture());
        assertTransparencyReportEnvelope(storeTransparencyReportCommandEnvelopeCaptor.getValue(), expectedEnglishPdfMetadata, expectedWelshPdfMetadata, caseIds);
    }

    private JsonObject buildFileMetadataJsonObject(final String fileName,
                                                   final int pdfPageCount,
                                                   final int fileSize,
                                                   final UUID englishPdfFileUUID) {
        return createObjectBuilder()
                .add("fileName", fileName)
                .add("numberOfPages", pdfPageCount)
                .add("fileSize", fileSize)
                .add("fileId", englishPdfFileUUID.toString()).build();
    }

    private void assertPayloadForDocumentGenerator(final JsonObject payload, final JsonObject pendingCasesResultPayload, final Integer totalNumberOfRecords, final Boolean isWelsh) {
        assertReadyCasesPayloadWithPendingCases(payload, pendingCasesResultPayload, "defendantName");
        assertReadyCasesPayloadWithPendingCases(payload, pendingCasesResultPayload, "town");
        assertReadyCasesPayloadWithPendingCases(payload, pendingCasesResultPayload, "county");
        assertReadyCasesPayloadWithPendingCases(payload, pendingCasesResultPayload, "postcode");

        assertThat(getCaseIdsFromPayload(payload, "readyCases", "prosecutorName"),
                is(range(0, totalNumberOfRecords).mapToObj(e -> isWelsh ? "Transport For London - Welsh" : "Transport For London").collect(toList())));

        assertThat(payload.getJsonArray("readyCases").size(), is(totalNumberOfRecords));
        assertThat(payload.getInt("totalNumberOfRecords"), is(totalNumberOfRecords));
        assertThat(payload.getString("generatedDateAndTime"), is(notNullValue()));
    }

    private void assertReadyCasesPayloadWithPendingCases(final JsonObject payload, final JsonObject pendingCasesResultPayload, final String field) {
        assertThat(getCaseIdsFromPayload(payload, "readyCases", field),
                is(getCaseIdsFromPayload(pendingCasesResultPayload, "pendingCases", field)));
    }

    private List<String> getCaseIdsFromPayload(final JsonObject payload, final String containerArray, final String jsonField) {
        return payload.getJsonArray(containerArray)
                .getValuesAs(JsonObject.class).stream()
                .map(e -> e.getString(jsonField))
                .collect(Collectors.toList());
    }

    private void assertTransparencyReportEnvelope(final JsonEnvelope storeTransparencyReportCommandEnvelope,
                                                  final JsonObject englishReportMetadata, final JsonObject welshReportMetadata, final List<UUID> caseIds) {
        assertThat(storeTransparencyReportCommandEnvelope.metadata().name(), is("sjp.command.store-transparency-report-data"));
        assertThat(storeTransparencyReportCommandEnvelope.payloadAsJsonObject().getJsonObject("englishReportMetadata"), is(englishReportMetadata));
        assertThat(storeTransparencyReportCommandEnvelope.payloadAsJsonObject().getJsonObject("welshReportMetadata"), is(welshReportMetadata));

        final JsonArray caseIdsJsonArray = storeTransparencyReportCommandEnvelope.payloadAsJsonObject().getJsonArray("caseIds");
        assertThat(caseIdsJsonArray.size(), is(caseIds.size()));
        assertEquals(range(0, caseIds.size()).mapToObj(idx -> fromString(caseIdsJsonArray.getString(idx))).collect(toList()), caseIds);
    }

    private void assertPayloadForFileStore(final JsonObject payloadForFileStore, final JsonObject expectedPayloadForFileStore) {
        assertThat(payloadForFileStore.getString("fileName"), is(expectedPayloadForFileStore.getString("fileName")));
        assertThat(payloadForFileStore.getInt("numberOfPages"), is(expectedPayloadForFileStore.getInt("numberOfPages")));
        assertThat(payloadForFileStore.getInt("fileSize"), is(expectedPayloadForFileStore.getInt("fileSize")));
    }

    private JsonObject createQueryPendingCasesPayload(final List<UUID> caseIds) {
        final JsonArrayBuilder pendingCaseArrayBuilder = createArrayBuilder();
        range(0, caseIds.size()).forEach(e -> {
            final JsonArrayBuilder offenceArrayBuilder = createArrayBuilder();
            offenceArrayBuilder.add(createObjectBuilder()
                    .add("offenceCode", "CA03011")
                    .add("offenceStartDate", now().toString()));
            offenceArrayBuilder.add(createObjectBuilder()
                    .add("offenceCode", "CA03011")
                    .add("offenceStartDate", now().minusMonths(1).toString()));

            pendingCaseArrayBuilder.add(createObjectBuilder()
                    .add("caseId", caseIds.get(e).toString())
                    .add("defendantName", "J. Doe".concat(String.valueOf(e)))
                    .add("town", "London".concat(String.valueOf(e)))
                    .add("county", "Greater London".concat(String.valueOf(e)))
                    .add("postcode", "SE1 1PJ".concat(String.valueOf(e)))
                    .add("offences", offenceArrayBuilder)
                    .add("prosecutorName", "TFL"));
        });

        return createObjectBuilder()
                .add("pendingCases", pendingCaseArrayBuilder)
                .build();
    }

}