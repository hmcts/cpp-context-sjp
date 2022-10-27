package uk.gov.moj.cpp.sjp.event.processor;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.time.LocalDate.parse;
import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
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
import uk.gov.moj.cpp.sjp.event.processor.exception.OffenceNotFoundException;
import uk.gov.moj.cpp.sjp.event.processor.service.ExportType;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyReportRequested;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.transaction.Transactional;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1067"})
@ServiceComponent(EVENT_PROCESSOR)
public class PressTransparencyReportRequestedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PressTransparencyReportRequestedProcessor.class);

    private static final String TEMPLATE_IDENTIFIER = "PressPendingCasesEnglish";

    private static final DateTimeFormatter DATE_FORMAT = ofPattern("dd MM yyyy");

    private static final int ADULT_AGE = 18;
    private static final String DEFENDANT_DATE_OF_BIRTH = "defendantDateOfBirth";
    public static final String PUBLIC_SJP_PRESS_TRANSPARENCY_REPORT_GENERATED = "public.sjp.press-transparency-report-generated";
    public static final String CASE_URN = "caseUrn";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String ADDRESS_LINE_1 = "addressLine1";
    public static final String ADDRESS_LINE_2 = "addressLine2";
    public static final String COUNTY = "county";
    public static final String TOWN = "town";
    public static final String POSTCODE = "postcode";
    public static final String DEFENDANT_NAME = "defendantName";
    public static final String OFFENCES = "offences";
    public static final String PROSECUTOR_NAME = "prosecutorName";
    public static final String EMPTY = "";
    public static final String STRING_FORMAT_COMMA = " %s,";
    public static final String STRING_FORMAT = " %s";
    public static final String SJP_OFFENCES = "sjpOffences";

    @Inject
    private FileStorer fileStorer;

    @Inject
    private SjpService sjpService;

    @Inject
    private ReferenceDataOffencesService referenceDataOffencesService;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender sender;

    private Table<String, String, JsonObject> offenceDataTable;

    private Map<String, String> prosecutorDataMap;

    @Handles(PressTransparencyReportRequested.EVENT_NAME)
    @Transactional
    @SuppressWarnings("squid:S00112")
    public void handlePressTransparencyRequest(final JsonEnvelope envelope) {
        initCache();

        final List<JsonObject> pendingCasesFromViewStore = getPendingCasesFromViewStore(envelope);
        final JsonObject eventPayload = envelope.payloadAsJsonObject();
        final UUID reportId = fromString(eventPayload.getString("pressTransparencyReportId"));
        try {
            final JsonObject payloadForDocumentGeneration = buildPayload(pendingCasesFromViewStore, false, envelope);
            requestDocumentGeneration(envelope, reportId, payloadForDocumentGeneration);
            sendPublicEvent(envelope, buildPayload(pendingCasesFromViewStore, true,  envelope));
            storeReportMetadata(envelope, reportId, pendingCasesFromViewStore);
        } catch (FileServiceException e) {
            throw new RuntimeException("IO Exception happened during press transparency report generation", e);
        }
    }

    private void sendPublicEvent(final JsonEnvelope envelope, final JsonObject payloadForDocumentGeneration) {
        LOGGER.info("publishing public event for sjp pending cases for public list in english");
        final JsonObjectBuilder pendingListEnglishBuilder = Json.createObjectBuilder()
                .add("language", "ENGLISH")
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

        final JsonEnvelope envelopeToSend = envelopeFrom(
                metadataFrom(envelope.metadata()).withName("sjp.command.store-press-transparency-report-data"),
                createObjectBuilder()
                        .add("pressTransparencyReportId", reportId.toString())
                        .add("caseIds", jsonArrayWithCaseIds(pendingCases))
                        .build());
        sender.send(envelopeToSend);
    }

    private JsonArrayBuilder jsonArrayWithCaseIds(final List<JsonObject> pendingCases) {
        final JsonArrayBuilder caseIdsBuilder = createArrayBuilder();
        pendingCases.stream().map(e -> e.getString("caseId")).forEach(caseIdsBuilder::add);
        return caseIdsBuilder;
    }

    private void requestDocumentGeneration(final JsonEnvelope envelope, final UUID reportId, final JsonObject payload) throws FileServiceException {
        final String payloadFileName = String.format("press-transparency-report-template-parameters.%s.json", reportId.toString());
        final UUID payloadFileId = storeDocumentGeneratorPayload(payload, payloadFileName);
        sendDocumentGenerationRequest(envelope, reportId, payloadFileId);
    }

    private UUID storeDocumentGeneratorPayload(final JsonObject documentGeneratorPayload, final String fileName) throws FileServiceException {
        final byte[] jsonPayloadInBytes = jsonObjectAsByteArray(documentGeneratorPayload);

        final JsonObject metadata = createObjectBuilder()
                .add("fileName", fileName)
                .add("conversionFormat", "pdf")
                .add("templateName", TEMPLATE_IDENTIFIER)
                .add("numberOfPages", 1)
                .add("fileSize", jsonPayloadInBytes.length)
                .build();
        return fileStorer.store(metadata, new ByteArrayInputStream(jsonPayloadInBytes));
    }

    private void sendDocumentGenerationRequest(final JsonEnvelope eventEnvelope,
                                               final UUID reportId,
                                               final UUID payloadFileServiceUUID) {

        final JsonObject docGeneratorPayload = createObjectBuilder()
                .add("originatingSource", "sjp")
                .add("templateIdentifier", TEMPLATE_IDENTIFIER)
                .add("conversionFormat", "pdf")
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

    private void initCache() {
        offenceDataTable = HashBasedTable.create();
        prosecutorDataMap = new HashMap<>();
    }

    private JsonObject buildPayload(final List<JsonObject> pendingCasesFromViewStore, final boolean isPayloadForPublicEvent, final JsonEnvelope envelope) {
        final JsonArray readyCases = createReadyCases(pendingCasesFromViewStore, isPayloadForPublicEvent, envelope).build();
        return createObjectBuilder()
                .add("generatedDateAndTime", formatDateTimeForReport(now(), false))
                .add("totalNumberOfRecords", readyCases.size())
                .add("readyCases", readyCases)
                .build();
    }


    private JsonArrayBuilder createReadyCases(final List<JsonObject> pendingCases, final boolean isPayloadForPublicEvent,
                                              final JsonEnvelope envelope) {
        final JsonArrayBuilder readyCasesBuilder = createArrayBuilder();

        pendingCases.forEach(pendingCase -> {
            Optional<Long> defendantAge = empty();
            if (!isEmpty(pendingCase.getString(DEFENDANT_DATE_OF_BIRTH, EMPTY))) {
                defendantAge = getAge(pendingCase.getString(DEFENDANT_DATE_OF_BIRTH));
            }

            final Boolean defendantIsAdultOrUnknownAge = defendantAge.map(this::isAdult).orElse(true);
            if(defendantIsAdultOrUnknownAge) {
                final JsonObjectBuilder readyCaseItem = isPayloadForPublicEvent? createReadyCaseForPublishEvent(envelope, pendingCase):createReadyCase(envelope, pendingCase);
                readyCasesBuilder.add(readyCaseItem);
            }
        });
        return readyCasesBuilder;
    }

    private JsonObjectBuilder createReadyCase(final JsonEnvelope envelope, final JsonObject pendingCase) {
        final JsonObjectBuilder readyCase =  createObjectBuilder()
                .add(CASE_URN, pendingCase.getString(CASE_URN))
                .add(DEFENDANT_NAME, buildDefendantName(pendingCase))
                .add("address", getDefendantAddress(pendingCase))
                .add(OFFENCES, createReadyCaseOffences(pendingCase.getJsonArray(OFFENCES), envelope))
                .add(PROSECUTOR_NAME, buildProsecutorName(pendingCase.getString(PROSECUTOR_NAME), envelope));

        if (hasDefendantDateOfBirth(pendingCase)) {
            readyCase.add("dateOfBirth", createDateOfBirth(pendingCase.getString(DEFENDANT_DATE_OF_BIRTH)));
        }

        return readyCase;
    }

    private JsonObjectBuilder createReadyCaseForPublishEvent(final JsonEnvelope envelope, final JsonObject pendingCase) {
        final JsonObjectBuilder readyCase =  createObjectBuilder()
                .add(CASE_URN, pendingCase.containsKey(CASE_URN) ? pendingCase.getString(CASE_URN):EMPTY)
                .add(FIRST_NAME, pendingCase.containsKey(FIRST_NAME) ?pendingCase.getString(FIRST_NAME):EMPTY)
                .add(LAST_NAME, pendingCase.containsKey(LAST_NAME) ?pendingCase.getString(LAST_NAME):EMPTY)
                .add(ADDRESS_LINE_1, pendingCase.containsKey(ADDRESS_LINE_1) ? format("%s,", pendingCase.getString(ADDRESS_LINE_1)) : EMPTY)
                .add(ADDRESS_LINE_2, pendingCase.containsKey(ADDRESS_LINE_2) ? format("%s,", pendingCase.getString(ADDRESS_LINE_2)) : EMPTY)
                .add(COUNTY, pendingCase.containsKey(COUNTY) ? format("%s,", pendingCase.getString(COUNTY)) : EMPTY)
                .add(TOWN, pendingCase.containsKey(TOWN) ? format("%s,", pendingCase.getString(TOWN)) : EMPTY)
                .add(POSTCODE, pendingCase.containsKey(POSTCODE) ? format("%s,", pendingCase.getString(POSTCODE)) : EMPTY)
                .add(DEFENDANT_NAME, buildDefendantName(pendingCase))
                .add(SJP_OFFENCES, createReadyCaseOffences(pendingCase.getJsonArray(OFFENCES), envelope))
                .add(PROSECUTOR_NAME, buildProsecutorName(pendingCase.getString(PROSECUTOR_NAME), envelope));

        if (hasDefendantDateOfBirth(pendingCase)) {
            readyCase.add("dateOfBirth", createDateOfBirth(pendingCase.getString(DEFENDANT_DATE_OF_BIRTH)));
        }

        return readyCase;
    }

    private String buildDefendantName(final JsonObject pendingCase) {
        if (pendingCase.containsKey("legalEntityName")) {
            return pendingCase.getString("legalEntityName").toUpperCase();
        } else {
            return format("%s %s", pendingCase.getString("firstName", ""), pendingCase.getString("lastName", "").toUpperCase());
        }
    }

    private String createDateOfBirth(final String defendantDateOfBirth) {
        try {
            final LocalDate defendantDob = parse(defendantDateOfBirth);
            return format("%s (%d)", DATE_FORMAT.format(defendantDob), getAge(defendantDateOfBirth).orElse(0l));
        } catch (DateTimeParseException e) {
            LOGGER.warn("error parsing defendant date of birth " +  defendantDateOfBirth, e);
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

    private JsonArray createReadyCaseOffences(final JsonArray pendingCaseOffences, final JsonEnvelope envelope) {
        final JsonArrayBuilder readyCaseOffences = createArrayBuilder();
        pendingCaseOffences.getValuesAs(JsonObject.class).forEach(pendingCaseOffence -> {
            final JsonObject pendingCasePressRestriction = pendingCaseOffence.getJsonObject("pressRestriction");
            final boolean pressRestrictionRequested = pendingCasePressRestriction.getBoolean("requested");

            final JsonObjectBuilder readyCaseOffence = createObjectBuilder()
                    .add("title", mapOffenceIntoOffenceTitleString(pendingCaseOffence, envelope))
                    .add("wording", pendingCaseOffence.getString("offenceWording"))
                    .add("pressRestrictionRequested", pressRestrictionRequested);

            if(pressRestrictionRequested) {
                readyCaseOffence.add("pressRestrictionName", pendingCasePressRestriction.getString("name", EMPTY));
            }

            readyCaseOffences.add(readyCaseOffence);
        });
        return readyCaseOffences.build();
    }

    private String mapOffenceIntoOffenceTitleString(final JsonObject offence, final JsonEnvelope envelope) {
        final String offenceCode = offence.getString("offenceCode");
        final String offenceStartDate = offence.getString("offenceStartDate");

        final JsonObject offenceReferenceData;
        if (!offenceDataTable.contains(offenceCode, offenceStartDate)) {
            offenceReferenceData = referenceDataOffencesService
                    .getOffenceReferenceData(envelope, offenceCode, offenceStartDate)
                    .orElseThrow(() -> new OffenceNotFoundException(
                            format("offence reference data not founf for code %s", offenceCode))
                    );
            offenceDataTable.put(offenceCode, offenceStartDate, offenceReferenceData);
        } else {
            offenceReferenceData = offenceDataTable.get(offenceCode, offenceStartDate);
        }

        return offenceReferenceData.getString("title");
    }

    private String buildProsecutorName(final String prosecutorName, final JsonEnvelope envelope) {
        final String prosecutor;
        if (!prosecutorDataMap.containsKey(prosecutorName)) {
            prosecutor = referenceDataService.getProsecutor(prosecutorName, false, envelope);
            prosecutorDataMap.put(prosecutorName, prosecutor);
        } else {
            prosecutor = prosecutorDataMap.get(prosecutorName);
        }

        return prosecutor;
    }

    private String getDefendantAddress(final JsonObject pendingCase) {
        final String addressLine1 = pendingCase.containsKey(ADDRESS_LINE_1) ? format(STRING_FORMAT_COMMA, pendingCase.getString(ADDRESS_LINE_1)) : EMPTY;
        final String addressLine2 = pendingCase.containsKey(ADDRESS_LINE_2) ? format(STRING_FORMAT_COMMA, pendingCase.getString(ADDRESS_LINE_2)) : EMPTY;
        final String county = pendingCase.containsKey(COUNTY) ? format(STRING_FORMAT_COMMA, pendingCase.getString(COUNTY)) : EMPTY;
        final String town = pendingCase.containsKey(TOWN) ? format(STRING_FORMAT, pendingCase.getString(TOWN)) : EMPTY;
        final String postcode = pendingCase.containsKey(POSTCODE) ? format(STRING_FORMAT, pendingCase.getString(POSTCODE)) : EMPTY;
        if (nonNull(postcode) && ! postcode.isEmpty()) {
            return format("%s%s%s%s,%s", addressLine1, addressLine2, county, town, postcode).trim();
        } else {
            return format("%s%s%s%s", addressLine1, addressLine2, county, town).trim();

        }
    }

    private List<JsonObject> getPendingCasesFromViewStore(final JsonEnvelope envelope) {
        return sjpService.getPendingCases(envelope, ExportType.PRESS);
    }

    private boolean hasDefendantDateOfBirth(final JsonObject pendingCase) {
        return pendingCase.containsKey(DEFENDANT_DATE_OF_BIRTH) && !isEmpty(pendingCase.getString(DEFENDANT_DATE_OF_BIRTH));
    }
}
