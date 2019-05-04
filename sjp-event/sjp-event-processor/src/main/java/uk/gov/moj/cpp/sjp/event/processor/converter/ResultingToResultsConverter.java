package uk.gov.moj.cpp.sjp.event.processor.converter;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.resulting.CourtDetails;
import uk.gov.moj.cpp.sjp.domain.resulting.Offence;
import uk.gov.moj.cpp.sjp.domain.resulting.Prompt;
import uk.gov.moj.cpp.sjp.domain.resulting.ReferencedDecisionsSaved;
import uk.gov.moj.cpp.sjp.domain.resulting.Result;
import uk.gov.moj.cpp.sjp.domain.resulting.SJPSession;
import uk.gov.moj.cpp.sjp.event.processor.service.ProsecutionCaseFileService;
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

    private static final String DEFAULT_NON_POLICE_PROSECUTOR_REFERENCE = "0800NP0100000000001H";
    private static final int DEFAULT_OFFENCE_DATE_CODE = 1;
    private static final String UNKNOWN = "0";
    private static final String OFFENCES_KEY = "offences";
    private static final String ADDRESS1_KEY = "address1";
    private static final String ADDRESS2_KEY = "address2";
    private static final String ADDRESS3_KEY = "address3";
    private static final String ADDRESS4_KEY = "address4";
    private static final String ADDRESS5_KEY = "address5";
    private static final String POSTCODE_KEY = "postcode";
    private static final String ADDRESS_KEY = "address";
    private static final String ROOM_NAME = "00";
    private static final String DEFAULT_BAIL_STATUS = "A";
    private static final String VERDICT_KEY = "verdict";
    private static final String ACCOUNT_DIVISION_CODE_KEY = "accountDivisionCode";
    private static final String ENFORCING_COURT_CODE_KEY = "enforcingCourtCode";
    private static final String MAGISTRATE_KEY = "magistrate";
    private static final String ENDED_AT_KEY = "endedAt";

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ProsecutionCaseFileService prosecutionCaseFileService;

    public JsonObject convert(final UUID caseId, final JsonEnvelope envelope, final CaseDetails caseDetails, final JsonObject sjpSessionPayload) {

        final JsonObject jsonPayload = envelope.payloadAsJsonObject();
        final ReferencedDecisionsSaved referencedDecisionsSaved = extractReferenceDecisionSaves(caseId, jsonPayload);
        final SJPSession sjpSession = extractSJPSession(sjpSessionPayload);
        final JsonEnvelope emptyEnvelope = envelopeFrom(metadataFrom(envelope.metadata()), NULL);
        final Optional<JsonObject> court = referenceDataService.getCourtByCourtHouseOUCode(sjpSession.getCourtDetails().getCourtHouseCode(), emptyEnvelope);

        return createObjectBuilder()
                .add("session", buildSession(sjpSession, court)) // --required
                .add("cases", buildCases(caseId, caseDetails, referencedDecisionsSaved, sjpSession, envelope)) // --required
                .build();
    }

    protected JsonArray buildCases(final UUID caseId, final CaseDetails caseDetails, final ReferencedDecisionsSaved referencedDecisionsSaved, final SJPSession sjpSession, final JsonEnvelope envelope) {
        return createArrayBuilder().add(createObjectBuilder()
                .add("caseId", caseId.toString()) // -- required
                .add("urn", caseDetails.getUrn()) // -- required
                .add("defendants", buildDefendants(caseDetails, referencedDecisionsSaved, sjpSession, envelope))).build(); // -- required
    }

    protected JsonObject buildSession(final SJPSession sjpSession, final Optional<JsonObject> court) {
        return createObjectBuilder()
                .add("sessionId", sjpSession.getId().toString()) // -- required
                .add("sessionLocation", buildSessionLocation(sjpSession, court)) // -- required
                .add("dateAndTimeOfSession", getZonedDateTimeString(sjpSession.getStartedAt())) // -- required
                .add("psaCode", sjpSession.getCourtDetails().getCourtHouseCode()).build(); // -- required
    }

    protected JsonObject buildSessionLocation(final SJPSession sjpSession, final Optional<JsonObject> courtOptional) {
        final JsonObjectBuilder builder = createObjectBuilder();
        builder.add("courtId", courtOptional.isPresent() ? courtOptional.get().getString("id") : null) // -- required
                .add("courtHouseCode", sjpSession.getCourtDetails().getCourtHouseCode()) // -- required
                .add("name", sjpSession.getCourtDetails().getCourtHouseName()) // -- required
                .add("roomName", ROOM_NAME); // -- required. It was decided by architect to set it to 00

        if (courtOptional.isPresent()) {
            final JsonObject court = courtOptional.get();
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

    protected JsonArray buildDefendants(final CaseDetails caseDetails, final ReferencedDecisionsSaved referencedDecisionsSaved, final SJPSession sjpSession, final JsonEnvelope envelope) {
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        final Defendant defendant = caseDetails.getDefendant();
        if (null != defendant) {
            final JsonEnvelope emptyEnvelope = envelopeFrom(metadataFrom(envelope.metadata()), NULL);
            final JsonObject caseFileDefendantDetails = prosecutionCaseFileService.getCaseFileDefendantDetails(caseDetails.getId(), emptyEnvelope).orElse(null); // this service returns first defendant as in the SJP there is only 1 defendant.
            final Optional<JsonObject> defendantSelfDefinedInformationOptional = Optional.ofNullable(caseFileDefendantDetails)
                    .map(defendantDetails -> (JsonObject) defendantDetails.getOrDefault("selfDefinedInformation", createObjectBuilder().build()));
            final Optional<JsonArray> caseFileDefendantOffencesOptional = Optional.ofNullable(caseFileDefendantDetails)
                    .map(defendantDetails -> (JsonArray) defendantDetails.getOrDefault(OFFENCES_KEY, createArrayBuilder().build()));
            final String countryCJSCode = defendantSelfDefinedInformationOptional
                    .map(selfDefinedInformation -> selfDefinedInformation.getString("nationality", null))
                    .flatMap(selfDefinedNationality -> referenceDataService.getNationality(selfDefinedNationality, emptyEnvelope))
                    .map(referenceDataNationality -> referenceDataNationality.getString("cjsCode")) // cjs country code
                    .orElse(UNKNOWN);

            arrayBuilder.add(createObjectBuilder()
                    .add("defendantId", defendant.getId().toString()) // --required
                    .add("prosecutorReference", DEFAULT_NON_POLICE_PROSECUTOR_REFERENCE) // --required
                    .add("individualDefendant", buildIndividualDefendant(defendant, countryCJSCode))
                    .add(OFFENCES_KEY, buildOffences(caseDetails, referencedDecisionsSaved, sjpSession, caseFileDefendantOffencesOptional))); // --required
        }

        return arrayBuilder.build();
    }

    protected JsonObject buildIndividualDefendant(final Defendant defendant, final String countryCJSCode) {
        final JsonObjectBuilder objectBuilder = createObjectBuilder();
        PersonalDetails personalDetails = defendant.getPersonalDetails();

        if (null != personalDetails) {
            objectBuilder.add("basePersonDetails", buildPerson(defendant.getPersonalDetails())); // -- required
        }

//                        .add("pncIdentifier", "")
        objectBuilder.add("personStatedNationality", countryCJSCode)
                .add("bailStatus", DEFAULT_BAIL_STATUS) // -- required
                .add("presentAtHearing", false); // -- required
        return objectBuilder.build();
    }

    protected JsonObject buildPerson(final PersonalDetails personalDetails) {
        final JsonObjectBuilder person = createObjectBuilder();
        person.add("personTitle", personalDetails.getTitle())
                .add("firstName", personalDetails.getFirstName())
                .add("lastName", personalDetails.getLastName()) // --required
                .add(ADDRESS_KEY, buildAddress(personalDetails.getAddress()));
        ContactDetails contactDetails = personalDetails.getContactDetails();
        if (null != contactDetails) {
            extractAndAddDetails(contactDetails.getBusiness(), "telephoneNumberBusiness", person);
            extractAndAddDetails(contactDetails.getHome(), "telephoneNumberHome", person);
            extractAndAddDetails(contactDetails.getMobile(), "telephoneNumberMobile", person);
            extractAndAddDetails(contactDetails.getEmail(), "emailAddress1", person);
            extractAndAddDetails(contactDetails.getEmail2(), "emailAddress2", person);
        }

        if (null != personalDetails.getDateOfBirth()) {
            person.add("birthDate", LocalDates.to(personalDetails.getDateOfBirth()));
        }

        if (null != personalDetails.getGender()) {
            person.add("gender", personalDetails.getGender().toString());
        }

        return person.build();
    }

    protected JsonArray buildOffences(final CaseDetails caseDetails, final ReferencedDecisionsSaved referencedDecisionsSaved, final SJPSession sjpSession, final Optional<JsonArray> caseFileDefendantOffencesOptional) {
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        final JsonArray caseFileDefendantOffences = caseFileDefendantOffencesOptional.isPresent() ? caseFileDefendantOffencesOptional.get() : createArrayBuilder().build();

        if (null != caseDetails.getDefendant().getOffences()) {
            caseDetails.getDefendant().getOffences().forEach(o -> {
                final JsonObject caseFileDefendantOffence = caseFileDefendantOffences.stream().map(cfdo -> (JsonObject) cfdo).filter(cfdo -> cfdo.getString("id").equalsIgnoreCase(o.getId().toString())).findFirst().orElse(createObjectBuilder().build());
                final JsonObjectBuilder builder = createObjectBuilder();
                builder.add("baseOffenceDetails", buildBaseOffenceDetails(o, caseFileDefendantOffence)) // --required
                        .add("initiatedDate", o.getStartDate()) // --required
                        .add("plea", buildPlea(o))
//                    .add("modeOfTrial", "")
//                    .add("convictionDate", "")
                        .add("convictingCourt", sjpSession.getCourtDetails().getCourtHouseCode())
//                    .add("finding", "") do not know, check with business
                        .add("results", buildResults(o, referencedDecisionsSaved)); // --required
                arrayBuilder.add(builder);
            });
        }

        return arrayBuilder.build();
    }

    protected JsonObject buildBaseOffenceDetails(final uk.gov.justice.json.schemas.domains.sjp.queries.Offence o, final JsonObject caseFileDefendantOffence) {
        final JsonObjectBuilder baseOffenceDetails = createObjectBuilder();
        baseOffenceDetails.add("offenceId", o.getId().toString()) // --required
                .add("offenceSequenceNumber", o.getOffenceSequenceNumber())
                .add("offenceCode", o.getCjsCode())
                .add("offenceWording", o.getWording())
                .add("offenceDateCode", DEFAULT_OFFENCE_DATE_CODE) // --required
                .add("offenceStartDateTime", o.getStartDate()); // --required

        if (null != o.getEndDate()) {
            baseOffenceDetails.add("offenceEndDateTime", o.getEndDate());
        }

        if (null != o.getChargeDate()) {
            baseOffenceDetails.add("chargeDate", o.getChargeDate());
        }

        if (null != caseFileDefendantOffence && !caseFileDefendantOffence.isEmpty()) {
            baseOffenceDetails.add("locationOfOffence", caseFileDefendantOffence.getJsonString("offenceLocation"));
        }

        return baseOffenceDetails.build();
    }

    protected JsonObject buildPlea(final uk.gov.justice.json.schemas.domains.sjp.queries.Offence o) {
        final JsonObjectBuilder builder = createObjectBuilder();

        if (null != o.getPlea()) {
            builder.add("pleaType", o.getPlea().toString());
        }

        if (null != o.getPleaDate()) {
            builder.add("pleaDate", getZonedDateTimeString(o.getPleaDate()));
        }

        if (null != o.getPleaMethod()) {
            builder.add("pleaMethod", o.getPleaMethod().toString());
        }

        return builder.build();
    }

    protected JsonArray buildResults(final uk.gov.justice.json.schemas.domains.sjp.queries.Offence caseOffence, final ReferencedDecisionsSaved referencedDecisionsSaved) {

        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        if (null != referencedDecisionsSaved) {
            final Optional<Offence> referenceDecisionSavedOffenceOptional = referencedDecisionsSaved.getOffences().stream().filter(o -> o.getId().equals(caseOffence.getId())).findFirst();
            if (referenceDecisionSavedOffenceOptional.isPresent()) {
                referenceDecisionSavedOffenceOptional.get().getResults().forEach(result -> {
                    final JsonObjectBuilder builder = createObjectBuilder();
                    builder .add("id", result.getId().toString()); // --required

                    if (null != result.getPrompts()) {
                        final JsonArrayBuilder prompts = createArrayBuilder();
                        result.getPrompts().forEach(p -> {
                            final JsonObjectBuilder prompt = createObjectBuilder()
                                    .add("id", p.getId().toString())
                                    .add("value", p.getValue());
                            prompts.add(prompt.build());
                        });
                        builder.add("prompts", prompts.build());
                    }
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
        final String verdict = payload.containsKey(VERDICT_KEY) && !payload.isNull(VERDICT_KEY) ? payload.getString(VERDICT_KEY) : null;
        final List<Offence> offences = extractOffences(payload);
        final Integer accountDivisionCode = payload.containsKey(ACCOUNT_DIVISION_CODE_KEY) && !payload.isNull(ACCOUNT_DIVISION_CODE_KEY) ? payload.getInt(ACCOUNT_DIVISION_CODE_KEY) : 0;
        final Integer enforcingCourtCode = payload.containsKey(ENFORCING_COURT_CODE_KEY) && !payload.isNull(ENFORCING_COURT_CODE_KEY) ? payload.getInt(ENFORCING_COURT_CODE_KEY) : 0;

        return new ReferencedDecisionsSaved(caseId, sjpSessionId, resultedOn, verdict, offences, accountDivisionCode, enforcingCourtCode);
    }

    private List<Offence> extractOffences(final JsonObject payload) {
        final JsonArray offencesPayload = payload.containsKey(OFFENCES_KEY) && !payload.isNull(OFFENCES_KEY) ? payload.getJsonArray(OFFENCES_KEY) : createArrayBuilder().build();
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
                .map(result -> new Result(fromString(result.getString("id")), extractTerminalEntries(result.getJsonArray("prompts"))))
                .collect(toList());
    }

    private List<Prompt> extractTerminalEntries(final JsonArray prompts) {
        return prompts.getValuesAs(JsonObject.class)
                .stream()
                .map(prompt -> new Prompt(fromString(prompt.getString("id")), prompt.getString("value")))
                .collect(toList());
    }

    private String getZonedDateTimeString(final ZonedDateTime date) {
        if (null != date) {
            return ZonedDateTimes.toString(date);
        }
        return null;
    }

    private SJPSession extractSJPSession(final JsonObject sjpSessionPayload) {
        final UUID sjpSessionId = fromString(sjpSessionPayload.getString("sessionId"));
        final UUID userId = extractUUID(sjpSessionPayload, "userId");
        final SessionType type = SessionType.valueOf(sjpSessionPayload.getString("type"));
        final String courtHouseCode = sjpSessionPayload.getString("courtHouseCode");
        final String courtHouseName = sjpSessionPayload.getString("courtHouseName");
        final String localJusticeAreaNationalCourtCode = sjpSessionPayload.getString("localJusticeAreaNationalCourtCode");
        final ZonedDateTime startedAt = ZonedDateTimes.fromString(sjpSessionPayload.getString("startedAt"));

        final String magistrate = sjpSessionPayload.containsKey(MAGISTRATE_KEY) && !sjpSessionPayload.getString(MAGISTRATE_KEY).isEmpty() ? sjpSessionPayload.getString(MAGISTRATE_KEY) : null;
        final ZonedDateTime endedAt = sjpSessionPayload.containsKey(ENDED_AT_KEY) && !sjpSessionPayload.getString(ENDED_AT_KEY).isEmpty() ? ZonedDateTimes.fromString(sjpSessionPayload.getString(ENDED_AT_KEY)) : null;
        final CourtDetails courtDetails = new CourtDetails(courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode);
        return new SJPSession(sjpSessionId, userId, type, courtDetails, magistrate, startedAt, endedAt);
    }

    private void extractAndAddDetails(final String providedString, final String stringToAdd, final JsonObjectBuilder objectToAddString) {
        if (null != providedString && !providedString.isEmpty()) {
            objectToAddString.add(stringToAdd, providedString);
        }
    }
}
