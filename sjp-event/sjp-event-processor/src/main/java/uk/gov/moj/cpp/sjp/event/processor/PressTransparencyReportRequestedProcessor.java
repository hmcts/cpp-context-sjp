package uk.gov.moj.cpp.sjp.event.processor;

import static java.lang.String.format;
import static java.time.LocalDate.parse;
import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.YEARS;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.sjp.domain.DocumentLanguage.WELSH;
import static uk.gov.moj.cpp.sjp.domain.DocumentRequestType.FULL;
import static uk.gov.moj.cpp.sjp.event.processor.DateTimeUtil.DATE_FORMAT;
import static uk.gov.moj.cpp.sjp.event.processor.DateTimeUtil.DOB_FORMAT;
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
import uk.gov.moj.cpp.sjp.event.processor.service.ExportType;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.utils.PayloadHelper;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyJSONReportRequested;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyPDFReportRequested;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1067","squid:MethodCyclomaticComplexity"})
@ServiceComponent(EVENT_PROCESSOR)
public class PressTransparencyReportRequestedProcessor {

    public static final String PUBLIC_SJP_PRESS_TRANSPARENCY_REPORT_GENERATED = "public.sjp.press-transparency-report-generated";
    public static final String CASE_URN = "caseUrn";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String ADDRESS = "address";
    public static final String ADDRESS_LINE_1 = "addressLine1";
    public static final String ADDRESS_LINE_2 = "addressLine2";
    public static final String ADDRESS_LINE_3 = "addressLine3";
    public static final String COUNTY = "county";
    public static final String TOWN = "town";
    public static final String POSTCODE = "postcode";
    public static final String DEFENDANT_NAME = "defendantName";
    public static final String LEGAL_ENTITY_NAME = "legalEntityName";
    public static final String OFFENCES = "offences";
    public static final String PROSECUTOR_NAME = "prosecutorName";
    public static final String EMPTY = "";
    public static final String STRING_FORMAT_COMMA = " %s,";
    public static final String STRING_FORMAT_COMMA_PREFIX = ", %s";
    public static final String STRING_FORMAT = " %s";
    public static final String SJP_OFFENCES = "sjpOffences";
    public static final String OFFENCE_WORDING = "offenceWording";
    public static final String OFFENCE_WELSH_WORDING = "offenceWelshWording";
    public static final String PRESS_RESTRICTION_REQUESTED = "pressRestrictionRequested";
    public static final String TITLE = "title";
    public static final String WORDING = "wording";
    private static final Logger LOGGER = LoggerFactory.getLogger(PressTransparencyReportRequestedProcessor.class);
    private static final int ADULT_AGE = 18;
    private static final String DEFENDANT_DATE_OF_BIRTH = "defendantDateOfBirth";
    private static final String DATE_OF_BIRTH = "dateOfBirth";
    private static final String AGE = "age";
    private static final String LANGUAGE = "language";
    private static final String REQUEST_TYPE = "requestType";
    private static final String CONVERSION_FORMAT = "pdf";
    private static final String PRESS_TRANSPARENCY_REPORT_ID = "pressTransparencyReportId";

    @Inject
    private FileStorer fileStorer;
    @Inject
    private SjpService sjpService;
    @Inject
    private ReferenceDataOffencesService referenceDataOffencesService;
    @Inject
    private ReferenceDataService referenceDataService;
    @Inject
    private PayloadHelper payloadHelper;
    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender sender;

    private String getTemplateIdentifier(final String type, final String lang) {
        return "PressPendingCases" + type + lang;
    }

    @Handles(PressTransparencyPDFReportRequested.EVENT_NAME)
    @Transactional
    @SuppressWarnings("squid:S00112")
    public void handlePressTransparencyPDFReportRequest(final JsonEnvelope envelope) {
        payloadHelper.initCache();

        final List<JsonObject> pendingCasesFromViewStore = getPendingCasesFromViewStore(envelope);
        final JsonObject eventPayload = envelope.payloadAsJsonObject();
        final UUID reportId = fromString(eventPayload.getString(PRESS_TRANSPARENCY_REPORT_ID));
        final String language = envelope.payloadAsJsonObject().getString(LANGUAGE);
        final boolean isWelsh = WELSH.name().equalsIgnoreCase(language);
        try {
            LOGGER.info("generating press transparency PDF report for press report {}", reportId);
            final JsonObject payloadForDocumentGeneration = buildPayload(pendingCasesFromViewStore, false, envelope, isWelsh);
            requestDocumentGeneration(envelope, reportId, payloadForDocumentGeneration);
            storeReportMetadata(envelope, reportId, pendingCasesFromViewStore);
        } catch (FileServiceException e) {
            throw new RuntimeException("IO Exception happened during press transparency report generation", e);
        }
    }

