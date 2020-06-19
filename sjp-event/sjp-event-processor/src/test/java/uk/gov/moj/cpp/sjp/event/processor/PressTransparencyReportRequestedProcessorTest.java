package uk.gov.moj.cpp.sjp.event.processor;

import static java.time.LocalTime.now;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.event.processor.helper.JsonObjectConversionHelper.streamToJsonObject;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PressTransparencyReportRequestedProcessorTest {

    private static final DateTimeFormatter DOB_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @InjectMocks
    private PressTransparencyReportRequestedProcessor processor;

    @Mock
    private FileStorer fileStorer;

    @Mock
    private ReferenceDataOffencesService referenceDataOffencesService;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private Sender sender;

    @Mock
    private SjpService sjpService;

    @Captor
    private ArgumentCaptor<InputStream> payloadForDocumentGenerationCaptor;

    @Captor
    private ArgumentCaptor<Envelope> documentGenerationRequestCaptor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> storePressTransparencyReportCommandEnvelopeCaptor;

    private static final String EXPECTED_TEMPLATE_NAME = "PressPendingCasesEnglish";


    @Test
    public void shouldCreatePressTransparencyReport() throws FileServiceException {
        final UUID payloadDocumentGeneratorFileId = randomUUID();
        final UUID reportId = randomUUID();

        final String offenceTitle = "OffenceTitle";
        final String prosecutorName = "TFL";
        final String prosecutor = "Transport For London";
        final int numberOfPendingCasesForExport = 9;
        final List<UUID> caseIds = range(0, numberOfPendingCasesForExport)
                .mapToObj(e -> randomUUID()).collect(toList());

        final Optional<JsonObject> CA03011_referenceDataPayload = Optional.of(createObjectBuilder()
                .add("title", offenceTitle)
                .build());

        when(referenceDataService.getProsecutor(eq(prosecutorName), eq(false), any())).thenReturn(prosecutor);
        when(referenceDataOffencesService.getOffenceReferenceData(any(), anyString(), anyString())).thenReturn(CA03011_referenceDataPayload);

        final List<JsonObject> pendingCasesList = pendingCasesList(caseIds, false);
        when(sjpService.getPendingCases(any())).thenReturn(pendingCasesList);
        when(fileStorer.store(any(), any())).thenReturn(payloadDocumentGeneratorFileId);


        final JsonEnvelope privateEventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.press-transparency-report-requested"),
                createObjectBuilder()
                        .add("pressTransparencyReportId", reportId.toString())
                        .build()
        );
        processor.handlePressTransparencyRequest(privateEventEnvelope);

        verify(fileStorer).store(any(JsonObject.class), payloadForDocumentGenerationCaptor.capture());

        final InputStream payloadBytes = payloadForDocumentGenerationCaptor.getValue();
        final JsonObject payloadForDocumentGenerator = streamToJsonObject(payloadBytes);

        assertPayloadForDocumentGenerator(payloadForDocumentGenerator, pendingCasesList, numberOfPendingCasesForExport);
        verify(referenceDataService).getProsecutor(eq(prosecutorName), eq(false), any());


        verify(sender).sendAsAdmin(documentGenerationRequestCaptor.capture());

        final JsonObject expectedDocGenerationPayload = createObjectBuilder()
                .add("originatingSource", "sjp")
                .add("templateIdentifier", EXPECTED_TEMPLATE_NAME)
                .add("conversionFormat", "pdf")
                .add("sourceCorrelationId", reportId.toString())
                .add("payloadFileServiceId", payloadDocumentGeneratorFileId.toString())
                .build();

        assertDocumentGenerationRequest(documentGenerationRequestCaptor.getValue(), expectedDocGenerationPayload);

        verify(sender).send(storePressTransparencyReportCommandEnvelopeCaptor.capture());
        assertPressTransparencyReportEnvelope(storePressTransparencyReportCommandEnvelopeCaptor.getValue(), reportId, caseIds);
    }

    @Test
    public void shouldExcludeYouthDefendants() throws FileServiceException {
        final UUID payloadDocumentGeneratorFileId = randomUUID();
        final UUID reportId = randomUUID();

        final String offenceTitle = "OffenceTitle";
        final String prosecutorName = "TFL";
        final String prosecutor = "Transport For London";
        final int numberOfPendingCasesForExport = 9;
        final List<UUID> caseIds = range(0, numberOfPendingCasesForExport)
                .mapToObj(e -> randomUUID()).collect(toList());

        final Optional<JsonObject> CA03011_referenceDataPayload = Optional.of(createObjectBuilder()
                .add("title", offenceTitle)
                .build());

        when(referenceDataService.getProsecutor(eq(prosecutorName), eq(false), any())).thenReturn(prosecutor);
        when(referenceDataOffencesService.getOffenceReferenceData(any(), anyString(), anyString())).thenReturn(CA03011_referenceDataPayload);

        final List<JsonObject> pendingCasesList = pendingCasesList(caseIds, true);
        when(sjpService.getPendingCases(any())).thenReturn(pendingCasesList);
        when(fileStorer.store(any(), any())).thenReturn(payloadDocumentGeneratorFileId);

        final JsonEnvelope privateEventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.press-transparency-report-requested"),
                createObjectBuilder()
                        .add("pressTransparencyReportId", reportId.toString())
                        .build()
        );
        processor.handlePressTransparencyRequest(privateEventEnvelope);

        verify(fileStorer).store(any(JsonObject.class), payloadForDocumentGenerationCaptor.capture());

        final InputStream payloadBytes = payloadForDocumentGenerationCaptor.getValue();
        final JsonObject payloadForDocumentGenerator = streamToJsonObject(payloadBytes);

        assertRootValuesOfPayloadForDocumentGenerator(payloadForDocumentGenerator, numberOfPendingCasesForExport / 2);
        verify(referenceDataService).getProsecutor(eq(prosecutorName), eq(false), any());

        final JsonObject expectedDocGenerationPayload = createObjectBuilder()
                .add("originatingSource", "sjp")
                .add("templateIdentifier", EXPECTED_TEMPLATE_NAME)
                .add("conversionFormat", "pdf")
                .add("sourceCorrelationId", reportId.toString())
                .add("payloadFileServiceId", payloadDocumentGeneratorFileId.toString())
                .build();

        verify(sender).sendAsAdmin(documentGenerationRequestCaptor.capture());
        assertDocumentGenerationRequest(documentGenerationRequestCaptor.getValue(), expectedDocGenerationPayload);

        verify(sender).send(storePressTransparencyReportCommandEnvelopeCaptor.capture());
        assertPressTransparencyReportEnvelope(storePressTransparencyReportCommandEnvelopeCaptor.getValue(), reportId, caseIds);
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

    private void assertPayloadForDocumentGenerator(final JsonObject payload, final List<JsonObject> pendingCasesList, final Integer totalNumberOfRecords) {
        assertReadyCasesPayloadWithPendingCases(payload, pendingCasesList, "caseUrn");
        assertReadyCasesPayloadWithPendingCases(payload, pendingCasesList, "adress");
        assertReadyCasesPayloadWithPropertyValue(payload, totalNumberOfRecords,"prosecutorName", "Transport For London");
        assertReadyCasesPayloadWithPropertyValue(payload, totalNumberOfRecords,"dateOfBirth", "12 06 1980 (40)");
        assertReadyCasesPayloadWithPropertyValueMatching(payload, "defendantName", "John DOE\\w*");

        assertRootValuesOfPayloadForDocumentGenerator(payload, totalNumberOfRecords);
    }

    private void assertRootValuesOfPayloadForDocumentGenerator(final JsonObject payload, final Integer totalNumberOfRecords) {
        assertThat(payload.getJsonArray("readyCases").size(), is(totalNumberOfRecords));
        assertThat(payload.getInt("totalNumberOfRecords"), is(totalNumberOfRecords));
        assertThat(payload.getString("generatedDateAndTime"), is(notNullValue()));
    }

    private void assertReadyCasesPayloadWithPropertyValue(final JsonObject payload,
                                                          final int numberOfRecords,
                                                          final String jsonField,
                                                          final String expectedValue) {
        assertThat(getPropertyFromPayload(payload, "readyCases", jsonField),
                is(range(0, numberOfRecords).mapToObj(e -> expectedValue).collect(toList())));
    }

    private void assertReadyCasesPayloadWithPropertyValueMatching(final JsonObject payload,
                                                          final String jsonField,
                                                          final String expectedValuePattern) {
        assertTrue(getPropertyFromPayload(payload, "readyCases", jsonField)
                .stream()
                .allMatch(fieldValue -> fieldValue.matches(expectedValuePattern)));
    }

    private void assertReadyCasesPayloadWithPendingCases(final JsonObject payload, final List<JsonObject> pendingCasesList, final String field) {
        assertThat(getPropertyFromPayload(payload, "readyCases", field),
                is(getPropertyFromlist(pendingCasesList, field)));
    }

    private List<String> getPropertyFromPayload(final JsonObject payload, final String containerArray, final String jsonField) {
        return payload.getJsonArray(containerArray)
                .getValuesAs(JsonObject.class).stream()
                .map(e -> e.getString(jsonField, null))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private List<String> getPropertyFromlist(final List<JsonObject> objects, final String jsonField) {
        return objects.stream()
                .map(e -> e.getString(jsonField, null))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private void assertPressTransparencyReportEnvelope(final JsonEnvelope storeTransparencyReportCommandEnvelope,
                                                       final UUID reportId,
                                                       final List<UUID> caseIds) {
        assertThat(storeTransparencyReportCommandEnvelope.metadata().name(), is("sjp.command.store-press-transparency-report-data"));
        assertThat(storeTransparencyReportCommandEnvelope.payloadAsJsonObject().getString("pressTransparencyReportId"), is(reportId.toString()));

        final JsonArray caseIdsJsonArray = storeTransparencyReportCommandEnvelope.payloadAsJsonObject().getJsonArray("caseIds");
        assertThat(caseIdsJsonArray.size(), is(caseIds.size()));
        assertThat(range(0, caseIds.size()).mapToObj(idx -> fromString(caseIdsJsonArray.getString(idx))).collect(toList()), is(caseIds));
    }

    private void assertDocumentGenerationRequest(final Envelope<JsonObject> documentGenerationEnvelope, final JsonObject expectedPayloadForDocumentGeneration) {
        assertThat(documentGenerationEnvelope.metadata().name(), is("systemdocgenerator.generate-document"));
        assertThat(documentGenerationEnvelope.payload(), is(expectedPayloadForDocumentGeneration));
    }

    private List<JsonObject> pendingCasesList(final List<UUID> caseIds, final boolean halfNotAdult) {
        final List<JsonObject> pendingCasesList = new LinkedList<>();

        for (int caseNumber = 0; caseNumber < caseIds.size(); caseNumber++) {
            boolean generateFullAddress = caseNumber % 2 == 0;

            final JsonArrayBuilder offenceArrayBuilder = createArrayBuilder()
                    .add(createObjectBuilder()
                            .add("offenceCode", "CA03011")
                            .add("offenceStartDate", now().toString())
                            .add("offenceWording", "offence wording"))
                    .add(createObjectBuilder()
                            .add("offenceCode", "CA03011")
                            .add("offenceStartDate", LocalDateTime.now().minusMonths(1).toLocalDate().toString())
                            .add("offenceWording", "offence wording")
                    );

            final String defendantDateOfBirth = (halfNotAdult && caseNumber % 2 == 0) ?
                    LocalDate.now().minusYears(15).format(DOB_FORMAT) :
                    "1980-06-12";

            final JsonObjectBuilder pendingCase = createObjectBuilder()
                    .add("caseId", caseIds.get(caseNumber).toString())
                    .add("caseUrn", "TFL000"+caseNumber)
                    .add("defendantName", "J. Doe" + caseNumber)
                    .add("firstName", "John")
                    .add("lastName", "DOE" + caseNumber)
                    .add("defendantDateOfBirth", defendantDateOfBirth)
                    .add("addressLine1", "Flat 7 Hermit House")
                    .add("addressLine2", "3, Long Road")
                    .add("postcode", "SE1 1PJ" + caseNumber)
                    .add("offences", offenceArrayBuilder)
                    .add("prosecutorName", "TFL");

            if (generateFullAddress) {
                pendingCase
                        .add("town", "London" + caseNumber)
                        .add("county", "Greater London" + caseNumber);
            }

            pendingCasesList.add(pendingCase.build());
        }

        return pendingCasesList;
    }

}