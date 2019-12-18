package uk.gov.moj.cpp.sjp.event.processor;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.Optional.ofNullable;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.LF;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.sjp.event.processor.DateTimeUtil.formatDateTimeForReport;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.sjp.event.processor.exception.OffenceNotFoundException;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.utils.PdfHelper;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportRequested;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.transaction.Transactional;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class TransparencyReportRequestedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransparencyReportRequestedProcessor.class);

    private static final String TEMPLATE_IDENTIFIER = "PendingCasesEnglish";
    private static final String TEMPLATE_IDENTIFIER_WELSH = "PendingCasesWelsh";

    private Table<String, String, JsonObject> offenceDataTable;
    private Table<String, Boolean, String> prosecutorDataTable;

    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Inject
    private PdfHelper pdfHelper;

    @Inject
    private FileStorer fileStorer;

    @Inject
    private Enveloper enveloper;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ReferenceDataOffencesService referenceDataOffencesService;

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender sender;

    @SuppressWarnings("squid:S00112")
    @Handles(TransparencyReportRequested.EVENT_NAME)
    @Transactional
    public void createTransparencyReport(final JsonEnvelope envelope) {
        initCache();

        final List<JsonObject> pendingCasesFromViewStore = getPendingCasesFromViewStore(envelope);
        try {
            final JsonObject payloadForDocumentGenerationEnglish = buildPayloadForDocumentGeneration(pendingCasesFromViewStore, false, envelope);
            final JsonObject englishTransparencyDocumentMetadata = generateDocument(TEMPLATE_IDENTIFIER, payloadForDocumentGenerationEnglish, "transparency-report-english.pdf");

            final JsonObject payloadForDocumentGenerationWelsh = buildPayloadForDocumentGeneration(pendingCasesFromViewStore, true, envelope);
            final JsonObject welshTransparencyDocumentMetadata = generateDocument(TEMPLATE_IDENTIFIER_WELSH, payloadForDocumentGenerationWelsh, "transparency-report-welsh.pdf");

            storeReportData(envelope, englishTransparencyDocumentMetadata, welshTransparencyDocumentMetadata, pendingCasesFromViewStore);
        } catch (IOException | FileServiceException e) {
            throw new RuntimeException("IO Exception happened during transparency report generation", e);
        }
    }

    // if a second request clears the cache when the first one is doing the processing it could lead to unintended results
    // or better make sure that there are no 2 processings happen at the same time. Can be handeled as part of the performance iteration
    private void initCache() {
        offenceDataTable = HashBasedTable.create();
        prosecutorDataTable = HashBasedTable.create();
    }

    private List<JsonObject> getPendingCasesFromViewStore(final JsonEnvelope envelope) {
        return requester.request(enveloper.withMetadataFrom(envelope, "sjp.query.pending-cases")
                .apply(createObjectBuilder().build()))
                .payloadAsJsonObject()
                .getJsonArray("pendingCases")
                .getValuesAs(JsonObject.class);
    }

    private JsonObject buildPayloadForDocumentGeneration(final List<JsonObject> pendingCases, boolean isWelsh, final JsonEnvelope envelope) {
        return createObjectBuilder()
                .add("generatedDateAndTime", formatDateTimeForReport(now(), isWelsh))
                .add("totalNumberOfRecords", pendingCases.size())
                .add("readyCases", createPendingCasesJsonArrayBuilderFromListOfPendingCases(pendingCases, isWelsh, envelope))
                .build();
    }

    private JsonObject generateDocument(final String template, final JsonObject payload, final String filename) throws IOException, FileServiceException {
        final byte[] pdfDocumentInBytes = this.documentGeneratorClientProducer.documentGeneratorClient()
                .generatePdfDocument(payload, template, getSystemUser());

        final JsonObject metadata = createObjectBuilder()
                .add("fileName", filename)
                .add("numberOfPages", pdfHelper.getDocumentPageCount(pdfDocumentInBytes))
                .add("fileSize", pdfDocumentInBytes.length)
                .build();
        final UUID fileId = fileStorer.store(metadata, new ByteArrayInputStream(pdfDocumentInBytes));

        return JsonObjects.createObjectBuilder(metadata)
                .add("fileId", fileId.toString())
                .build();
    }

    private void storeReportData(final JsonEnvelope envelope,
                                 final JsonObject englishTransparencyDocumentMetadata,
                                 final JsonObject welshTransparencyDocumentMetadata,
                                 final List<JsonObject> pendingCases) {

        final JsonEnvelope envelopeToSend = enveloper.withMetadataFrom(envelope,
                "sjp.command.store-transparency-report-data")
                .apply(createObjectBuilder()
                        .add("englishReportMetadata", englishTransparencyDocumentMetadata)
                        .add("welshReportMetadata", welshTransparencyDocumentMetadata)
                        .add("caseIds", createJsonArrayWithCaseIds(pendingCases))
                        .build());
        sender.send(envelopeToSend);
    }

    private UUID getSystemUser() {
        return systemUserProvider.getContextSystemUserId()
                .orElseThrow(() -> new RuntimeException("systemUserProvider.getContextSystemUserId() not available"));
    }

    private JsonArrayBuilder createPendingCasesJsonArrayBuilderFromListOfPendingCases(final List<JsonObject> pendingCases, final Boolean isWelsh, final JsonEnvelope envelope) {
        final JsonArrayBuilder pendingCasesBuilder = createArrayBuilder();
        pendingCases.forEach(pendingCase -> {
            final JsonObjectBuilder pendingCaseBuilder = createObjectBuilder()
                    .add("defendantName", pendingCase.getString("defendantName"))
                    .add("postcode", pendingCase.getString("postcode"))
                    .add("offenceTitle", buildOffenceTitleFromOffenceArray(pendingCase.getJsonArray("offences"), isWelsh, envelope))
                    .add("prosecutorName", buildProsecutorName(pendingCase.getString("prosecutorName"), isWelsh, envelope));

            ofNullable(pendingCase.getString("town", null))
                    .ifPresent(town -> pendingCaseBuilder.add("town", town));

            ofNullable(pendingCase.getString("county", null))
                    .ifPresent(county -> pendingCaseBuilder.add("county", county));

            pendingCasesBuilder.add(pendingCaseBuilder);
        });
        return pendingCasesBuilder;
    }

    private String buildProsecutorName(final String prosecutorName, final Boolean isWelsh, final JsonEnvelope envelope) {
        final String prosecutor;
        if (!prosecutorDataTable.contains(prosecutorName, isWelsh)) {
            prosecutor = referenceDataService.getProsecutor(prosecutorName, isWelsh, envelope);
            prosecutorDataTable.put(prosecutorName, isWelsh, prosecutor);
        } else {
            prosecutor = prosecutorDataTable.get(prosecutorName, isWelsh);
        }

        return prosecutor;
    }

    private String buildOffenceTitleFromOffenceArray(final JsonArray offenceJsonArray, final Boolean isWelsh, final JsonEnvelope envelope) {
        // REFDATA-219 -- Call reference data offences only once by passing all the offence codes and the service should return the offences including the legacy versions
        return offenceJsonArray.getValuesAs(JsonObject.class).stream()
                .map(e -> mapOffenceIntoOffenceTitleString(e, isWelsh, envelope))
                .reduce((offenceTitle1, offenceTitle2) -> offenceTitle1.concat(LF).concat(offenceTitle2))
                .orElseThrow(() -> new RuntimeException("Error during processing payload for document generator! "));
    }

    private String mapOffenceIntoOffenceTitleString(final JsonObject offence, final Boolean isWelsh, final JsonEnvelope envelope) {
        final String offenceCode = offence.getString("offenceCode");
        final String offenceStartDate = offence.getString("offenceStartDate");

        final JsonObject offenceReferenceData;
        if (!offenceDataTable.contains(offenceCode, offenceStartDate)) {
            offenceReferenceData = referenceDataOffencesService
                    .getOffenceReferenceData(envelope, offenceCode, offenceStartDate)
                    .orElseThrow(() -> new OffenceNotFoundException(
                            format("Referral decision not found for case %s",
                                    offenceCode))
                    );
            offenceDataTable.put(offenceCode, offenceStartDate, offenceReferenceData);
        } else {
            offenceReferenceData = offenceDataTable.get(offenceCode, offenceStartDate);
        }

        return getOffenceTitle(offenceReferenceData, offenceCode, isWelsh);
    }

    private JsonArrayBuilder createJsonArrayWithCaseIds(final List<JsonObject> pendingCases) {
        final JsonArrayBuilder pendingCasesBuilder = createArrayBuilder();
        pendingCases.stream().map(e -> e.getString("caseId")).forEach(pendingCasesBuilder::add);
        return pendingCasesBuilder;
    }

    private String getOffenceTitle(final JsonObject offenceReferenceData, final String offenceCode, final Boolean isWelsh) {
        if (!isWelsh) {
            return offenceReferenceData.getString("title");
        }

        final Optional<String> offenceTitleWelshOptional = JsonObjects.getString(offenceReferenceData, "details", "document", "welsh", "welshoffencetitle");
        return offenceTitleWelshOptional.orElseGet(() -> {
            LOGGER.warn("No welsh offence referencedata translations for offenceCode: {}", offenceCode);
            return offenceReferenceData.getString("title");
        });
    }

}