    @Handles(PressTransparencyJSONReportRequested.EVENT_NAME)
    @Transactional
    @SuppressWarnings("squid:S00112")
    public void handlePressTransparencyJSONReportRequest(final JsonEnvelope envelope) {
        payloadHelper.initCache();

        final List<JsonObject> pendingCasesFromViewStore = getPendingCasesFromViewStore(envelope);
        final JsonObject eventPayload = envelope.payloadAsJsonObject();
        final UUID reportId = fromString(eventPayload.getString(PRESS_TRANSPARENCY_REPORT_ID));
        final boolean isWelsh = WELSH.name().equalsIgnoreCase(eventPayload.getString(LANGUAGE));
        LOGGER.info("generating press transparency JSON report for press report {}", reportId);
        sendPublicEvent(envelope, buildPayload(pendingCasesFromViewStore, true, envelope, isWelsh));
    }

    /**
     * @deprecated with CCT-1587 now we are using two separate events for PDF and JSON report
     * generation {@link PressTransparencyPDFReportRequested} and {@link
     * PressTransparencyJSONReportRequested}
     */
    @Deprecated(forRemoval = true)
    @Handles("sjp.events.press-transparency-report-requested")
    @Transactional
    @SuppressWarnings({"squid:S00112", "squid:S1133"})
    public void handlePressTransparencyRequest(final JsonEnvelope envelope) {
        payloadHelper.initCache();

        final List<JsonObject> pendingCasesFromViewStore = getPendingCasesFromViewStore(envelope);
        final JsonObject eventPayload = envelope.payloadAsJsonObject();
        final UUID reportId = fromString(eventPayload.getString(PRESS_TRANSPARENCY_REPORT_ID));
        try {
            final JsonObject payloadForDocumentGeneration = buildPayload(pendingCasesFromViewStore, false, envelope, false);
            requestDocumentGeneration(envelope, reportId, payloadForDocumentGeneration);
            sendPublicEvent(envelope, buildPayload(pendingCasesFromViewStore, true, envelope, false));
            storeReportMetadata(envelope, reportId, pendingCasesFromViewStore);
        } catch (FileServiceException e) {
            throw new RuntimeException("IO Exception happened during press transparency report generation", e);
        }
    }

