package uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.utils.PdfHelper;

import javax.json.JsonObject;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.DocumentAvailableEventEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.GenerationFailedEventEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.SystemDocGeneratorEnvelopes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;

@ExtendWith(MockitoExtension.class)
public class PressAndTransparencyReportStrategyTest {

    private static final byte[] PDF_BYTES = "test-pdf-content".getBytes(UTF_8);
    private static final int PAGE_COUNT = 3;

    @TempDir
    Path tempDir;

    @Mock
    private Sender sender;

    @Mock
    private PdfHelper pdfHelper;

    @InjectMocks
    private PressAndTransparencyReportStrategy strategy;

    // ── canProcess ──────────────────────────────────────────────────────────────

    @Test
    public void shouldBeAbleToProcessSystemDocDocumentAvailablePublicEvent() {
        final DocumentAvailableEventEnvelopeBuilder envelopeBuilder = SystemDocGeneratorEnvelopes.documentAvailablePublicEvent();

        assertThat(strategy.canProcess(envelopeBuilder.templateIdentifier("PublicPendingCasesFullEnglish").envelope()), is(true));
        assertThat(strategy.canProcess(envelopeBuilder.templateIdentifier("PublicPendingCasesFullWelsh").envelope()), is(true));
        assertThat(strategy.canProcess(envelopeBuilder.templateIdentifier("PressPendingCasesFullEnglish").envelope()), is(true));
    }

    @Test
    public void shouldBeAbleToProcessSystemDocGenerationFailedPublicEvent() {
        final GenerationFailedEventEnvelopeBuilder envelopeBuilder = SystemDocGeneratorEnvelopes.generationFailedEvent();

        assertThat(strategy.canProcess(envelopeBuilder.templateIdentifier("PublicPendingCasesFullEnglish").envelope()), is(true));
        assertThat(strategy.canProcess(envelopeBuilder.templateIdentifier("PublicPendingCasesFullWelsh").envelope()), is(true));
        assertThat(strategy.canProcess(envelopeBuilder.templateIdentifier("PressPendingCasesFullEnglish").envelope()), is(true));
    }

    @Test
    public void shouldNotProcessUnknownTemplateIdentifier() {
        final JsonEnvelope generationFailedEnvelope = SystemDocGeneratorEnvelopes.generationFailedEvent()
                .templateIdentifier("random").envelope();
        final JsonEnvelope documentAvailableEnvelope = SystemDocGeneratorEnvelopes.documentAvailablePublicEvent()
                .templateIdentifier("random").envelope();

        assertThat(strategy.canProcess(generationFailedEnvelope), is(false));
        assertThat(strategy.canProcess(documentAvailableEnvelope), is(false));
    }

    // ── processDocumentAvailable — no sourceUri ─────────────────────────────────

    @Test
    public void shouldNotSendAnyCommandWhenDocumentAvailableHasNoSourceUri() {
        final JsonEnvelope envelope = buildDocumentAvailableEnvelopeWithoutSourceUri(
                "PublicPendingCasesFullEnglish", randomUUID());

        strategy.process(envelope);

        verifyNoInteractions(sender);
    }

    // ── processDocumentAvailable — download failure ──────────────────────────────

