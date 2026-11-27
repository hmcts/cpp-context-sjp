package uk.gov.moj.cpp.sjp.event.processor;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.length;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.domain.DocumentLanguage.WELSH;
import static uk.gov.moj.cpp.sjp.domain.DocumentRequestType.FULL;
import static uk.gov.moj.cpp.sjp.event.processor.DateTimeUtil.ENGLISH_TITLE_DATE_FORMATTER;
import static uk.gov.moj.cpp.sjp.event.processor.DateTimeUtil.formatDateTimeForPdfReport;
import static uk.gov.moj.cpp.sjp.event.processor.DateTimeUtil.formatPublicationDateTimeForJsonReport;
import static uk.gov.moj.cpp.sjp.event.processor.DateTimeUtil.getDateTimeForDeltaReport;
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
import uk.gov.moj.cpp.sjp.domain.ListType;
import uk.gov.moj.cpp.sjp.event.processor.service.CourtListPublishingService;
import uk.gov.moj.cpp.sjp.event.processor.service.ExportType;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.utils.PayloadHelper;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyJSONReportRequested;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyPDFReportRequested;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1067", "squid:S3776", "squid:S1450", "squid:S1068"})
@ServiceComponent(EVENT_PROCESSOR)
public class TransparencyReportRequestedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransparencyReportRequestedProcessor.class);

    private static final int DEFENDANT_IS_18 = 18;
    private static final String SJP_OFFENCES = "sjpOffences";
    private static final String OFFENCES = "offences";
    private static final String LIST_TYPE = "listType";
    private static final String SJP_PUBLIC_LIST = "SJP_PUBLIC_LIST";
    private static final String SJP_DELTA_PUBLIC_LIST = "SJP_DELTA_PUBLIC_LIST";
    public static final String DEFENDANT_NAME = "defendantName";
    public static final String PROSECUTOR_NAME = "prosecutorName";
    public static final String TITLE = "title";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String POSTCODE = "postcode";
    public static final String STRING_FORMAT = "%s,";
    public static final String OFFENCE_TITLE = "offenceTitle";
    private static final String LANGUAGE = "language";
    private static final String REQUEST_TYPE = "requestType";
    public static final String TRANSPARENCY_REPORT_ID = "transparencyReportId";
    public static final String LIST_PAYLOAD = "listPayload";
    private static final String CONVERSION_FORMAT = "pdf";
    public static final String LEGAL_ENTITY_NAME = "legalEntityName";
    public static final String TEMPLATE_IDENTIFIER_STRING = "templateIdentifier";
    public static final String CONVERSION_FORMAT_STRING = "conversionFormat";

    @Inject
    private FileStorer fileStorer;

    @Inject
    private SjpService sjpService;

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private PayloadHelper payloadHelper;

    @Inject
    private CourtListPublishingService courtListPublishingService;

    private String getTemplateIdentifier(final String type, final String lang) {
        return "PublicPendingCases" + type + lang;
    }

    @SuppressWarnings("squid:S00112")
    @Handles(TransparencyPDFReportRequested.EVENT_NAME)
    @Transactional
    public void createTransparencyPDFReport(final JsonEnvelope envelope) {
        payloadHelper.initCache();

        final JsonObject eventPayload = envelope.payloadAsJsonObject();
        final UUID transparencyReportId = fromString(eventPayload.getString(TRANSPARENCY_REPORT_ID));
        final List<JsonObject> allPendingCasesFromViewStore = getPendingCasesFromViewStore(envelope);
        final List<JsonObject> filteredCases = getFilteredCases(allPendingCasesFromViewStore);
        final boolean isWelsh = WELSH.name().equalsIgnoreCase(eventPayload.getString(LANGUAGE));
        try {
            final JsonObject payloadForDocumentGeneration = buildPayload(filteredCases, isWelsh, false, envelope);
            requestDocumentGeneration(envelope, transparencyReportId, payloadForDocumentGeneration);
            storeReportMetadata(envelope, transparencyReportId, filteredCases);
        } catch (FileServiceException e) {
            throw new RuntimeException("IO Exception happened during transparency report generation", e);
        }
    }

    @SuppressWarnings("squid:S00112")
    @Handles(TransparencyJSONReportRequested.EVENT_NAME)
    @Transactional
    public void createTransparencyJSONReport(final JsonEnvelope envelope) {
        payloadHelper.initCache();

        final List<JsonObject> allPendingCasesFromViewStore = getPendingCasesFromViewStore(envelope);
        final JsonObject eventPayload = envelope.payloadAsJsonObject();
        final UUID transparencyReportId = fromString(eventPayload.getString(TRANSPARENCY_REPORT_ID));
        final boolean isWelsh = WELSH.name().equalsIgnoreCase(eventPayload.getString(LANGUAGE));
        LOGGER.info("generating public transparency JSON report {}", transparencyReportId);
        final List<JsonObject> filteredCases = getFilteredCases(allPendingCasesFromViewStore);
        LOGGER.info("generating public transparency JSON report for press report {}", transparencyReportId);
        publishCourtList(envelope, buildPayload(filteredCases, isWelsh, true, envelope));
        LOGGER.info("completed handling public transparency JSON report request for press report {}", transparencyReportId);
        storeReportMetadata(envelope, transparencyReportId, filteredCases);
    }

    private void requestDocumentGeneration(final JsonEnvelope envelope, final UUID reportId, final JsonObject payload) throws FileServiceException {
        final String payloadFileName = String.format("transparency-report-template-parameters.%s.json", reportId.toString());
        String type = envelope.payloadAsJsonObject().getString(REQUEST_TYPE).toLowerCase();
        type = type.substring(0, 1).toUpperCase() + type.substring(1);
        String language = envelope.payloadAsJsonObject().getString(LANGUAGE).toLowerCase();
        language = language.substring(0, 1).toUpperCase() + language.substring(1);
        final UUID payloadFileId = storeDocumentGeneratorPayload(payload, payloadFileName, type, language);
        sendDocumentGenerationRequest(envelope, reportId, payloadFileId, type, language);
    }

    private void sendDocumentGenerationRequest(final JsonEnvelope eventEnvelope,
                                               final UUID reportId,
                                               final UUID payloadFileServiceUUID, final String type, final String language) {

        final JsonObject docGeneratorPayload = createObjectBuilder()
                .add("originatingSource", "sjp")
                .add(TEMPLATE_IDENTIFIER_STRING, getTemplateIdentifier(type, language))
                .add(CONVERSION_FORMAT_STRING, CONVERSION_FORMAT)
                .add("sourceCorrelationId", reportId.toString())
                .add("payloadFileServiceId", payloadFileServiceUUID.toString())
                .build();

        sender.sendAsAdmin(
                Envelope.envelopeFrom(
                        metadataFrom(eventEnvelope.metadata()).withName("systemdocgenerator.generate-document"),
                        docGeneratorPayload
                )
        );
    }

    private void publishCourtList(final JsonEnvelope envelope, final JsonObject payloadForDocumentGeneration) {
        LOGGER.info("publishing sjp pending cases public list to court list publishing service");
        final String type = envelope.payloadAsJsonObject().getString(REQUEST_TYPE);
        final String language = envelope.payloadAsJsonObject().getString(LANGUAGE);
        // FULL/DELTA is part of the CaTH list-type vocabulary, not a separate field: sending
        // SJP_PUBLIC_LIST for delta content makes CaTH render it with the full-list template.
        final String listType = FULL.name().equals(type) ? SJP_PUBLIC_LIST : SJP_DELTA_PUBLIC_LIST;
        LOGGER.info("building sjp public court list publish request, listType {}, requestType {}, language {}",
                listType, type, language);
        final JsonObject courtListPublishRequest = createObjectBuilder()
                .add(LIST_TYPE, listType)
                .add(LANGUAGE, language)
                .add(REQUEST_TYPE, type)
                .add(LIST_PAYLOAD, payloadForDocumentGeneration)
                .build();
        LOGGER.info("publishing sjp public court list pending cases list to court list publishing service");

        try {
            courtListPublishingService.publishCourtList(courtListPublishRequest.toString());
        } catch (IOException e) {
            LOGGER.error("IO Exception happened while publishing sjp public court list", e);
            throw new RuntimeException("IO Exception happened while publishing sjp public court list", e);
        }
    }

    private List<JsonObject> getPendingCasesFromViewStore(final JsonEnvelope envelope) {
        final ListType requestType = ListType.valueOf(envelope.payloadAsJsonObject().getString(REQUEST_TYPE));
        if (requestType.equals(ListType.FULL)) {
            return sjpService.getPendingCases(envelope, ExportType.PUBLIC);
        }
        return sjpService.getPendingDeltaCases(envelope, ExportType.PUBLIC);
    }

    private List<JsonObject> getFilteredCases(final List<JsonObject> pendingCases) {
        return pendingCases.stream()
                .filter(this::caseIsForAdultDefendant)
                .filter(this::isCaseWithoutPressRestriction)
                .toList();
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

    private JsonObject buildPayload(final List<JsonObject> pendingCases, boolean isWelsh, final boolean isPayloadForPublicEvent, final JsonEnvelope envelope) {
        final JsonArrayBuilder readyCasesBuilder = isPayloadForPublicEvent ? createPendingCasesJsonArrayBuilderFromListOfPendingCasesForPublicEvent(pendingCases, isWelsh, envelope) :
                createPendingCasesJsonArrayBuilderFromListOfPendingCases(pendingCases, isWelsh, envelope);
        return createObjectBuilder()
                .add("generatedDateAndTime", isPayloadForPublicEvent ? formatPublicationDateTimeForJsonReport(now(), isWelsh) : formatDateTimeForPdfReport(now(), isWelsh))
                .add("totalNumberOfRecords", pendingCases.size())
                .add("readyCases", readyCasesBuilder.build())
                .add("startDate", payloadHelper.getStartDate(isWelsh))
                .build();
    }

    private UUID storeDocumentGeneratorPayload(final JsonObject documentGeneratorPayload, final String fileName, final String type, final String language) throws FileServiceException {
        final byte[] jsonPayloadInBytes = jsonObjectAsByteArray(documentGeneratorPayload);

        final JsonObject metadata = createObjectBuilder()
                .add("fileName", fileName)
                .add(CONVERSION_FORMAT_STRING, CONVERSION_FORMAT)
                .add(TEMPLATE_IDENTIFIER_STRING, getTemplateIdentifier(type, language))
                .add("numberOfPages", 1)
                .add("fileSize", jsonPayloadInBytes.length)
                .build();
        return fileStorer.store(metadata, new ByteArrayInputStream(jsonPayloadInBytes));
    }

    private void storeReportMetadata(final JsonEnvelope envelope,
                                     final UUID transparencyReportId,
                                     final List<JsonObject> pendingCases) {

        String type = envelope.payloadAsJsonObject().getString(REQUEST_TYPE).toLowerCase();
        type = type.substring(0, 1).toUpperCase() + type.substring(1);
        String language = envelope.payloadAsJsonObject().getString(LANGUAGE).toLowerCase();
        language = language.substring(0, 1).toUpperCase() + language.substring(1);
        final String format = envelope.payloadAsJsonObject().getString("format");

        final JsonEnvelope envelopeToSend = envelopeFrom(
                metadataFrom(envelope.metadata()).withName("sjp.command.store-transparency-report-data"),
                createObjectBuilder()
                        .add(TRANSPARENCY_REPORT_ID, transparencyReportId.toString())
                        .add("caseIds", createJsonArrayWithCaseIds(pendingCases))
                        .add("format", format)
                        .add(REQUEST_TYPE, type.toUpperCase())
                        .add(LANGUAGE, language.toUpperCase())
                        .add(TITLE, getTitle(type, language))
                        .build());
        sender.send(envelopeToSend);
    }

    private String getTitle(String type, String language) {
        final String titleLanguage = " (" + language + ")";
        return type.equalsIgnoreCase(FULL.name()) ? "All Pending cases" +
                titleLanguage : "New cases, since " + getDateTimeForDeltaReport().format(ENGLISH_TITLE_DATE_FORMATTER) + titleLanguage;
    }

    private JsonArrayBuilder createPendingCasesJsonArrayBuilderFromListOfPendingCases(final List<JsonObject> pendingCases, final Boolean isWelsh, final JsonEnvelope envelope) {
        final JsonArrayBuilder pendingCasesBuilder = createArrayBuilder();
        pendingCases
                .forEach(pendingCase -> {
                    final JsonObjectBuilder pendingCaseBuilder = createObjectBuilder()
                            .add(DEFENDANT_NAME, buildDefendantName(pendingCase))
                            .add(OFFENCE_TITLE, payloadHelper.buildOffenceTitleFromOffenceArray(pendingCase.getJsonArray(OFFENCES), isWelsh, envelope))
                            .add(PROSECUTOR_NAME, payloadHelper.buildProsecutorName(pendingCase.getString(PROSECUTOR_NAME), isWelsh, envelope));

                    ofNullable(pendingCase.getString(POSTCODE, null))
                            .ifPresent(postcode -> pendingCaseBuilder.add(POSTCODE, getPostcodePrefix(postcode)));

                    pendingCasesBuilder.add(pendingCaseBuilder);
                });
        return pendingCasesBuilder;
    }

    private JsonArrayBuilder createPendingCasesJsonArrayBuilderFromListOfPendingCasesForPublicEvent(final List<JsonObject> pendingCases, final Boolean isWelsh, final JsonEnvelope envelope) {
        final JsonArrayBuilder pendingCasesBuilder = createArrayBuilder();
        pendingCases
                .forEach(pendingCase -> {
                    final JsonObjectBuilder pendingCaseBuilder = createObjectBuilder();
                    JsonObjects.getString(pendingCase, FIRST_NAME).ifPresent(firstName -> pendingCaseBuilder.add(FIRST_NAME, getDefendantFirstName(pendingCase)));
                    JsonObjects.getString(pendingCase, LAST_NAME).ifPresent(lastName -> pendingCaseBuilder.add(LAST_NAME, getDefendantLastName(pendingCase)));
                    JsonObjects.getString(pendingCase, POSTCODE).ifPresent(postcode -> pendingCaseBuilder.add(POSTCODE, getPostcodePrefix(postcode)));
                    JsonObjects.getString(pendingCase, LEGAL_ENTITY_NAME).ifPresent(legalEntity -> pendingCaseBuilder.add(LEGAL_ENTITY_NAME, format(STRING_FORMAT, legalEntity)));
                    pendingCaseBuilder
                            .add(SJP_OFFENCES, buildOffencesArrayFromOffenceArrayForPublicEvent(pendingCase.getJsonArray(OFFENCES), isWelsh, envelope))
                            .add(PROSECUTOR_NAME, payloadHelper.buildProsecutorName(pendingCase.getString(PROSECUTOR_NAME), isWelsh, envelope));

                    getPersonDefendantFullName(pendingCase).ifPresent(defendantName -> pendingCaseBuilder.add(DEFENDANT_NAME, defendantName.trim()));
                    pendingCasesBuilder.add(pendingCaseBuilder);
                });
        return pendingCasesBuilder;
    }

    private String getPostcodePrefix(final String postcode) {
        return length(postcode) > 2 ? postcode.substring(0, 2) : postcode;
    }

    private boolean isCaseWithoutPressRestriction(final JsonObject pendingCase) {
        return pendingCase.getJsonArray(OFFENCES)
                .getValuesAs(JsonObject.class).stream()
                .filter(offence -> !offence.getBoolean("completed", false))
                .map(offence -> offence.getJsonObject("pressRestriction"))
                .noneMatch(pressRestriction -> pressRestriction.getBoolean("requested"));
    }

    private JsonArray buildOffencesArrayFromOffenceArrayForPublicEvent(final JsonArray pendingCaseOffences, final Boolean isWelsh, final JsonEnvelope envelope) {
        final JsonArrayBuilder readyCaseOffences = createArrayBuilder();
        pendingCaseOffences.getValuesAs(JsonObject.class).forEach(pendingCaseOffence -> {
            final JsonObjectBuilder readyCaseOffence = createObjectBuilder()
                    .add(TITLE, payloadHelper.mapOffenceIntoOffenceTitleString(pendingCaseOffence, isWelsh, envelope));
            readyCaseOffences.add(readyCaseOffence);
        });
        return readyCaseOffences.build();
    }

    private JsonArrayBuilder createJsonArrayWithCaseIds(final List<JsonObject> pendingCases) {
        final JsonArrayBuilder pendingCasesBuilder = createArrayBuilder();
        pendingCases.stream().map(e -> e.getString("caseId")).forEach(pendingCasesBuilder::add);
        return pendingCasesBuilder;
    }

    private String buildDefendantName(final JsonObject pendingCase) {
        if (pendingCase.containsKey(LEGAL_ENTITY_NAME)) {
            return pendingCase.getString(LEGAL_ENTITY_NAME).toUpperCase();
        } else {
            return format("%s %s", pendingCase.getString(FIRST_NAME).length() > 0 ? pendingCase.getString(FIRST_NAME).toUpperCase().charAt(0) : "", capitalize(lowerCase(pendingCase.getString(LAST_NAME, ""))));
        }
    }

    private String getDefendantFirstName(final JsonObject pendingCase) {
        return pendingCase.getString(FIRST_NAME).length() > 0 ? String.valueOf(pendingCase.getString(FIRST_NAME).toUpperCase().charAt(0)) : "";
    }

    private String getDefendantLastName(final JsonObject pendingCase) {
        return capitalize(lowerCase(pendingCase.getString(LAST_NAME, "")));
    }

    private Optional<String> getPersonDefendantFullName(final JsonObject pendingCase) {
        if( JsonObjects.getString(pendingCase, FIRST_NAME).isPresent() || JsonObjects.getString(pendingCase, LAST_NAME).isPresent() ) {
            return Optional.of(format("%s %s", pendingCase.getString(FIRST_NAME).length() > 0 ? pendingCase.getString(FIRST_NAME).toUpperCase().charAt(0) : "", capitalize(lowerCase(pendingCase.getString(LAST_NAME, "")))));
        }
        return Optional.empty();
    }
}
