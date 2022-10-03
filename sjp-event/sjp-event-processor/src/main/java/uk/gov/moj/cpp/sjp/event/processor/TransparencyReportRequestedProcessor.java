package uk.gov.moj.cpp.sjp.event.processor;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.apache.commons.lang3.StringUtils.length;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.sjp.event.processor.DateTimeUtil.formatDateTimeForReport;
import static uk.gov.moj.cpp.sjp.event.processor.helper.JsonObjectConversionHelper.jsonObjectAsByteArray;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.sjp.event.processor.exception.OffenceNotFoundException;
import uk.gov.moj.cpp.sjp.event.processor.service.ExportType;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportRequested;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private static final int DEFENDANT_IS_18 = 18;
    private static final String OFFENCES = "offences";

    private Table<String, String, JsonObject> offenceDataTable;
    private Table<String, Boolean, String> prosecutorDataTable;

    @Inject
    private FileStorer fileStorer;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ReferenceDataOffencesService referenceDataOffencesService;

    @Inject
    private SjpService sjpService;

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender sender;

    @SuppressWarnings("squid:S00112")
    @Handles(TransparencyReportRequested.EVENT_NAME)
    @Transactional
    public void createTransparencyReport(final JsonEnvelope envelope) {
        initCache();

        final JsonObject eventPayload = envelope.payloadAsJsonObject();
        final UUID transparencyReportId = fromString(eventPayload.getString("transparencyReportId"));
        final List<JsonObject> allPendingCasesFromViewStore = getPendingCasesFromViewStore(envelope);
        final List<JsonObject> filteredCases = getFilteredCases(allPendingCasesFromViewStore);
        storeReportMetadata(envelope, transparencyReportId, filteredCases);
        try {
            final JsonObject payloadForDocumentGenerationEnglish = buildPayloadForDocumentGeneration(filteredCases, false, envelope);
            final String englishPayloadFileName = String.format("transparency-report-template-parameters.english.%s.json", transparencyReportId.toString());
            final UUID englishPayloadFileId = storeDocumentGeneratorPayload(payloadForDocumentGenerationEnglish, englishPayloadFileName, TEMPLATE_IDENTIFIER);
            requestDocumentGeneration(envelope, transparencyReportId, TEMPLATE_IDENTIFIER, englishPayloadFileId);

            final JsonObject payloadForDocumentGenerationWelsh = buildPayloadForDocumentGeneration(filteredCases, true, envelope);
            final String welshPayloadFileName = String.format("transparency-report-template-parameters.welsh.%s.json", transparencyReportId.toString());
            final UUID welshPayloadFileId = storeDocumentGeneratorPayload(payloadForDocumentGenerationWelsh, welshPayloadFileName, TEMPLATE_IDENTIFIER_WELSH);
            requestDocumentGeneration(envelope, transparencyReportId, TEMPLATE_IDENTIFIER_WELSH, welshPayloadFileId);

        } catch (FileServiceException e) {
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
        return sjpService.getPendingCases(envelope, ExportType.PUBLIC);
    }

    private List<JsonObject> getFilteredCases(final List<JsonObject> pendingCases) {
        return pendingCases.stream()
                .filter(this::caseIsForAdultDefendant)
                .filter(this::isCaseWithoutPressRestriction)
                .collect(Collectors.toList());
    }

    private boolean caseIsForAdultDefendant(final JsonObject pendingCase) {
        final LocalDate defendantDateOfBirth = Optional.ofNullable(pendingCase.getString("defendantDateOfBirth", null)).map(LocalDate::parse).orElse(null);
        final LocalDate earliestOffenceDate = pendingCase.getJsonArray(OFFENCES)
                .getValuesAs(JsonObject.class).stream()
                .map(offence -> offence.getString("offenceStartDate"))
                .map(LocalDate::parse)
                .min(Comparator.naturalOrder()).orElse(null);
        // Remove cases where the offender is younger than 18 years old for one of the offences
        if (nonNull(defendantDateOfBirth) && nonNull(earliestOffenceDate)) {
            final Period defendantAgeAtOffenceDate = Period.between(defendantDateOfBirth, earliestOffenceDate);
            return defendantAgeAtOffenceDate.getYears() >= DEFENDANT_IS_18;
        }
        return true;
    }

    private JsonObject buildPayloadForDocumentGeneration(final List<JsonObject> pendingCases, boolean isWelsh, final JsonEnvelope envelope) {
        return createObjectBuilder()
                .add("generatedDateAndTime", formatDateTimeForReport(now(), isWelsh))
                .add("totalNumberOfRecords", pendingCases.size())
                .add("readyCases", createPendingCasesJsonArrayBuilderFromListOfPendingCases(pendingCases, isWelsh, envelope))
                .build();
    }

    private void requestDocumentGeneration(final JsonEnvelope eventEnvelope,
                                           final UUID transparencyReportId,
                                           final String template,
                                           final UUID payloadFileServiceUUID) {

        final JsonObject docGeneratorPayload = createObjectBuilder()
                .add("originatingSource", "sjp")
                .add("templateIdentifier", template)
                .add("conversionFormat", "pdf")
                .add("sourceCorrelationId", transparencyReportId.toString())
                .add("payloadFileServiceId", payloadFileServiceUUID.toString())
                .build();

        sender.sendAsAdmin(
                Envelope.envelopeFrom(
                    metadataFrom(eventEnvelope.metadata()).withName("systemdocgenerator.generate-document"),
                    docGeneratorPayload
                )
        );

    }

    private UUID storeDocumentGeneratorPayload(final JsonObject documentGeneratorPayload, final String fileName, final String templateIdentifier) throws FileServiceException {
        final byte[] jsonPayloadInBytes = jsonObjectAsByteArray(documentGeneratorPayload);

        final JsonObject metadata = createObjectBuilder()
                .add("fileName", fileName)
                .add("conversionFormat","pdf")
                .add("templateName", templateIdentifier)
                .add("numberOfPages", 1)
                .add("fileSize", jsonPayloadInBytes.length)
                .build();
        return fileStorer.store(metadata, new ByteArrayInputStream(jsonPayloadInBytes));
    }

    private void storeReportMetadata(final JsonEnvelope envelope,
                                     final UUID transparencyReportId,
                                     final List<JsonObject> pendingCases) {

        final JsonEnvelope envelopeToSend = envelopeFrom(
                metadataFrom(envelope.metadata()).withName("sjp.command.store-transparency-report-data"),
                createObjectBuilder()
                        .add("transparencyReportId", transparencyReportId.toString())
                        .add("caseIds", createJsonArrayWithCaseIds(pendingCases))
                        .build());
        sender.send(envelopeToSend);
    }

    private JsonArrayBuilder createPendingCasesJsonArrayBuilderFromListOfPendingCases(final List<JsonObject> pendingCases, final Boolean isWelsh, final JsonEnvelope envelope) {
        final JsonArrayBuilder pendingCasesBuilder = createArrayBuilder();
        pendingCases.stream()
                .forEach(pendingCase -> {
                    final JsonObjectBuilder pendingCaseBuilder = createObjectBuilder()
                            .add("defendantName",buildDefendantName(pendingCase))
                            .add("offenceTitle", buildOffenceTitleFromOffenceArray(pendingCase.getJsonArray(OFFENCES), isWelsh, envelope))
                            .add("prosecutorName", buildProsecutorName(pendingCase.getString("prosecutorName"), isWelsh, envelope));

                     ofNullable(pendingCase.getString("postcode", null))
                             .ifPresent(postcode -> pendingCaseBuilder.add("postcode",getPostcodePrefix (postcode)));

                    pendingCasesBuilder.add(pendingCaseBuilder);
                });
        return pendingCasesBuilder;
    }
    private String getPostcodePrefix(final String postcode) {
        return  length(postcode) > 2 ? postcode.substring(0, 2) : postcode;

    }

    private boolean isCaseWithoutPressRestriction(final JsonObject pendingCase) {
        return pendingCase.getJsonArray(OFFENCES)
                .getValuesAs(JsonObject.class).stream()
                .filter(offence -> !offence.getBoolean("completed", false))
                .map(offence -> offence.getJsonObject("pressRestriction"))
                .noneMatch(pressRestriction -> pressRestriction.getBoolean("requested"));
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

    private String buildDefendantName(final JsonObject pendingCase) {
        if (pendingCase.containsKey("legalEntityName")) {
            return pendingCase.getString("legalEntityName").toUpperCase();
        } else {
            return format("%s %s", pendingCase.getString("firstName", ""), pendingCase.getString("lastName", "").toUpperCase());
        }
    }

}
