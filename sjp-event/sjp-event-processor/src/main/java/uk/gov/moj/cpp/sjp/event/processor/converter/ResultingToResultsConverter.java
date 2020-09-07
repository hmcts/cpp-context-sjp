package uk.gov.moj.cpp.sjp.event.processor.converter;

import static java.lang.Integer.parseInt;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.CourtsGender;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.results.BaseCaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.results.BaseOffence;
import uk.gov.justice.json.schemas.domains.sjp.results.BasePersonDetail;
import uk.gov.justice.json.schemas.domains.sjp.results.BaseResult;
import uk.gov.justice.json.schemas.domains.sjp.results.BaseSessionStructure;
import uk.gov.justice.json.schemas.domains.sjp.results.CaseDefendant;
import uk.gov.justice.json.schemas.domains.sjp.results.CaseOffence;
import uk.gov.justice.json.schemas.domains.sjp.results.IndividualDefendant;
import uk.gov.justice.json.schemas.domains.sjp.results.Plea;
import uk.gov.justice.json.schemas.domains.sjp.results.Prompts;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicSjpResulted;
import uk.gov.justice.json.schemas.domains.sjp.results.SessionLocation;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.resulting.CaseDecision;
import uk.gov.moj.cpp.sjp.domain.resulting.CaseResults;
import uk.gov.moj.cpp.sjp.domain.resulting.CourtDetails;
import uk.gov.moj.cpp.sjp.domain.resulting.Result;
import uk.gov.moj.cpp.sjp.domain.resulting.SJPSession;
import uk.gov.moj.cpp.sjp.event.processor.service.ProsecutionCaseFileService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class ResultingToResultsConverter {

    private static final String DEFAULT_NON_POLICE_PROSECUTOR_REFERENCE = "0800NP0100000000001H";
    private static final String UNKNOWN = "0";
    private static final String OFFENCES_KEY = "offences";
    private static final String ADDRESS1_KEY = "address1";
    private static final String ADDRESS2_KEY = "address2";
    private static final String ADDRESS3_KEY = "address3";
    private static final String ADDRESS4_KEY = "address4";
    private static final String ADDRESS5_KEY = "address5";
    private static final String POSTCODE_KEY = "postcode";
    private static final String ROOM_NAME = "00";
    private static final String DEFAULT_BAIL_STATUS = "A";
    private static final String MAGISTRATE_KEY = "magistrate";
    private static final String ENDED_AT_KEY = "endedAt";
    private static final int MODE_OF_TRIAL = 1;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ProsecutionCaseFileService prosecutionCaseFileService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    private static <T> Stream<T> asStream(final List<T> list) {
        return Optional.ofNullable(list)
                .map(List::stream)
                .orElseGet(Stream::empty);
    }

    public PublicSjpResulted convert(final UUID caseId, final Envelope envelope, final CaseDetails caseDetails, final JsonObject sjpSessionPayload) {
        final CaseResults caseResults = jsonObjectToObjectConverter.convert((JsonObject) envelope.payload(), CaseResults.class);
        final SJPSession sjpSession = extractSJPSession(sjpSessionPayload);
        final JsonEnvelope emptyEnvelope = envelopeFrom(metadataFrom(envelope.metadata()), NULL);
        final Optional<JsonObject> court = referenceDataService.getCourtByCourtHouseOUCode(sjpSession.getCourtDetails().getCourtHouseCode(), emptyEnvelope);

        return PublicSjpResulted.publicSjpResulted()
                .withSession(buildSession(sjpSession, court))
                .withCases(buildCases(caseId, caseDetails, caseResults, sjpSession, envelope.metadata()))
                .build();
    }

    protected List<BaseCaseDetails> buildCases(final UUID caseId, final CaseDetails caseDetails, final CaseResults caseResults, final SJPSession sjpSession, final Metadata metadata) {
        final List<BaseCaseDetails> baseCaseDetailsList = new ArrayList<>();
        baseCaseDetailsList.add(BaseCaseDetails.baseCaseDetails()
                .withCaseId(caseId)
                .withUrn(caseDetails.getUrn())
                .withProsecutionAuthorityCode(caseDetails.getProsecutingAuthority().toString())
                .withDefendants(buildDefendants(caseDetails, caseResults, sjpSession, metadata)).build());
        return baseCaseDetailsList;
    }

    protected BaseSessionStructure buildSession(final SJPSession sjpSession, final Optional<JsonObject> court) {
        return BaseSessionStructure.baseSessionStructure()
                .withSessionId(sjpSession.getId())
                .withDateAndTimeOfSession(sjpSession.getStartedAt())
                .withOuCode(sjpSession.getCourtDetails().getCourtHouseCode())
                .withSessionLocation(buildSessionLocation(sjpSession, court))
                .build();
    }

    protected SessionLocation buildSessionLocation(final SJPSession sjpSession, final Optional<JsonObject> courtOptional) {
        final SessionLocation.Builder builder = SessionLocation.sessionLocation();

        builder.withCourtId(courtOptional.isPresent() ? fromString(courtOptional.get().getString("id", null)) : null)
                .withCourtHouseCode(sjpSession.getCourtDetails().getCourtHouseCode())
                .withName(sjpSession.getCourtDetails().getCourtHouseName())
                .withRoomName(ROOM_NAME)
                .withLja(courtOptional.isPresent() ? courtOptional.get().getString("lja", null) : null);

        if (courtOptional.isPresent()) {
            final JsonObject court = courtOptional.get();
            builder.withAddress(Address.address()
                    .withAddress1(court.getString(ADDRESS1_KEY, null))
                    .withAddress2(court.getString(ADDRESS2_KEY, null))
                    .withAddress3(court.getString(ADDRESS3_KEY, null))
                    .withAddress4(court.getString(ADDRESS4_KEY, null))
                    .withAddress5(court.getString(ADDRESS5_KEY, null))
                    .withPostcode(court.getString(POSTCODE_KEY, null))
                    .build());
        }

        return builder.build();
    }

    protected List<CaseDefendant> buildDefendants(final CaseDetails caseDetails, final CaseResults caseResults, final SJPSession sjpSession, final Metadata metadata) {
        final List<CaseDefendant> arrayBuilder = new ArrayList<>();
        final Defendant defendant = caseDetails.getDefendant();
        if (null != defendant) {
            final JsonEnvelope emptyEnvelope = envelopeFrom(metadataFrom(metadata), NULL);
            final JsonObject caseFileDefendantDetails = prosecutionCaseFileService.getCaseFileDefendantDetails(caseDetails.getId(), emptyEnvelope).orElse(null); // this service returns first defendant as in the SJP there is only 1 defendant.
            final Optional<JsonObject> defendantSelfDefinedInformationOptional = Optional.ofNullable(caseFileDefendantDetails)
                    .map(defendantDetails -> (JsonObject) defendantDetails.getOrDefault("selfDefinedInformation", createObjectBuilder().build()));
            final Optional<JsonArray> caseFileDefendantOffencesOptional = Optional.ofNullable(caseFileDefendantDetails)
                    .map(defendantDetails -> (JsonArray) defendantDetails.getOrDefault(OFFENCES_KEY, createArrayBuilder().build()));
            final String countryCJSCode = defendantSelfDefinedInformationOptional
                    .map(selfDefinedInformation -> selfDefinedInformation.getString("nationality", null))
                    .flatMap(selfDefinedNationality -> referenceDataService.getNationality(selfDefinedNationality, emptyEnvelope))
                    .map(referenceDataNationality -> referenceDataNationality.getString("isoCode", null))
                    .orElse(UNKNOWN);

            arrayBuilder.add(CaseDefendant.caseDefendant()
                    .withDefendantId(defendant.getId())
                    .withProsecutorReference(DEFAULT_NON_POLICE_PROSECUTOR_REFERENCE)
                    .withIndividualDefendant(buildIndividualDefendant(defendant, countryCJSCode))
                    .withOffences(buildOffences(caseDetails, caseResults, sjpSession, caseFileDefendantOffencesOptional))
                    .build());
        }

        return arrayBuilder;
    }

    protected IndividualDefendant buildIndividualDefendant(final Defendant defendant, final String countryCJSCode) {
        final IndividualDefendant.Builder objectBuilder = IndividualDefendant.individualDefendant();
        final PersonalDetails personalDetails = defendant.getPersonalDetails();

        if (null != personalDetails) {
            objectBuilder.withBasePersonDetails(buildPerson(defendant.getPersonalDetails()));
        }
        objectBuilder.withPersonStatedNationality(countryCJSCode)
                .withBailStatus(DEFAULT_BAIL_STATUS)
                .withPresentAtHearing(false);
        return objectBuilder.build();
    }

    protected BasePersonDetail buildPerson(final PersonalDetails personalDetails) {
        final BasePersonDetail.Builder person = BasePersonDetail.basePersonDetail();

        person.withPersonTitle(personalDetails.getTitle())
                .withFirstName(personalDetails.getFirstName())
                .withLastName(personalDetails.getLastName())
                .withAddress(personalDetails.getAddress());

        final ContactDetails contactDetails = personalDetails.getContactDetails();
        if (null != contactDetails) {
            person.withTelephoneNumberBusiness(contactDetails.getBusiness())
                    .withTelephoneNumberHome(contactDetails.getHome())
                    .withTelephoneNumberMobile(contactDetails.getMobile())
                    .withEmailAddress1(contactDetails.getEmail())
                    .withEmailAddress2(contactDetails.getEmail2()).build();
        }

        if (null != personalDetails.getDateOfBirth()) {
            person.withBirthDate(personalDetails.getDateOfBirth().atStartOfDay(ZoneId.systemDefault()));
        }

        if (null != personalDetails.getGender()) {
            person.withGender(CourtsGender.valueOf(personalDetails.getGender().toString()
                    .toUpperCase()
                    .replace(' ','_')));
        }

        return person.build();
    }

    protected List<CaseOffence> buildOffences(final CaseDetails caseDetails, final CaseResults caseResults, final SJPSession sjpSession,
                                              final Optional<JsonArray> caseFileDefendantOffencesOptional) {
        final List<CaseOffence> arrayBuilder = new ArrayList<>();
        final JsonArray caseFileDefendantOffences = caseFileDefendantOffencesOptional.isPresent() ? caseFileDefendantOffencesOptional.get() : createArrayBuilder().build();

        if (null != caseDetails.getDefendant().getOffences()) {
            buildOffence(caseDetails, caseResults, sjpSession, arrayBuilder, caseFileDefendantOffences);
        }

        return arrayBuilder;
    }

    private void buildOffence(final CaseDetails caseDetails, final CaseResults caseResults,
                              final SJPSession sjpSession, final List<CaseOffence> arrayBuilder, final JsonArray caseFileDefendantOffences) {

        final List<uk.gov.justice.json.schemas.domains.sjp.queries.Offence> offences = caseDetails.getDefendant().getOffences();
        offences.stream().forEach(offence -> {

            final JsonObject caseFileDefendantOffence = caseFileDefendantOffences
                    .stream()
                    .map(cfdo -> (JsonObject) cfdo)
                    .filter(cfdo -> cfdo.getString("offenceId", null).equalsIgnoreCase(offence.getId().toString()))
                    .findFirst()
                    .orElse(createObjectBuilder().build());


            final List<CaseDecision> caseDecisions = caseResults.getCaseDecisions();

            for (final CaseDecision caseDecision : caseDecisions) {
                final CaseOffence caseOffence = CaseOffence.caseOffence()
                        .withModeOfTrial(MODE_OF_TRIAL)
                        .withBaseOffenceDetails(buildBaseOffenceDetails(offence, caseFileDefendantOffence))
                        .withInitiatedDate(null != offence.getStartDate() ? LocalDate.parse(offence.getStartDate()).atStartOfDay(ZoneId.systemDefault()) : null)
                        .withPlea(buildPlea(offence))
                        .withConvictionDate(caseDecision.getResultedOn())
                        .withConvictingCourt(parseInt(sjpSession.getCourtDetails().getLocalJusticeAreaNationalCourtCode()))
                        .withResults(buildResults(caseDecision)).build();

                arrayBuilder.add(caseOffence);
            }
        });
    }

    @SuppressWarnings("squid:S1067")
    protected BaseOffence buildBaseOffenceDetails(final uk.gov.justice.json.schemas.domains.sjp.queries.Offence o, final JsonObject caseFileDefendantOffence) {
        return BaseOffence.baseOffence()
                .withOffenceId(o.getId())
                .withOffenceSequenceNumber(o.getOffenceSequenceNumber())
                .withOffenceCode(o.getCjsCode())
                .withOffenceWording(o.getWording())
                .withOffenceDateCode(o.getOffenceDateCode())
                .withOffenceStartDate(null != o.getStartDate() ? LocalDate.parse(o.getStartDate()) : null)
                .withOffenceEndDate(null != o.getEndDate() ? LocalDate.parse(o.getEndDate()) : null)
                .withChargeDate(null != o.getChargeDate() ? LocalDate.parse(o.getChargeDate()) : null)
                .withLocationOfOffence(null != caseFileDefendantOffence ? caseFileDefendantOffence.getString("offenceLocation", null) : null)
                .build();
    }

    protected Plea buildPlea(final uk.gov.justice.json.schemas.domains.sjp.queries.Offence o) {
        return Plea.plea()
                .withPleaType(o.getPlea())
                .withPleaDate(o.getPleaDate())
                .withPleaMethod(o.getPleaMethod()).build();

    }

    protected List<BaseResult> buildResults(final CaseDecision caseDecision) {
        final List<BaseResult> baseResultBuilderList = new ArrayList<>();

        if (nonNull(caseDecision)) {
            asStream(caseDecision.getOffences())
                    .forEach(referenceDecisionSavedOffence -> referenceDecisionSavedOffence.getResults()
                            .forEach(result -> getBaseResultBuilderListWithPrompts(baseResultBuilderList, result)));
            return baseResultBuilderList;
        }
        return baseResultBuilderList;
    }

    private void getBaseResultBuilderListWithPrompts(final List<BaseResult> baseResultBuilderList, final Result result) {
        final BaseResult.Builder baseResultBuilder = BaseResult.baseResult();
        baseResultBuilder.withId(result.getResultDefinitionId());

        final List<Prompts> promptList = new ArrayList<>();
        if (null != result.getPrompts()) {
            result.getPrompts().forEach(p ->
                    promptList.add(Prompts.prompts()
                            .withId(p.getPromptDefinitionId())
                            .withValue(p.getValue()).build()));
            baseResultBuilder.withPrompts(promptList);
        }
        baseResultBuilderList.add(baseResultBuilder.build());
    }

    private UUID extractUUID(final JsonObject object, final String key) {
        return object.containsKey(key) && !object.getString(key).isEmpty() ? fromString(object.getString(key, null)) : null;
    }

    private SJPSession extractSJPSession(final JsonObject sjpSessionPayload) {
        final UUID sjpSessionId = fromString(sjpSessionPayload.getString("sessionId", null));
        final UUID userId = extractUUID(sjpSessionPayload, "userId");
        final SessionType type = SessionType.valueOf(sjpSessionPayload.getString("type", null));
        final String courtHouseCode = sjpSessionPayload.getString("courtHouseCode", null);
        final String courtHouseName = sjpSessionPayload.getString("courtHouseName", null);
        final String localJusticeAreaNationalCourtCode = sjpSessionPayload.getString("localJusticeAreaNationalCourtCode", null);
        final ZonedDateTime startedAt = ZonedDateTimes.fromString(sjpSessionPayload.getString("startedAt", null));

        final String magistrate = sjpSessionPayload.containsKey(MAGISTRATE_KEY) && !sjpSessionPayload.getString(MAGISTRATE_KEY).isEmpty() ? sjpSessionPayload.getString(MAGISTRATE_KEY, null) : null;
        final ZonedDateTime endedAt = sjpSessionPayload.containsKey(ENDED_AT_KEY) && !sjpSessionPayload.getString(ENDED_AT_KEY).isEmpty() ? ZonedDateTimes.fromString(sjpSessionPayload.getString(ENDED_AT_KEY, null)) : null;
        final CourtDetails courtDetails = new CourtDetails(courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode);
        return new SJPSession(sjpSessionId, userId, type, courtDetails, magistrate, startedAt, endedAt);
    }

}
