package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static java.util.Objects.isNull;
import static java.util.Optional.of;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.ConverterUtils.extractUUID;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class CourtCentreConverter {

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private SjpService sjpService;

    @Inject
    private LJADetailsConverter ljaDetailsConverter;

    @Inject
    private AddressConverter addressConverter;

    private static final String ID = "id";
    private static final String OUCODE = "oucode";
    private static final String IS_WELSH = "isWelsh";
    private static final String OUCODE_L3_NAME_KEY = "oucodeL3Name";
    private static final String OUCODE_L3_WELSH_NAME_KEY = "oucodeL3WelshName";

    public CourtCentre convert(final UUID sjpSessionId, final Metadata sourceMetadata) {

        final JsonEnvelope emptyEnvelope = envelopeFrom(metadataFrom(sourceMetadata), NULL);
        final JsonObject sjpSessionPayload = sjpService.getSessionDetails(sjpSessionId, emptyEnvelope);
        final CourtCentre.Builder courtCentreBuilder = CourtCentre.courtCentre();
        if (sjpSessionPayload != null) {
            final String courtHouseCode = sjpSessionPayload.getString("courtHouseCode", null);
            final Optional<JsonObject> courtOptional = referenceDataService.getCourtByCourtHouseOUCode(courtHouseCode, emptyEnvelope);
            if (courtOptional.isPresent()) {
                populateCourtCenter(sjpSessionPayload, courtCentreBuilder, courtOptional);
            }
        }
        return courtCentreBuilder.build();
    }

    public Optional<CourtCentre> convertByOffenceId(final UUID offenceId, final Metadata sourceMetadata) {

        final JsonEnvelope emptyEnvelope = envelopeFrom(metadataFrom(sourceMetadata), NULL);
        final Optional<JsonObject> sjpSessionPayloadOptional = sjpService.getConvictingCourtSessionDetails(offenceId, emptyEnvelope);
        final CourtCentre.Builder courtCentreBuilder = CourtCentre.courtCentre();
        sjpSessionPayloadOptional.ifPresent(sjpSessionPayload -> {
            final String courtHouseCode = sjpSessionPayload.getString("courtHouseCode", null);
            final Optional<JsonObject> courtOptional = referenceDataService.getCourtByCourtHouseOUCode(courtHouseCode, emptyEnvelope);
            if (courtOptional.isPresent()) {
                populateCourtCenter(sjpSessionPayload, courtCentreBuilder, courtOptional);
            }
        });
        final CourtCentre courtCentre = courtCentreBuilder.build();
        return isNull(courtCentre.getId()) ? Optional.empty() : of(courtCentre);
    }

    private void populateCourtCenter(final JsonObject sjpSessionPayload,
                                     final CourtCentre.Builder courtCentreBuilder,
                                     final Optional<JsonObject> courtOptional) {
        if (courtOptional.isPresent()) {
            final JsonObject court = courtOptional.get();
            courtCentreBuilder
                    .withId(extractUUID(court, ID))
                    .withName(court.getString(OUCODE_L3_NAME_KEY, null))
                    .withCode(court.getString(OUCODE, null))
                    .withLja(ljaDetailsConverter.convert(sjpSessionPayload, courtOptional))
                    .withAddress(addressConverter.convert(courtOptional));

            if (court.getBoolean(IS_WELSH, false)) {
                courtCentreBuilder
                        .withWelshName(court.getString(OUCODE_L3_WELSH_NAME_KEY, null))
                        .withWelshCourtCentre(court.getBoolean(IS_WELSH, false))
                        .withWelshAddress(addressConverter.convertWelsh(courtOptional));

            }
        }
    }

}