    @Test
    public void shouldSendOnlyIngestFileCommandWhenSourceUriDownloadFails() {
        final UUID blobFileId = randomUUID();
        final UUID reportId = randomUUID();
        final JsonEnvelope envelope = buildDocumentAvailableEnvelope(
                "PublicPendingCasesFullEnglish", reportId, blobFileId,
                "file:///non-existent-path-that-will-fail-xyz.pdf");

        strategy.process(envelope);

        final ArgumentCaptor<JsonEnvelope> sentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender, times(1)).send(sentCaptor.capture());
        assertThat(sentCaptor.getValue().metadata().name(), is("sjp.ingest-file"));
    }

    // ── processDocumentAvailable — pdfHelper failure ──────────────────────────────

    @Test
    public void shouldSendOnlyIngestFileCommandWhenPdfHelperThrowsIoException() throws Exception {
        final UUID blobFileId = randomUUID();
        final UUID reportId = randomUUID();
        final Path tempFile = tempDir.resolve("test.bin");
        Files.write(tempFile, PDF_BYTES);

        final ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        when(pdfHelper.getDocumentPageCount(bytesCaptor.capture())).thenThrow(new IOException("pdf parse failure"));

        strategy.process(buildDocumentAvailableEnvelope(
                "PublicPendingCasesFullEnglish", reportId, blobFileId, tempFile.toUri().toString()));

        verify(sender, times(1)).send(ArgumentCaptor.forClass(JsonEnvelope.class).capture());
        assertArrayEquals(PDF_BYTES, bytesCaptor.getValue());
    }

    // ── processDocumentAvailable — transparency templates ────────────────────────

    @Test
    public void shouldProcessDocumentAvailableForTransparencyFullEnglish() throws Exception {
        verifyTransparencyDocumentAvailable(
                "PublicPendingCasesFullEnglish", "transparency-report-full-english.pdf", "en");
    }

    @Test
    public void shouldProcessDocumentAvailableForTransparencyDeltaEnglish() throws Exception {
        verifyTransparencyDocumentAvailable(
                "PublicPendingCasesDeltaEnglish", "transparency-report-delta-english.pdf", "en");
    }

    @Test
    public void shouldProcessDocumentAvailableForTransparencyFullWelsh() throws Exception {
        verifyTransparencyDocumentAvailable(
                "PublicPendingCasesFullWelsh", "transparency-report-full-welsh.pdf", "cy");
    }

    @Test
    public void shouldProcessDocumentAvailableForTransparencyDeltaWelsh() throws Exception {
        verifyTransparencyDocumentAvailable(
                "PublicPendingCasesDeltaWelsh", "transparency-report-delta-welsh.pdf", "cy");
    }

    // ── processDocumentAvailable — press transparency templates ──────────────────

    @Test
    public void shouldProcessDocumentAvailableForPressTransparencyFullEnglish() throws Exception {
        verifyPressDocumentAvailable(
                "PressPendingCasesFullEnglish", "press-transparency-report-full-english.pdf");
    }

    @Test
    public void shouldProcessDocumentAvailableForPressTransparencyDeltaEnglish() throws Exception {
        verifyPressDocumentAvailable(
                "PressPendingCasesDeltaEnglish", "press-transparency-report-delta-english.pdf");
    }

    @Test
    public void shouldProcessDocumentAvailableForPressTransparencyFullWelsh() throws Exception {
        verifyPressDocumentAvailable(
                "PressPendingCasesFullWelsh", "press-transparency-report-full-welsh.pdf");
    }

    @Test
    public void shouldProcessDocumentAvailableForPressTransparencyDeltaWelsh() throws Exception {
        verifyPressDocumentAvailable(
                "PressPendingCasesDeltaWelsh", "press-transparency-report-delta-welsh.pdf");
    }

    // ── processDocumentAvailable — default (unrecognised) branch ────────────────

    @Test
    public void shouldLogInfoForUnrecognisedTemplateWhenDocumentAvailable() throws Exception {
        final UUID blobFileId = randomUUID();
        final UUID reportId = randomUUID();
        final Path tempFile = tempDir.resolve("test.bin");
        Files.write(tempFile, PDF_BYTES);

        final ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        when(pdfHelper.getDocumentPageCount(bytesCaptor.capture())).thenReturn(PAGE_COUNT);

        strategy.process(buildDocumentAvailableEnvelope(
                "UnrecognisedTemplate", reportId, blobFileId, tempFile.toUri().toString()));

        verify(sender, times(1)).send(ArgumentCaptor.forClass(JsonEnvelope.class).capture());
    }

    // ── processGenerationFailed ──────────────────────────────────────────────────

    @Test
    public void shouldSendTransparencyReportFailedCommandOnGenerationFailed() {
        final UUID reportId = randomUUID();
        final String templateIdentifier = "PublicPendingCasesFullEnglish";
        final JsonEnvelope envelope = buildGenerationFailedEnvelope(templateIdentifier, reportId);

        strategy.process(envelope);

        final ArgumentCaptor<JsonEnvelope> sentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender, times(1)).send(sentCaptor.capture());

        final JsonEnvelope failedCommand = sentCaptor.getValue();
        assertThat(failedCommand.metadata().name(), is("sjp.command.transparency-report-failed"));
        assertThat(failedCommand.payloadAsJsonObject().getString("transparencyReportId"), is(reportId.toString()));
        assertThat(failedCommand.payloadAsJsonObject().getString("templateIdentifier"), is(templateIdentifier));
    }

    @Test
    public void shouldSendPressTransparencyReportFailedCommandOnGenerationFailed() {
        final UUID reportId = randomUUID();
        final JsonEnvelope envelope = buildGenerationFailedEnvelope("PressPendingCasesFullEnglish", reportId);

        strategy.process(envelope);

        final ArgumentCaptor<JsonEnvelope> sentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender, times(1)).send(sentCaptor.capture());

        final JsonEnvelope failedCommand = sentCaptor.getValue();
        assertThat(failedCommand.metadata().name(), is("sjp.command.press-transparency-report-failed"));
        assertThat(failedCommand.payloadAsJsonObject().getString("pressTransparencyReportId"), is(reportId.toString()));
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private void verifyTransparencyDocumentAvailable(final String templateIdentifier,
                                                      final String expectedFilename,
                                                      final String expectedLanguage) throws Exception {
        final UUID blobFileId = randomUUID();
        final UUID reportId = randomUUID();
        final Path tempFile = tempDir.resolve("test.bin");
        Files.write(tempFile, PDF_BYTES);

        final ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        when(pdfHelper.getDocumentPageCount(bytesCaptor.capture())).thenReturn(PAGE_COUNT);

        strategy.process(buildDocumentAvailableEnvelope(
                templateIdentifier, reportId, blobFileId, tempFile.toUri().toString()));

        final ArgumentCaptor<JsonEnvelope> sentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender, times(2)).send(sentCaptor.capture());

        final List<JsonEnvelope> sent = sentCaptor.getAllValues();
        verifyIngestCommand(sent.get(0), blobFileId, reportId, expectedFilename, tempFile.toUri().toString());

        final javax.json.JsonObject updatePayload = sent.get(1).payloadAsJsonObject();
        assertThat(sent.get(1).metadata().name(), is("sjp.command.update-transparency-report-data"));
        assertThat(updatePayload.getString("transparencyReportId"), is(reportId.toString()));
        assertThat(updatePayload.getString("language"), is(expectedLanguage));
        verifyMetadata(updatePayload.getJsonObject("metadata"), blobFileId, expectedFilename);
        assertArrayEquals(PDF_BYTES, bytesCaptor.getValue());
    }

    private void verifyPressDocumentAvailable(final String templateIdentifier,
                                               final String expectedFilename) throws Exception {
        final UUID blobFileId = randomUUID();
        final UUID reportId = randomUUID();
        final Path tempFile = tempDir.resolve("test.bin");
        Files.write(tempFile, PDF_BYTES);

        final ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        when(pdfHelper.getDocumentPageCount(bytesCaptor.capture())).thenReturn(PAGE_COUNT);

        strategy.process(buildDocumentAvailableEnvelope(
                templateIdentifier, reportId, blobFileId, tempFile.toUri().toString()));

        final ArgumentCaptor<JsonEnvelope> sentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        verify(sender, times(2)).send(sentCaptor.capture());

        final List<JsonEnvelope> sent = sentCaptor.getAllValues();
        verifyIngestCommand(sent.get(0), blobFileId, reportId, expectedFilename, tempFile.toUri().toString());

        final javax.json.JsonObject updatePayload = sent.get(1).payloadAsJsonObject();
        assertThat(sent.get(1).metadata().name(), is("sjp.command.update-press-transparency-report-data"));
        assertThat(updatePayload.getString("pressTransparencyReportId"), is(reportId.toString()));
        verifyMetadata(updatePayload.getJsonObject("metadata"), blobFileId, expectedFilename);
        assertArrayEquals(PDF_BYTES, bytesCaptor.getValue());
    }

    private void verifyIngestCommand(final JsonEnvelope envelope,
                                      final UUID blobFileId,
                                      final UUID reportId,
                                      final String expectedFilename,
                                      final String expectedSourceUri) {
        assertThat(envelope.metadata().name(), is("sjp.ingest-file"));
        final javax.json.JsonObject payload = envelope.payloadAsJsonObject();
        assertThat(payload.getString("fileId"), is(blobFileId.toString()));
        assertThat(payload.getString("correlationId"), is(reportId.toString()));
        assertThat(payload.getString("filename"), is(expectedFilename));
        assertThat(payload.getString("sourceUri"), is(expectedSourceUri));
    }

    private void verifyMetadata(final javax.json.JsonObject metadata,
                                 final UUID blobFileId,
                                 final String expectedFilename) {
        assertThat(metadata.getString("fileId"), is(blobFileId.toString()));
        assertThat(metadata.getInt("numberOfPages"), is(PAGE_COUNT));
        assertThat(metadata.getInt("fileSize"), is(PDF_BYTES.length));
        assertThat(metadata.getString("fileName"), is(expectedFilename));
    }

    private JsonEnvelope buildDocumentAvailableEnvelope(final String templateIdentifier,
                                                         final UUID reportId,
                                                         final UUID blobFileId,
                                                         final String sourceUri) {
        return EnvelopeFactory.createEnvelope(
                "public.systemdocgenerator.events.document-available",
                createObjectBuilder()
                        .add("templateIdentifier", templateIdentifier)
                        .add("sourceCorrelationId", reportId.toString())
                        .add("blobFileId", blobFileId.toString())
                        .add("sourceUri", sourceUri)
                        .build());
    }

    private JsonEnvelope buildDocumentAvailableEnvelopeWithoutSourceUri(final String templateIdentifier,
                                                                          final UUID reportId) {
        return EnvelopeFactory.createEnvelope(
                "public.systemdocgenerator.events.document-available",
                createObjectBuilder()
                        .add("templateIdentifier", templateIdentifier)
                        .add("sourceCorrelationId", reportId.toString())
                        .build());
    }

    private JsonEnvelope buildGenerationFailedEnvelope(final String templateIdentifier, final UUID reportId) {
        return EnvelopeFactory.createEnvelope(
                "public.systemdocgenerator.events.generation-failed",
                createObjectBuilder()
                        .add("templateIdentifier", templateIdentifier)
                        .add("sourceCorrelationId", reportId.toString())
                        .add("reason", "Test generation failure")
                        .build());
    }
}
