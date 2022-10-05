package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonObject;
import javax.json.JsonString;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateCaseListedInCriminalCourtsHandler extends CaseCommandHandler {

    public static final String COMMAND_NAME = "sjp.command.update-case-listed-in-criminal-courts";
    public static final String HEARING_ID = "hearingId";
    public static final String DEFENDANT_ID = "defendantId";
    public static final String DEFENDANT_OFFENCES = "defendantOffences";
    public static final String COURT_CENTRE = "courtCentre";
    public static final String HEARING_DAYS = "hearingDays";
    public static final String HEARING_TYPE = "hearingType";

    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Handles(UpdateCaseListedInCriminalCourtsHandler.COMMAND_NAME)
    public void updateCaseListedInCriminalCourts(final JsonEnvelope updateCaseListedInCriminalCourtsCommand) throws EventStreamException {

        final JsonObject payload = updateCaseListedInCriminalCourtsCommand.payloadAsJsonObject();

        final UUID caseId = getCaseId(payload);
        final UUID hearingId = UUID.fromString(payload.getString(HEARING_ID));
        final UUID defendantId = UUID.fromString(payload.getString(DEFENDANT_ID));
        final List<UUID> defendantOffences = payload
                .getJsonArray(DEFENDANT_OFFENCES)
                .getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .map(UUID::fromString)
                .collect(Collectors.toList());

        final CourtCentre courtCentre = this.jsonObjectToObjectConverter.convert(payload.getJsonObject(COURT_CENTRE), CourtCentre.class);
        final List<HearingDay> hearingDays =
                payload
                        .getJsonArray(HEARING_DAYS)
                        .getValuesAs(JsonObject.class)
                        .stream()
                        .map(e -> this.jsonObjectToObjectConverter.convert(e, HearingDay.class))
                        .collect(Collectors.toList());

        final HearingType hearingType = this.jsonObjectToObjectConverter.convert(payload.getJsonObject(HEARING_TYPE), HearingType.class);

        applyToCaseAggregate(updateCaseListedInCriminalCourtsCommand,
                aCase -> aCase.updateCaseListedInCriminalCourts(caseId,
                        defendantId,
                        defendantOffences,
                        hearingId,
                        courtCentre,
                        hearingDays,
                        hearingType));
    }

}
