package uk.gov.moj.cpp.sjp.event.processor;

import static java.lang.String.format;
import static java.time.LocalDate.parse;
import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.time.temporal.ChronoUnit.YEARS;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
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
public class PressTransparencyReportRequestedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PressTransparencyReportRequestedProcessor.class);

    private static final String TEMPLATE_IDENTIFIER = "PressPendingCasesEnglish";

    private static final DateTimeFormatter DATE_FORMAT = ofPattern("dd MM yyyy");

    private static final int ADULT_AGE = 18;

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
            final JsonObject payloadForDocumentGeneration = buildPayloadForDocumentGeneration(pendingCasesFromViewStore, envelope);
            requestDocumentGeneration(envelope, reportId, payloadForDocumentGeneration);

            storeReportMetadata(envelope, reportId, pendingCasesFromViewStore);
        } catch (FileServiceException e) {
            throw new RuntimeException("IO Exception happened during press transparency report generation", e);
        }
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

    private JsonObject buildPayloadForDocumentGeneration(final List<JsonObject> pendingCasesFromViewStore, final JsonEnvelope envelope) {
        final JsonArray readyCases = createReadyCases(pendingCasesFromViewStore, envelope).build();
        return createObjectBuilder()
                .add("generatedDateAndTime", formatDateTimeForReport(now(), false))
                .add("totalNumberOfRecords", readyCases.size())
                .add("readyCases", readyCases)
                .build();
    }

    private JsonArrayBuilder createReadyCases(final List<JsonObject> pendingCases,
                                              final JsonEnvelope envelope) {
        final JsonArrayBuilder readyCasesBuilder = createArrayBuilder();
        pendingCases.forEach(pendingCase -> {
            final Optional<Long> defendantAge = getAge(pendingCase.getString("defendantDateOfBirth",""));
            final Boolean defendantIsAdultOrUnknownAge = defendantAge.map(this::isAdult).orElse(true);
            if(defendantIsAdultOrUnknownAge) {
                final JsonObjectBuilder readyCaseItem = createReadyCase(envelope, pendingCase);


                readyCasesBuilder.add(readyCaseItem);
            }
        });
        return readyCasesBuilder;
    }

    private JsonObjectBuilder createReadyCase(final JsonEnvelope envelope, final JsonObject pendingCase) {
        final JsonObjectBuilder readyCase =  createObjectBuilder()
                .add("caseUrn", pendingCase.getString("caseUrn"))
                .add("defendantName", buildDefendantName(pendingCase))
                .add("address", getDefendantAddress(pendingCase))
                .add("offences", createReadyCaseOffences(pendingCase.getJsonArray("offences"), envelope))
                .add("prosecutorName", buildProsecutorName(pendingCase.getString("prosecutorName"), envelope));

        if (pendingCase.containsKey("defendantDateOfBirth")) {
            readyCase.add("dateOfBirth", createDateOfBirth(pendingCase.getString("defendantDateOfBirth")));
        }

        return readyCase;
    }

    private String buildDefendantName(final JsonObject pendingCase) {
        return format("%s %s", pendingCase.getString("firstName", ""), pendingCase.getString("lastName","").toUpperCase());
    }

    private String createDateOfBirth(final String defendantDateOfBirth) {
        try {
            final LocalDate defendantDob = parse(defendantDateOfBirth);
            return format("%s (%d)", DATE_FORMAT.format(defendantDob), getAge(defendantDateOfBirth).orElse(0l));
        } catch (DateTimeParseException e) {
            LOGGER.warn("error parsing defendant date of birth " +  defendantDateOfBirth, e);
            return "";
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
            final JsonObject readyCaseOffence = createObjectBuilder()
                    .add("title", mapOffenceIntoOffenceTitleString(pendingCaseOffence,envelope))
                    .add("wording", pendingCaseOffence.getString("offenceWording"))
                    .build();
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
        final String addressLine1 = pendingCase.containsKey("addressLine1") ? format("%s,", pendingCase.getString("addressLine1")) : "";
        final String addressLine2 = pendingCase.containsKey("addressLine2") ? format(" %s,", pendingCase.getString("addressLine2")) : "";
        final String county = pendingCase.containsKey("county") ? format(" %s,", pendingCase.getString("county")) : "";
        final String town = pendingCase.containsKey("town") ? format(" %s,", pendingCase.getString("town")) : "";
        final String postcode = pendingCase.containsKey("postcode") ? format(" %s", pendingCase.getString("postcode")) : "";
        return format("%s%s%s%s%s", addressLine1, addressLine2, county, town, postcode).trim();
    }

    private List<JsonObject> getPendingCasesFromViewStore(final JsonEnvelope envelope) {
        return sjpService.getPendingCases(envelope);
    }
}