    private void sendPublicEvent(final JsonEnvelope envelope, final JsonObject payloadForDocumentGeneration) {
        LOGGER.info("publishing public event for sjp pending cases for public list in english");
        final String type = envelope.payloadAsJsonObject().getString(REQUEST_TYPE);
        final String language = envelope.payloadAsJsonObject().getString(LANGUAGE);
        final JsonObjectBuilder pendingListEnglishBuilder = Json.createObjectBuilder()
                .add(LANGUAGE, language)
                .add(REQUEST_TYPE, type)
                .add("listPayload", payloadForDocumentGeneration);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("publishing Sjp public event for press report {}, {}", PUBLIC_SJP_PRESS_TRANSPARENCY_REPORT_GENERATED, payloadForDocumentGeneration);
        }
        sender.send(Envelope.envelopeFrom(metadataFrom(envelope.metadata())
                        .withName(PUBLIC_SJP_PRESS_TRANSPARENCY_REPORT_GENERATED),
                pendingListEnglishBuilder.build()));
    }

    private void storeReportMetadata(final JsonEnvelope envelope,
                                     final UUID reportId,
                                     final List<JsonObject> pendingCases) {

        String type = envelope.payloadAsJsonObject().getString(REQUEST_TYPE).toLowerCase();
        type = type.substring(0, 1).toUpperCase() + type.substring(1);
        String language = envelope.payloadAsJsonObject().getString(LANGUAGE).toLowerCase();
        language = language.substring(0, 1).toUpperCase() + language.substring(1);
        final String format = envelope.payloadAsJsonObject().getString("format");
        final JsonEnvelope envelopeToSend = envelopeFrom(
                metadataFrom(envelope.metadata()).withName("sjp.command.store-press-transparency-report-data"),
                createObjectBuilder()
                        .add(PRESS_TRANSPARENCY_REPORT_ID, reportId.toString())
                        .add("caseIds", jsonArrayWithCaseIds(pendingCases))
                        .add("format", format)
                        .add(REQUEST_TYPE, type.toUpperCase())
                        .add(LANGUAGE, language.toUpperCase())
                        .add(TITLE, getTitle(type, language))
                        .build());

        sender.send(envelopeToSend);
    }

    private String getTitle(final String type, final String language) {
        final String titleLanguage = " (" + language + ")";
        return type.equalsIgnoreCase(FULL.name()) ? "All Pending cases (Press version)" +
                titleLanguage : "New cases, since " + getDateTimeForDeltaReport().format(ENGLISH_TITLE_DATE_FORMATTER) + titleLanguage;
    }


    private JsonArrayBuilder jsonArrayWithCaseIds(final List<JsonObject> pendingCases) {
        final JsonArrayBuilder caseIdsBuilder = createArrayBuilder();
        pendingCases.stream().map(e -> e.getString("caseId")).forEach(caseIdsBuilder::add);
        return caseIdsBuilder;
    }

    private void requestDocumentGeneration(final JsonEnvelope envelope, final UUID reportId, final JsonObject payload) throws FileServiceException {
        final String payloadFileName = String.format("press-transparency-report-template-parameters.%s.json", reportId.toString());
        String type = envelope.payloadAsJsonObject().getString(REQUEST_TYPE).toLowerCase();
        type = type.substring(0, 1).toUpperCase() + type.substring(1);
        String language = envelope.payloadAsJsonObject().getString(LANGUAGE).toLowerCase();
        language = language.substring(0, 1).toUpperCase() + language.substring(1);
        final UUID payloadFileId = storeDocumentGeneratorPayload(payload, payloadFileName, type, language);
        sendDocumentGenerationRequest(envelope, reportId, payloadFileId, type, language);
    }

    @SuppressWarnings("squid:S2629")
    private UUID storeDocumentGeneratorPayload(final JsonObject documentGeneratorPayload, final String fileName, final String type, final String language) throws FileServiceException {
        final byte[] jsonPayloadInBytes = jsonObjectAsByteArray(documentGeneratorPayload);

        final JsonObject metadata = createObjectBuilder()
                .add("fileName", fileName)
                .add("conversionFormat", CONVERSION_FORMAT)
                .add("templateName", getTemplateIdentifier(type, language))
                .add("numberOfPages", 1)
                .add("fileSize", jsonPayloadInBytes.length)
                .build();
        return fileStorer.store(metadata, new ByteArrayInputStream(jsonPayloadInBytes));
    }

    private void sendDocumentGenerationRequest(final JsonEnvelope eventEnvelope,
                                               final UUID reportId,
                                               final UUID payloadFileServiceUUID, final String type, final String language) {

        final JsonObject docGeneratorPayload = createObjectBuilder()
                .add("originatingSource", "sjp")
                .add("templateIdentifier", getTemplateIdentifier(type, language))
                .add("conversionFormat", CONVERSION_FORMAT)
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

    private JsonObject buildPayload(final List<JsonObject> pendingCasesFromViewStore, final boolean isPayloadForPublicEvent, final JsonEnvelope envelope, final boolean isWelsh) {
        final JsonArray readyCases = createReadyCases(pendingCasesFromViewStore, isPayloadForPublicEvent, envelope, isWelsh).build();
        return createObjectBuilder()
                .add("generatedDateAndTime", isPayloadForPublicEvent ? formatPublicationDateTimeForJsonReport(now(), isWelsh) : formatDateTimeForPdfReport(now(), isWelsh))
                .add("totalNumberOfRecords", readyCases.size())
                .add("readyCases", readyCases)
                .add("startDate", payloadHelper.getStartDate(isWelsh))
                .build();
    }

    private JsonArrayBuilder createReadyCases(final List<JsonObject> pendingCases, final boolean isPayloadForPublicEvent,
                                              final JsonEnvelope envelope, final boolean isWelsh) {
        final JsonArrayBuilder readyCasesBuilder = createArrayBuilder();

        pendingCases.forEach(pendingCase -> {
            Optional<Long> defendantAge = empty();
            if (!isEmpty(pendingCase.getString(DEFENDANT_DATE_OF_BIRTH, EMPTY))) {
                defendantAge = getAge(pendingCase.getString(DEFENDANT_DATE_OF_BIRTH));
            }

            final Boolean defendantIsAdultOrUnknownAge = defendantAge.map(this::isAdult).orElse(true);
            if (defendantIsAdultOrUnknownAge) {
                final JsonObjectBuilder readyCaseItem = isPayloadForPublicEvent ? createReadyCaseForPublishEvent(envelope, pendingCase, isWelsh) : createReadyCase(envelope, pendingCase, isWelsh);
                readyCasesBuilder.add(readyCaseItem);
            }
        });
        return readyCasesBuilder;
    }

    private JsonObjectBuilder createReadyCase(final JsonEnvelope envelope, final JsonObject pendingCase, final boolean isWelsh) {
        final JsonObjectBuilder readyCase = createObjectBuilder()
                .add(CASE_URN, pendingCase.getString(CASE_URN))
                .add(DEFENDANT_NAME, payloadHelper.buildDefendantName(pendingCase))
                .add(ADDRESS, getDefendantAddress(pendingCase))
                .add(OFFENCES, createReadyCaseOffences(pendingCase.getJsonArray(OFFENCES), envelope, isWelsh))
                .add(PROSECUTOR_NAME, payloadHelper.buildProsecutorName(pendingCase.getString(PROSECUTOR_NAME), isWelsh, envelope));

        if (hasDefendantDateOfBirth(pendingCase)) {
            readyCase.add(DATE_OF_BIRTH, createDateOfBirthWithAge(pendingCase.getString(DEFENDANT_DATE_OF_BIRTH)));
        }

        return readyCase;
    }

    private JsonObjectBuilder createReadyCaseForPublishEvent(final JsonEnvelope envelope, final JsonObject pendingCase, final boolean isWelsh) {
        final JsonObjectBuilder readyCaseBuilder = createObjectBuilder();

        JsonObjects.getString(pendingCase, CASE_URN).ifPresent(urn -> readyCaseBuilder.add(CASE_URN, urn));
        JsonObjects.getString(pendingCase, TITLE).ifPresent(title -> readyCaseBuilder.add(TITLE, title));
        JsonObjects.getString(pendingCase, FIRST_NAME).ifPresent(firstName -> readyCaseBuilder.add(FIRST_NAME, firstName));
        JsonObjects.getString(pendingCase, LAST_NAME).ifPresent(lastName -> readyCaseBuilder.add(LAST_NAME, lastName));
        JsonObjects.getString(pendingCase, ADDRESS_LINE_1).ifPresent(line1 -> readyCaseBuilder.add(ADDRESS_LINE_1, line1));
        JsonObjects.getString(pendingCase, ADDRESS_LINE_2).ifPresent(line2 -> readyCaseBuilder.add(ADDRESS_LINE_2, line2));
        JsonObjects.getString(pendingCase, ADDRESS_LINE_3).ifPresent(line3 -> readyCaseBuilder.add(ADDRESS_LINE_3, line3));
        JsonObjects.getString(pendingCase, TOWN).ifPresent(town -> readyCaseBuilder.add(TOWN, town));
        JsonObjects.getString(pendingCase, COUNTY).ifPresent(country -> readyCaseBuilder.add(COUNTY, country));
        JsonObjects.getString(pendingCase, POSTCODE).ifPresent(postcode -> readyCaseBuilder.add(POSTCODE, postcode));
        JsonObjects.getString(pendingCase, LEGAL_ENTITY_NAME).ifPresent(legalEntity -> readyCaseBuilder.add(LEGAL_ENTITY_NAME, legalEntity));

        readyCaseBuilder.add(SJP_OFFENCES, createReadyCaseOffences(pendingCase.getJsonArray(OFFENCES), envelope, isWelsh))
                .add(PROSECUTOR_NAME, payloadHelper.buildProsecutorName(pendingCase.getString(PROSECUTOR_NAME), isWelsh, envelope));

        getPersonDefendantFullName(pendingCase).ifPresent(defendantName -> readyCaseBuilder.add(DEFENDANT_NAME, defendantName.trim()));

        if (hasDefendantDateOfBirth(pendingCase)) {
            readyCaseBuilder.add(DATE_OF_BIRTH, getDateOfBirth(pendingCase.getString(DEFENDANT_DATE_OF_BIRTH)));
            readyCaseBuilder.add(AGE, ((getAge(pendingCase.getString(DEFENDANT_DATE_OF_BIRTH))).orElse(0L)).toString());
        }

        return readyCaseBuilder;
    }

    private String createDateOfBirthWithAge(final String defendantDateOfBirth) {
        try {
            final LocalDate defendantDob = parse(defendantDateOfBirth);
            return format("%s (%d)", DATE_FORMAT.format(defendantDob), getAge(defendantDateOfBirth).orElse(0L));
        } catch (DateTimeParseException e) {
            LOGGER.warn("error parsing defendant date of birth " + defendantDateOfBirth, e);
            return EMPTY;
        }
    }

    private String getDateOfBirth(final String defendantDateOfBirth) {
        try {
            final LocalDate defendantDob = parse(defendantDateOfBirth);
            return format("%s", DOB_FORMAT.format(defendantDob));
        } catch (DateTimeParseException e) {
            LOGGER.warn("error parsing defendant date of birth " + defendantDateOfBirth, e);
            return EMPTY;
        }
    }

    private Optional<Long> getAge(final String defendantDobString) {
        try {
            final LocalDate defendantDob = parse(defendantDobString);
            return of(YEARS.between(defendantDob, LocalDate.now()));
        } catch (DateTimeParseException e) {
            LOGGER.warn("could not parse defendant dob " + defendantDobString, e);
            return empty();
        }
    }

    private boolean isAdult(final Long age) {
        return age >= ADULT_AGE;
    }

    private JsonArray createReadyCaseOffences(final JsonArray pendingCaseOffences, final JsonEnvelope envelope, final boolean isWelsh) {
        final JsonArrayBuilder readyCaseOffences = createArrayBuilder();
        pendingCaseOffences.getValuesAs(JsonObject.class).forEach(pendingCaseOffence -> {
            final JsonObject pendingCasePressRestriction = pendingCaseOffence.getJsonObject("pressRestriction");
            final boolean pressRestrictionRequested = pendingCasePressRestriction.getBoolean("requested");
            final String welshWording = pendingCaseOffence.containsKey(OFFENCE_WELSH_WORDING) ? pendingCaseOffence.getString(OFFENCE_WELSH_WORDING) : EMPTY;
            final String wording = isWelsh ? welshWording : pendingCaseOffence.getString(OFFENCE_WORDING);
            final JsonObjectBuilder readyCaseOffence = createObjectBuilder()
                    .add(TITLE, payloadHelper.mapOffenceIntoOffenceTitleString(pendingCaseOffence, isWelsh, envelope))
                    .add(WORDING, wording)
                    .add(PRESS_RESTRICTION_REQUESTED, pressRestrictionRequested);

            if (pressRestrictionRequested) {
                readyCaseOffence.add("pressRestrictionName", pendingCasePressRestriction.getString("name", EMPTY));
            }

            readyCaseOffences.add(readyCaseOffence);
        });
        return readyCaseOffences.build();
    }

    private String getDefendantAddress(final JsonObject pendingCase) {
        final String addressLine1 = pendingCase.containsKey(ADDRESS_LINE_1) ? pendingCase.getString(ADDRESS_LINE_1) : EMPTY;
        final String addressLine2 = pendingCase.containsKey(ADDRESS_LINE_2) ? format(STRING_FORMAT_COMMA_PREFIX, pendingCase.getString(ADDRESS_LINE_2)) : EMPTY;
        final String county = pendingCase.containsKey(COUNTY) ? format(STRING_FORMAT_COMMA_PREFIX, pendingCase.getString(COUNTY)) : EMPTY;
        final String town = pendingCase.containsKey(TOWN) ? format(STRING_FORMAT_COMMA_PREFIX, pendingCase.getString(TOWN)) : EMPTY;
        final String postcode = pendingCase.containsKey(POSTCODE) ? format(STRING_FORMAT, pendingCase.getString(POSTCODE)) : EMPTY;
        if (nonNull(postcode) && !postcode.isEmpty()) {
            return format("%s%s%s%s,%s", addressLine1, addressLine2, county, town, postcode).trim();
        } else {
            return format("%s%s%s%s", addressLine1, addressLine2, county, town).trim();

        }
    }

    private List<JsonObject> getPendingCasesFromViewStore(final JsonEnvelope envelope) {
        final ListType requestType = ListType.valueOf(envelope.payloadAsJsonObject().getString(REQUEST_TYPE));
        if (requestType.equals(ListType.FULL)) {
            return sjpService.getPendingCases(envelope, ExportType.PRESS);
        }
        return sjpService.getPendingDeltaCases(envelope, ExportType.PRESS);
    }

    private boolean hasDefendantDateOfBirth(final JsonObject pendingCase) {
        return pendingCase.containsKey(DEFENDANT_DATE_OF_BIRTH) && !isEmpty(pendingCase.getString(DEFENDANT_DATE_OF_BIRTH));
    }

    private Optional<String> getPersonDefendantFullName(final JsonObject pendingCase) {
        if( JsonObjects.getString(pendingCase, FIRST_NAME).isPresent() || JsonObjects.getString(pendingCase, LAST_NAME).isPresent() ) {
            return Optional.of((String) format("%s %s", pendingCase.getString(FIRST_NAME, ""), pendingCase.getString(LAST_NAME, "").toUpperCase()));
        }
        return Optional.empty();
    }
}
