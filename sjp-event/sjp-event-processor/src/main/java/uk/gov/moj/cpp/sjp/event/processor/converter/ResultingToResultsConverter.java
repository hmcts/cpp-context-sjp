package uk.gov.moj.cpp.sjp.event.processor.converter;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.resulting.CourtDetails;
import uk.gov.moj.cpp.sjp.domain.resulting.Offence;
import uk.gov.moj.cpp.sjp.domain.resulting.ReferencedDecisionsSaved;
import uk.gov.moj.cpp.sjp.domain.resulting.Result;
import uk.gov.moj.cpp.sjp.domain.resulting.SJPSession;
import uk.gov.moj.cpp.sjp.domain.resulting.TerminalEntry;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class ResultingToResultsConverter {

    protected static final String ADDRESS1_KEY = "address1";
    protected static final String ADDRESS2_KEY = "address2";
    protected static final String ADDRESS3_KEY = "address3";
    protected static final String ADDRESS4_KEY = "address4";
    protected static final String ADDRESS5_KEY = "address5";
    protected static final String POSTCODE_KEY = "postcode";
    protected static final String ADDRESS_KEY = "address";
    protected static final String ROOM_NAME = "00";
    protected static final String DEFAULT_BAIL_STATUS = "A";
    public static final String DEFAULT_NON_POLICE_PROSECUTOR_REFERENCE = "0800NP0100000000001H";
    public static final int DEFAULT_OFFENCE_DATE_CODE = 1;

    @Inject
    private ReferenceDataService referenceDataService;

    public JsonObject convert(final UUID caseId, final JsonEnvelope envelope, final CaseDetails caseDetails, final JsonObject sjpSessionPayload) {

        final JsonObject jsonPayload = envelope.payloadAsJsonObject();
        final ReferencedDecisionsSaved referencedDecisionsSaved = extractReferenceDecisionSaves(caseId, jsonPayload);
        final SJPSession sjpSession = extractSJPSession(sjpSessionPayload);
        final JsonEnvelope emptyEnvelope = envelopeFrom(metadataFrom(envelope.metadata()), NULL);
        final Optional<JsonObject> court = referenceDataService.getCourtByCourtHouseOUCode(sjpSession.getCourtDetails().getCourtHouseCode(), emptyEnvelope);

        return createObjectBuilder()
                .add("session", buildSession(sjpSession, court)) // --required
                .add("cases", buildCases(caseId, caseDetails, referencedDecisionsSaved, sjpSession)) // --required
                .build();
    }

    protected JsonArray buildCases(final UUID caseId, final CaseDetails caseDetails, final ReferencedDecisionsSaved referencedDecisionsSaved, final SJPSession sjpSession) {
        return createArrayBuilder().add(createObjectBuilder()
                .add("caseId", caseId.toString()) // -- required
                .add("urn", caseDetails.getUrn()) // -- required
                .add("defendants", buildDefendants(caseDetails, referencedDecisionsSaved, sjpSession))).build(); // -- required
    }

    protected JsonObject buildSession(final SJPSession sjpSession, final Optional<JsonObject> court) {
        return createObjectBuilder()
                .add("sessionId", sjpSession.getId().toString()) // -- required
                .add("sessionLocation", buildSessionLocation(sjpSession, court)) // -- required
                .add("dateAndTimeOfSession", getZonedDateTimeString(sjpSession.getStartedAt())) // -- required
                .add("psaCode", sjpSession.getCourtDetails().getCourtHouseCode()).build(); // -- required
    }

    protected JsonObject buildSessionLocation(final SJPSession sjpSession, final Optional<JsonObject> courtOptional) {
        JsonObjectBuilder builder = createObjectBuilder();


        builder.add("courtId", courtOptional.isPresent() ? courtOptional.get().getString("id") : null) // -- required
                .add("courtHouseCode", sjpSession.getCourtDetails().getCourtHouseCode()) // -- required
                .add("name", sjpSession.getCourtDetails().getCourtHouseName()) // -- required
                .add("roomName", ROOM_NAME); // -- required. It was decided by architect to set it to 00



        if (courtOptional.isPresent()) {
            JsonObject court = courtOptional.get();
            builder.add(ADDRESS_KEY, createObjectBuilder()
                    .add(ADDRESS1_KEY, court.getString(ADDRESS1_KEY)) // -- required
                    .add(ADDRESS2_KEY, court.getString(ADDRESS2_KEY))
                    .add(ADDRESS3_KEY, court.getString(ADDRESS3_KEY))
                    .add(ADDRESS4_KEY, court.getString(ADDRESS4_KEY))
                    .add(ADDRESS5_KEY, court.getString(ADDRESS5_KEY))
                    .add(POSTCODE_KEY, !court.isNull(POSTCODE_KEY) ? court.getString(POSTCODE_KEY) : ""));
        }

        return builder.build();
    }

    protected JsonArray buildDefendants(final CaseDetails caseDetails, final ReferencedDecisionsSaved referencedDecisionsSaved, final SJPSession sjpSession) {
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        final Defendant defendant = caseDetails.getDefendant();
        arrayBuilder.add(createObjectBuilder()
                .add("defendantId", defendant.getId().toString()) // --required
                .add("prosecutorReference", DEFAULT_NON_POLICE_PROSECUTOR_REFERENCE) // --required
                .add("individualDefendant", buildIndividualDefendant(defendant))
                .add("offences", buildOffences(caseDetails, referencedDecisionsSaved, sjpSession))); // --required
        return arrayBuilder.build();
    }

    protected JsonObject buildIndividualDefendant(final Defendant defendant) {
        return createObjectBuilder()
                .add("basePersonDetails", buildPerson(defendant.getPersonalDetails())) // -- required
//                        .add("pncIdentifier", "")
//                        .add("personStatedNationality", "")
                .add("bailStatus", DEFAULT_BAIL_STATUS) // -- required
                .add("presentAtHearing", false) // -- required
                .build();
    }

    protected JsonObject buildPerson(final PersonalDetails personalDetails) {
        final JsonObjectBuilder person = createObjectBuilder();
        person.add("personTitle", personalDetails.getTitle())
                .add("firstName", personalDetails.getFirstName())
                .add("lastName", personalDetails.getLastName()) // --required
                .add(ADDRESS_KEY, buildAddress(personalDetails.getAddress()));
        if (null != personalDetails.getContactDetails()) {
            person.add("telephoneNumberBusiness", personalDetails.getContactDetails().getBusiness())
                    .add("telephoneNumberHome", personalDetails.getContactDetails().getHome())
                    .add("telephoneNumberMobile", personalDetails.getContactDetails().getMobile())
                    .add("emailAddress1", personalDetails.getContactDetails().getEmail())
                    .add("emailAddress2", personalDetails.getContactDetails().getEmail2());
        }

        if (null != personalDetails.getDateOfBirth()) {
            person.add("birthDate", LocalDates.to(personalDetails.getDateOfBirth()));
        }

        if (null != personalDetails.getGender()) {
            person.add("gender", personalDetails.getGender().toString());
        }

        return person.build();
    }

    protected JsonArray buildOffences(final CaseDetails caseDetails, final ReferencedDecisionsSaved referencedDecisionsSaved, final SJPSession sjpSession) {
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        if (null != caseDetails.getDefendant().getOffences()) {
            caseDetails.getDefendant().getOffences().forEach(o -> {
                final JsonObjectBuilder builder = createObjectBuilder();
                builder.add("baseOffenceDetails", buildBaseOffenceDetails(o)) // --required
                        .add("initiatedDate", o.getStartDate()) // --required
                        .add("plea", buildPlea(o))
//                    .add("modeOfTrial", "")
//                    .add("convictionDate", "")
                        .add("convictingCourt", sjpSession.getCourtDetails().getCourtHouseCode())
//                    .add("finding", "") do not know, check with business
                        .add("results", buildResults(o, referencedDecisionsSaved, sjpSession)); // --required
                arrayBuilder.add(builder);
            });
        }

        return arrayBuilder.build();
    }

    protected JsonObject buildBaseOffenceDetails(final uk.gov.justice.json.schemas.domains.sjp.queries.Offence o) {
        return createObjectBuilder()
                .add("offenceId", o.getId().toString()) // --required
                .add("offenceSequenceNumber", o.getOffenceSequenceNumber())
                .add("offenceCode", o.getCjsCode())
                .add("offenceWording", o.getWording())
                .add("offenceDateCode", DEFAULT_OFFENCE_DATE_CODE) // --required
                .add("offenceStartDateTime", o.getStartDate()) // --required
                .add("offenceEndDateTime", o.getEndDate())
                .add("chargeDate", o.getChargeDate())
//                .add("locationOfOffence", "")
                .add("alcoholLevelAmount", NULL)
                .add("alcoholLevelMethod", NULL)
                .add("vehicleCode", NULL)
                .add("vehicleRegistrationMark", NULL)
                .build();
    }

    protected JsonObject buildPlea(final uk.gov.justice.json.schemas.domains.sjp.queries.Offence o) {
        return createObjectBuilder()
                .add("pleaType", null != o.getPlea() ? o.getPlea().toString() : "")
                .add("pleaDate", getZonedDateTimeString(o.getPleaDate()))
                .add("pleaMethod", null != o.getPleaMethod() ? o.getPleaMethod().toString() : "").build();
    }

    protected JsonArray buildResults(final uk.gov.justice.json.schemas.domains.sjp.queries.Offence caseOffence, final ReferencedDecisionsSaved referencedDecisionsSaved, final SJPSession sjpSession) {

        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        if (null != referencedDecisionsSaved) {
            Optional<Offence> referenceDecisionSavedOffenceOptional = referencedDecisionsSaved.getOffences().stream().filter(o -> o.getId().equals(caseOffence.getId())).findFirst();
            if (referenceDecisionSavedOffenceOptional.isPresent()) {
                referenceDecisionSavedOffenceOptional.get().getResults().forEach(result -> {
                    final JsonObjectBuilder builder = createObjectBuilder();
                    builder.add("resultId", sjpSession.getId().toString()) // TODO --required
                            .add("resultCode", result.getCode())
                            .add("resultText", "") // TODO "REPLACE_THIS" --required
                            .add("resultCodeQualifier", "") // TODO "REPLACE_THIS"
                            .add("bailStatusOffence", "") // TODO "REPLACE_THIS"
                            .add("durationValue", "") // TODO "REPLACE_THIS"
                            .add("durationUnit", "") // TODO "REPLACE_THIS"
                            .add("secondaryDurationValue", "") // TODO "REPLACE_THIS"
                            .add("secondaryDurationUnit", "") // TODO "REPLACE_THIS"
                            .add("durationStartDate", "") // TODO "REPLACE_THIS"
                            .add("durationEndDate", ""); // TODO "REPLACE_THIS"
                    arrayBuilder.add(builder);
                });
            }
        }
        return arrayBuilder.build();
    }

    protected JsonObject buildAddress(final Address address) {
        return createObjectBuilder()
                .add(ADDRESS1_KEY, address.getAddress1()) // -- required
                .add(ADDRESS2_KEY, address.getAddress2())
                .add(ADDRESS3_KEY, address.getAddress3())
                .add(ADDRESS4_KEY, address.getAddress4())
                .add(ADDRESS5_KEY, address.getAddress5())
                .add(POSTCODE_KEY, address.getPostcode()).build();
    }

    protected ReferencedDecisionsSaved extractReferenceDecisionSaves(final UUID caseId, final JsonObject payload) {
        final UUID sjpSessionId = fromString(payload.getString("sjpSessionId"));
        final ZonedDateTime resultedOn = payload.getString("resultedOn").isEmpty() ? null : ZonedDateTimes.fromString(payload.getString("resultedOn"));
        final String verdict = payload.getString("verdict");
        final List<Offence> offences = extractOffences(payload.getJsonArray("offences"));
        final Integer accountDivisionCode = payload.isNull("accountDivisionCode") ? 0 : payload.getInt("accountDivisionCode");
        final Integer enforcingCourtCode = payload.isNull("enforcingCourtCode") ? 0 : payload.getInt("enforcingCourtCode");

        return new ReferencedDecisionsSaved(caseId, sjpSessionId, resultedOn, verdict, offences, accountDivisionCode, enforcingCourtCode);
    }

    private List<Offence> extractOffences(final JsonArray offencesPayload) {
        return offencesPayload.getValuesAs(JsonObject.class)
                .stream()
                .map(offence -> new Offence(extractUUID(offence, "id"), extractResults(offence.getJsonArray("results"))))
                .collect(toList());
    }

    private UUID extractUUID(final JsonObject object, final String key) {
        return object.getString(key).isEmpty() ? null : fromString(object.getString(key));
    }

    private List<Result> extractResults(final JsonArray results) {
        return results.getValuesAs(JsonObject.class)
                .stream()
                .map(result -> new Result(result.getString("code"), extractUUID(result, "resultTypeId"), extractTerminalEntries(result.getJsonArray("terminalEntries"))))
                .collect(toList());
    }

    private List<TerminalEntry> extractTerminalEntries(final JsonArray terminalEntries) {
        return terminalEntries.getValuesAs(JsonObject.class)
                .stream()
                .map(terminalEntry -> new TerminalEntry(terminalEntry.getInt("index"), terminalEntry.getString("value")))
                .collect(toList());
    }

    private String getZonedDateTimeString(final ZonedDateTime date) {
        if (null != date) {
            return ZonedDateTimes.toString(date);
        }
        return null;
    }

    private SJPSession extractSJPSession(final JsonObject sjpSessionPayload) {
        final UUID sjpSessionId = fromString(sjpSessionPayload.getString("id"));
        final UUID userId = extractUUID(sjpSessionPayload, "userId");
        final SessionType type = sjpSessionPayload.getString("type").isEmpty() ? null : SessionType.valueOf(sjpSessionPayload.getString("type"));

        final String courtHouseCode = sjpSessionPayload.getString("courtHouseCode");
        final String courtHouseName = sjpSessionPayload.getString("courtHouseName");
        final String localJusticeAreaNationalCourtCode = sjpSessionPayload.getString("localJusticeAreaNationalCourtCode");
        final String magistrate = sjpSessionPayload.getString("magistrate");
        final ZonedDateTime startedAt = sjpSessionPayload.getString("startedAt").isEmpty() ? null : ZonedDateTimes.fromString(sjpSessionPayload.getString("startedAt"));
        final ZonedDateTime endedAt = sjpSessionPayload.getString("endedAt").isEmpty() ? null : ZonedDateTimes.fromString(sjpSessionPayload.getString("endedAt"));
        final CourtDetails courtDetails = new CourtDetails(courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode);
        return new SJPSession(sjpSessionId, userId, type, courtDetails, magistrate, startedAt, endedAt);
    }
}
