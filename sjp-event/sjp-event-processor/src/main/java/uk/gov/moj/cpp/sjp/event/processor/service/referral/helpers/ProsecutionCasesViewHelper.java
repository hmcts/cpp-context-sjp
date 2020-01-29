package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.NotifiedPleaViewHelper.createNotifiedPleaView;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;
import uk.gov.justice.json.schemas.domains.sjp.Interpreter;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.json.schemas.domains.sjp.query.EmployerDetails;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.AddressView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ContactView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.DefendantView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.EmployerOrganisationView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.NotifiedPleaView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.OffenceFactsView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.OffenceView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.PersonDefendantView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.PersonDetailsView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseIdentifierView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseView;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;

public class ProsecutionCasesViewHelper {

    private static final String WELSH_LANGUAGE_CODE = "W";
    private static final String OFFENCES_KEY = "offences";

    @SuppressWarnings("squid:S00107")
    public List<ProsecutionCaseView> createProsecutionCaseViews(
            final CaseDetails caseDetails,
            final JsonObject prosecutors,
            final JsonObject caseFileDefendantDetails,
            final EmployerDetails employer,
            final String nationalityId,
            final String ethnicityId,
            final CaseReferredForCourtHearing caseReferredForCourtHearing,
            final String pleaMitigation,
            final Map<String, UUID> offenceDefinitionIdByOffenceCode,
            final List<Offence> referredOffences) {

        final String prosecutionFacts = getProsecutionFacts(referredOffences)
                .orElse(getProsecutionFacts(caseDetails.getDefendant().getOffences()).orElse(null));

        final DefendantView defendantView = createDefendantView(
                caseDetails,
                referredOffences,
                caseReferredForCourtHearing.getReferredOffences(),
                caseReferredForCourtHearing.getReferredAt().toLocalDate(),
                caseFileDefendantDetails,
                employer,
                nationalityId,
                ethnicityId,
                pleaMitigation,
                offenceDefinitionIdByOffenceCode);

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
                prosecutionFacts,
                statementOfFactsWelsh,
                prosecutionCaseIdentifier,
                singletonList(defendantView));

        return singletonList(prosecutionCaseView);
    }

    @SuppressWarnings("squid:S00107")
    private DefendantView createDefendantView(
            final CaseDetails caseDetails,
            final List<Offence> referredOffences,
            final List<OffenceDecisionInformation> offenceDecisionInformationList,
            final LocalDate referredAt,
            final JsonObject caseFileDefendantDetails,
            final EmployerDetails employer,
            final String nationalityId,
            final String ethnicityId,
            final String pleaMitigation,
            final Map<String, UUID> offenceDefinitionIdByOffenceCode) {

        final Defendant defendantDetails = caseDetails.getDefendant();
        final PersonDefendantView personDefendantView = createPersonDefendantView(
                defendantDetails,
                caseFileDefendantDetails,
                employer,
                nationalityId,
                ethnicityId);

        final List<OffenceView> offenceViews = referredOffences
                .stream().map(offence -> createOffenceView(offenceDecisionInformationList,
                        referredAt,
                        offence,
                        caseFileDefendantDetails,
                        createNotifiedPleaView(referredAt, offence),
                        offenceDefinitionIdByOffenceCode))
                .collect(Collectors.toList());

        return new DefendantView(
                defendantDetails.getId(),
                caseDetails.getId(),
                defendantDetails.getNumPreviousConvictions(),
                pleaMitigation,
                offenceViews,
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
                        .orElse(null), ethnicityId);

    }

    private static OffenceView createOffenceView(
            final List<OffenceDecisionInformation> offenceDecisionInformationList,
            final LocalDate referredAt,
            final Offence offenceDetails,
            final JsonObject caseFileDefendantDetails,
            final NotifiedPleaView notifiedPleaView,
            final Map<String, UUID> offenceDefinitionIdByOffenceCode) {

        final Optional<JsonObject> caseFileOffenceDetailsOptional = ofNullable(caseFileDefendantDetails)
                .map(defendantDetails -> defendantDetails.getJsonArray(OFFENCES_KEY).getJsonObject(0));

        final VerdictType verdict = offenceDecisionInformationList
                .stream()
                .filter(offenceDecisionInformation -> offenceDecisionInformation.getOffenceId().equals(offenceDetails.getId()))
                .map(OffenceDecisionInformation::getVerdict)
                .findFirst()
                .orElse(NO_VERDICT);

        final LocalDate convictionDate = NO_VERDICT.equals(verdict) ? null : referredAt;

        return OffenceView.builder()
                .withId(offenceDetails.getId())
                .withOffenceDefinitionId(offenceDefinitionIdByOffenceCode.get(offenceDetails.getCjsCode()))
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
                .withOffenceFacts(createOffenceFactsView(offenceDetails))
                .build();

    }

    private static OffenceFactsView createOffenceFactsView(final Offence offence) {
        if(isNotEmpty(offence.getVehicleRegistrationMark()) ||  isNotEmpty(offence.getVehicleMake())) {
            return OffenceFactsView.builder()
                    .withVehicleMake(offence.getVehicleMake())
                    .withVehicleRegistration(offence.getVehicleRegistrationMark())
                    .build();
        } else {
            return null;
        }
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

    private static Optional<String> getProsecutionFacts(final List<Offence> offences) {
        return offences
                .stream()
                .map(Offence::getProsecutionFacts)
                .filter(StringUtils::isNotEmpty)
                .findFirst();
    }
}
