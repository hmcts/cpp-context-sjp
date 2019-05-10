package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static com.google.common.collect.Iterables.getFirst;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;
import uk.gov.justice.json.schemas.domains.sjp.Interpreter;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.json.schemas.domains.sjp.query.EmployerDetails;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.AddressView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ContactView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.DefendantView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.EmployerOrganisationView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.NotifiedPleaView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.OffenceView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.PersonDefendantView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.PersonDetailsView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseIdentifierView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseView;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

public class ProsecutionCasesViewHelper {

    private static final String WELSH_LANGUAGE_CODE = "W";
    private static final String NO_VERDICT = "NO_VERDICT";
    private static final String OFFENCES_KEY = "offences";

    @SuppressWarnings("squid:S00107")
    public List<ProsecutionCaseView> createProsecutionCaseViews(
            final CaseDetails caseDetails,
            final JsonObject referenceDataOffences,
            final JsonObject prosecutors,
            final JsonObject caseDecision,
            final JsonObject caseFileDefendantDetails,
            final EmployerDetails employer,
            final String nationalityId,
            final String ethnicityId,
            final LocalDate referredAt,
            final NotifiedPleaView notifiedPleaView,
            final String pleaMitigation) {

        final Offence offenceDetails = ofNullable(getFirst(caseDetails.getDefendant().getOffences(), null))
                .orElseThrow(() -> new IllegalStateException(String.format("Offence not found for case %s", caseDetails.getId())));

        final String verdict = caseDecision.getString("verdict", NO_VERDICT);
        final LocalDate convictionDate = NO_VERDICT.equals(verdict) ? null : referredAt;

        final DefendantView defendantView = createDefendantView(
                caseDetails,
                referenceDataOffences,
                offenceDetails,
                convictionDate,
                caseFileDefendantDetails,
                employer,
                nationalityId,
                ethnicityId,
                notifiedPleaView,
                pleaMitigation);

        final JsonObject prosecutor = prosecutors.getJsonArray("prosecutors").getJsonObject(0);
        final ProsecutionCaseIdentifierView prosecutionCaseIdentifier = new ProsecutionCaseIdentifierView(
                UUID.fromString(prosecutor.getString("id")),
                prosecutor.getString("shortName"),
                caseDetails.getUrn());

        final String statementOfFactsWelsh = Optional.ofNullable(caseFileDefendantDetails)
                .map(defendantDetails -> defendantDetails.getJsonArray(OFFENCES_KEY))
                .map(defendantOffences -> defendantOffences.getJsonObject(0).getString("statementOfFactsWelsh", null))
                .orElse(null);

        final ProsecutionCaseView prosecutionCaseView = new ProsecutionCaseView(
                caseDetails.getId(),
                "J",
                offenceDetails.getProsecutionFacts(),
                statementOfFactsWelsh,
                prosecutionCaseIdentifier,
                singletonList(defendantView));

        return singletonList(prosecutionCaseView);
    }

    @SuppressWarnings("squid:S00107")
    private DefendantView createDefendantView(
            final CaseDetails caseDetails,
            final JsonObject referenceDataOffences,
            final Offence offenceDetails,
            final LocalDate convictionDate,
            final JsonObject caseFileDefendantDetails,
            final EmployerDetails employer,
            final String nationalityId,
            final String ethnicityId,
            final NotifiedPleaView notifiedPleaView,
            final String pleaMitigation) {

        final Defendant defendantDetails = caseDetails.getDefendant();
        final PersonDefendantView personDefendantView = createPersonDefendantView(
                defendantDetails,
                caseFileDefendantDetails,
                employer,
                nationalityId,
                ethnicityId);

        final OffenceView offence = createOffenceView(
                referenceDataOffences,
                convictionDate,
                offenceDetails,
                caseFileDefendantDetails,
                notifiedPleaView);

        return new DefendantView(
                defendantDetails.getId(),
                caseDetails.getId(),
                defendantDetails.getNumPreviousConvictions(),
                pleaMitigation,
                singletonList(offence),
                personDefendantView);
    }

