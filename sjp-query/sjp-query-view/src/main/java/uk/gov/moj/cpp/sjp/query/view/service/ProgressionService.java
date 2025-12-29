package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

@SuppressWarnings("squid:CallToDeprecatedMethod")
public class ProgressionService {

    public static final String PROSECUTION_CASE_FIELD_NAME = "prosecutionCase";
    public static final String PERSON_DEFENDANT_FIELD_NAME = "personDefendant";
    public static final String PERSON_DETAILS_FIELD_NAME = "personDetails";
    public static final String CASE_STATUS_FIELD_NAME = "caseStatus";
    public static final String FIRSTNAME_FIELD_NAME = "firstName";
    public static final String CASEID_FIELD_NAME = "caseId";
    public static final String OFFENCES_FIELD_NAME = "offences";
    public static final String DEFENDANTS_FIELD_NAME = "defendants";
    public static final String PROGRESSION_CASE_QUERY = "progression.query.case";
    public static final String WORDING_FIELD_NAME = "wording";
    public static final String LASTNAME_FIELD_NAME = "lastName";
    public static final String DOB_FIELD_NAME = "dateOfBirth";
    public static final String ADDRESS_FIELD_NAME = "address";
    public static final String ADDRESS1_FIELD_NAME = "address1";
    public static final String POSTCODE_FIELD_NAME = "postcode";

    @Inject
    @ServiceComponent(QUERY_VIEW)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public Optional<JsonObject> findCaseById(UUID caseId) {
        final JsonObject requestPayload = createObjectBuilder().
                                              add(CASEID_FIELD_NAME, caseId.toString()).
                                              build();
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataBuilder().
                                                 withId(randomUUID()).
                                                 withName(PROGRESSION_CASE_QUERY), requestPayload);
        final JsonEnvelope jsonResultEnvelope = requester.requestAsAdmin(requestEnvelope);

        return ofNullable(jsonResultEnvelope.payloadAsJsonObject());
    }

    public List<String> findDefendantOffences(UUID caseId, DefendantDetail defendant) {
        final Optional<JsonObject> progressionCaseOpt = findCaseById(caseId);
        if (progressionCaseOpt.isPresent()) {
            final JsonObject prosecutionCase = progressionCaseOpt.get().
                                                getJsonObject(PROSECUTION_CASE_FIELD_NAME);
            if (prosecutionCase != null) {
                return prosecutionCase.
                           getJsonArray(DEFENDANTS_FIELD_NAME).
                           stream().
                           filter(caseDefendant -> defendantMatch((JsonObject) caseDefendant, defendant)).
                           map(caseDefendant -> ((JsonObject) caseDefendant).getJsonArray(OFFENCES_FIELD_NAME)).
                           flatMap(Collection::stream).
                           map(offence -> ((JsonObject) offence).getString(WORDING_FIELD_NAME)).
                           collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private boolean defendantMatch(JsonObject caseDefendant, DefendantDetail defendant) {
        final JsonObject personDefendant = caseDefendant.getJsonObject(PERSON_DEFENDANT_FIELD_NAME);
        final JsonObject caseDefendantPersonalDetails = personDefendant.getJsonObject(PERSON_DETAILS_FIELD_NAME);
        final String firstName = caseDefendantPersonalDetails.getString(FIRSTNAME_FIELD_NAME);
        final String lastName = caseDefendantPersonalDetails.getString(LASTNAME_FIELD_NAME);
        final String dateOfBirth = caseDefendantPersonalDetails.getString(DOB_FIELD_NAME);
        final JsonObject address = caseDefendantPersonalDetails.getJsonObject(ADDRESS_FIELD_NAME);
        final String addressLine1 = address.getString(ADDRESS1_FIELD_NAME);
        final String postCode = address.getString(POSTCODE_FIELD_NAME);

        final PersonalDetails personalDetails = defendant.getPersonalDetails();

        return Objects.equals(firstName, personalDetails.getFirstName()) &&
                   Objects.equals(lastName, personalDetails.getLastName()) &&
                   Objects.equals(dateOfBirth, personalDetails.getDateOfBirth().toString()) &&
                   Objects.equals(addressLine1, defendant.getAddress().getAddress1()) &&
                   Objects.equals(postCode, defendant.getAddress().getPostcode());
    }
}