    private static PersonDefendantView createPersonDefendantView(final Defendant defendant,
                                                                 final JsonObject caseFileDefendantDetails,
                                                                 final EmployerDetails employer,
                                                                 final String nationalityId,
                                                                 final String ethnicityId) {

        final PersonalDetails defendantPersonalDetails = defendant.getPersonalDetails();
        final Optional<JsonObject> defendantPersonalInformationOptional = ofNullable(caseFileDefendantDetails)
                .map(defendantDetails -> (JsonObject) defendantDetails.getOrDefault("personalInformation", createObjectBuilder().build()));

        return new PersonDefendantView(
                PersonDetailsView.builder()
                        .withTitle(DefendantTitleParser.parse(defendantPersonalDetails.getTitle()))
                        .withFirstName(defendantPersonalDetails.getFirstName())
                        .withLastName(defendantPersonalDetails.getLastName())
                        .withDateOfBirth(defendantPersonalDetails.getDateOfBirth())
                        .withGender(defendantPersonalDetails.getGender().name())
                        .withInterpreterLanguageNeeds(
                                ofNullable(defendant.getInterpreter())
                                        .map(Interpreter::getLanguage)
                                        .orElse(null))
                        .withNationalityId(nationalityId)
                        .withEthnicityId(ethnicityId)
                        .withDocumentationLanguageNeeds(ofNullable(caseFileDefendantDetails)
                                .map(defendantDetails -> defendantDetails.getString("documentationLanguage", null))
                                .map(documentationLanguage -> WELSH_LANGUAGE_CODE.equals(documentationLanguage) ? "WELSH" : "ENGLISH")
                                .orElse("ENGLISH"))
                        .withNationalInsuranceNumber(defendant.getPersonalDetails().getNationalInsuranceNumber())
                        .withOccupation(defendantPersonalInformationOptional.map(personalInformation -> personalInformation.getString("occupation", null)).orElse(null))
                        .withOccupationCode(defendantPersonalInformationOptional.map(personalInformation -> personalInformation.getInt("occupationCode", 0))
                                .map(String::valueOf)
                                .orElse(null))
                        .withSpecificRequirements(ofNullable(caseFileDefendantDetails)
                                .map(defendantDetails -> defendantDetails.getString("specificRequirements", null))
                                .orElse(null))
                        .withAddress(createAddressView(defendantPersonalDetails.getAddress()))
                        .withContact(createDefendantContactView(defendantPersonalDetails, caseFileDefendantDetails))
                        .build(),
                ofNullable(employer.getName())
                        .map(employerName -> new EmployerOrganisationView(
                                employerName,
                                createAddressView(employer.getAddress()),
                                new ContactView(employer.getPhone())))
                        .orElse(null));

    }

    private static OffenceView createOffenceView(
            final JsonObject referenceDataOffences,
            final LocalDate convictionDate,
            final Offence offenceDetails,
            final JsonObject caseFileDefendantDetails,
            final NotifiedPleaView notifiedPleaView) {

        final Optional<String> offenceDefinitionIdOptional = referenceDataOffences.getJsonArray(OFFENCES_KEY).getValuesAs(JsonObject.class).stream()
                .filter(referenceDataOffence -> referenceDataOffence.getString("cjsOffenceCode").equals(offenceDetails.getCjsCode()))
                .map(referenceDataOffence -> referenceDataOffence.getString("offenceId"))
                .findFirst();

        final Optional<JsonObject> caseFileOffenceDetailsOptional = ofNullable(caseFileDefendantDetails)
                .map(defendantDetails -> defendantDetails.getJsonArray(OFFENCES_KEY).getJsonObject(0));

        return OffenceView.builder()
                .withId(offenceDetails.getId())
                .withOffenceDefinitionId(offenceDefinitionIdOptional.map(UUID::fromString).orElse(null))
                .withWording(offenceDetails.getWording())
                .withWordingWelsh(offenceDetails.getWordingWelsh())
                .withStartDate(LocalDate.parse(offenceDetails.getStartDate()))
                .withChargeDate(LocalDate.parse(offenceDetails.getChargeDate()))
                .withConvictionDate(convictionDate)
                .withOrderIndex(offenceDetails.getOffenceSequenceNumber())
                .withNotifiedPlea(notifiedPleaView)
                .withEndDate(caseFileOffenceDetailsOptional.map(caseFileOffenceDetails -> caseFileOffenceDetails.getString("offenceCommittedEndDate", null))
                        .map(LocalDate::parse)
                        .orElse(null))
                .build();

    }

    private static AddressView createAddressView(final Address nullableAddress) {
        return Optional.ofNullable(nullableAddress)
                .map(address -> new AddressView(
                        address.getAddress1(),
                        address.getAddress2(),
                        address.getAddress3(),
                        address.getAddress4(),
                        address.getAddress5(),
                        address.getPostcode()))
                .orElse(null);
    }

    private static ContactView createDefendantContactView(final PersonalDetails defendantPersonalDetails, final JsonObject caseFileDefendantDetails) {
        final ContactDetails defendantContactDetails = defendantPersonalDetails.getContactDetails();
        final Optional<JsonObject> caseFileDefendantPersonalInformation = ofNullable(caseFileDefendantDetails)
                .map(defendantDetails -> defendantDetails.getJsonObject("personalInformation"));

        return new ContactView(
                defendantPersonalDetails.getContactDetails().getHome(),
                caseFileDefendantPersonalInformation.map(personalInformation -> personalInformation.getString("work", null)).orElse(null),
                defendantContactDetails.getMobile(),
                defendantContactDetails.getEmail(),
                caseFileDefendantPersonalInformation.map(personalInformation -> personalInformation.getString("secondaryEmail", null)).orElse(null));
    }

}
