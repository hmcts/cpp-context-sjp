package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
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
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.AddressView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ContactView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.DefendantAliasView;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;

public class ProsecutionCasesViewHelper {

    private static final String WELSH_LANGUAGE_CODE = "W";
    private static final String OFFENCES_KEY = "offences";

    @SuppressWarnings("squid:S00107")
    public List<ProsecutionCaseView> createProsecutionCaseViews(
            final CaseDetails caseDetails,
            final JsonObject prosecutors,
            final JsonObject prosecutionCaseFile,
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
                offenceDefinitionIdByOffenceCode,
                caseReferredForCourtHearing.getDefendantCourtOptions());

        final JsonObject prosecutor = prosecutors.getJsonArray("prosecutors").getJsonObject(0);
        final boolean policeCase = ofNullable(caseDetails.getPoliceFlag()).orElse(false);
        String prosecutingAuthorityReference = null;
        String caseURN = null;

        if(policeCase){
            caseURN = caseDetails.getUrn();
        } else {
            prosecutingAuthorityReference = caseDetails.getUrn();
        }

        final ProsecutionCaseIdentifierView prosecutionCaseIdentifier = new ProsecutionCaseIdentifierView(
                UUID.fromString(prosecutor.getString("id")),
                prosecutor.getString("shortName"),
                prosecutingAuthorityReference,
                caseURN
        );


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
                singletonList(defendantView),
                ofNullable(prosecutionCaseFile)
                        .map(caseFile -> caseFile.getString("originatingOrganisation", null))
                        .orElse(null));

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
            final Map<String, UUID> offenceDefinitionIdByOffenceCode,
            final DefendantCourtOptions defendantCourtOptions) {

        final Defendant defendantDetails = caseDetails.getDefendant();

        final PersonDefendantView personDefendantView = createPersonDefendantView(
                defendantDetails,
                caseFileDefendantDetails,
                employer,
                nationalityId,
                ethnicityId,
                defendantCourtOptions);

        final List<OffenceView> offenceViews = referredOffences
                .stream().map(offence -> createOffenceView(offenceDecisionInformationList,
                        referredAt,
                        offence,
                        caseFileDefendantDetails,
                        createNotifiedPleaView(referredAt, offence),
                        offenceDefinitionIdByOffenceCode))
                .collect(Collectors.toList());

        final List<DefendantAliasView> aliases = ofNullable(caseFileDefendantDetails)
                .map(pcfDefendantDetails -> {
                    if (caseFileDefendantDetails.containsKey("individualAliases")) {
                        final JsonArray individualAliases = caseFileDefendantDetails.getJsonArray("individualAliases");
                        return individualAliases
                                .getValuesAs(JsonObject.class).stream()
                                .map(this::toDefendantAliasView)
                                .collect(Collectors.toList());
                    }
                    return null;
                })
                .filter(aliasViews -> !aliasViews.isEmpty())
                .orElse(null);


        return new DefendantView(
                defendantDetails.getId(),
                caseDetails.getId(),
                defendantDetails.getNumPreviousConvictions(),
                pleaMitigation,
                offenceViews,
                personDefendantView,
                aliases);
    }

    private DefendantAliasView toDefendantAliasView(final JsonObject individualAlias) {
        return new DefendantAliasView(
                individualAlias.getString("title", null),
                individualAlias.getString("firstName", null),
                individualAlias.getString("givenName2", null),
                individualAlias.getString("lastName", null)
        );
    }

    private static PersonDefendantView createPersonDefendantView(final Defendant defendant,
                                                                 final JsonObject caseFileDefendantDetails,
                                                                 final EmployerDetails employer,
                                                                 final String nationalityId,
                                                                 final String ethnicityId,
                                                                 final DefendantCourtOptions defendantCourtOptions) {

        final PersonalDetails defendantPersonalDetails = defendant.getPersonalDetails();
        final Optional<JsonObject> defendantPersonalInformationOptional = ofNullable(caseFileDefendantDetails)
                .map(defendantDetails -> (JsonObject) defendantDetails.getOrDefault("personalInformation", createObjectBuilder().build()));

        String interpreter = ofNullable(defendant.getInterpreter()).map(Interpreter::getLanguage).orElse(null);
        if(defendantCourtOptions != null && defendantCourtOptions.getInterpreter() != null) {
            interpreter = defendantCourtOptions.getInterpreter().getLanguage();
        }

        return new PersonDefendantView(
                PersonDetailsView.builder()
                        .withTitle(DefendantTitleParser.parse(defendantPersonalDetails.getTitle()))
                        .withFirstName(defendantPersonalDetails.getFirstName())
                        .withLastName(defendantPersonalDetails.getLastName())
                        .withDateOfBirth(defendantPersonalDetails.getDateOfBirth())
                        .withGender(defendantPersonalDetails.getGender().name())
                        .withInterpreterLanguageNeeds(interpreter)
                        .withDisabilityStatus(ofNullable(defendantCourtOptions)
                                .map(DefendantCourtOptions::getDisabilityNeeds)
                                .map(DisabilityNeeds::getDisabilityNeeds)
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
                        .withSpecificRequirements(createSpecialRequirement(caseFileDefendantDetails, defendantCourtOptions))
                        .withAddress(createAddressView(defendantPersonalDetails.getAddress()))
                        .withContact(createDefendantContactView(defendantPersonalDetails, caseFileDefendantDetails))
                        .build(),
                ofNullable(employer.getName())
                        .map(employerName -> new EmployerOrganisationView(
                                employerName,
                                createAddressView(employer.getAddress()),
                                new ContactView(employer.getPhone())))
                        .orElse(null),
                ethnicityId,
                defendantPersonalDetails.getDriverNumber(),
                null,
                ofNullable(caseFileDefendantDetails)
                        .map(defendantDetails -> defendantDetails.getString("asn", null))
                        .orElse(null));

    }

    private static OffenceView createOffenceView(
            final List<OffenceDecisionInformation> offenceDecisionInformationList,
            final LocalDate referredAt,
            final Offence offenceDetails,
            final JsonObject caseFileDefendantDetails,
            final NotifiedPleaView notifiedPleaView,
            final Map<String, UUID> offenceDefinitionIdByOffenceCode) {

        final Optional<JsonObject> caseFileOffenceDetailsOptional = ofNullable(caseFileDefendantDetails)
                .flatMap(defendantDetails -> defendantDetails.getJsonArray(OFFENCES_KEY)
                        .getValuesAs(JsonObject.class)
                        .stream()
                        .filter(caseFileOffence -> offenceDetails.getId().toString().equals(caseFileOffence.getString("offenceId",null)))
                        .findFirst());

        final VerdictType verdict = offenceDecisionInformationList
                .stream()
                .filter(offenceDecisionInformation -> offenceDecisionInformation.getOffenceId().equals(offenceDetails.getId()))
                .map(OffenceDecisionInformation::getVerdict)
                .filter(Objects::nonNull)
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
                .withOffenceFacts(createOffenceFactsView(offenceDetails, caseFileOffenceDetailsOptional))
                .build();

    }

    private static OffenceFactsView createOffenceFactsView(final Offence offence, final Optional<JsonObject> caseFileOffenceOptional) {
        if(isNotEmpty(offence.getVehicleRegistrationMark()) ||  isNotEmpty(offence.getVehicleMake())) {

            final OffenceFactsView.Builder factsBuilder = OffenceFactsView.builder()
                    .withVehicleMake(offence.getVehicleMake())
                    .withVehicleRegistration(offence.getVehicleRegistrationMark());

            caseFileOffenceOptional
                    .map(caseFileOffence -> caseFileOffence.getJsonObject("alcoholRelatedOffence"))
                    .ifPresent(alcoholRelatedFacts -> {
                        if(alcoholRelatedFacts.containsKey("alcoholLevelAmount")){
                            factsBuilder.withAlcoholReadingAmount(alcoholRelatedFacts.getInt("alcoholLevelAmount"));
                        }

                        factsBuilder.withAlcoholReadingMethodCode(alcoholRelatedFacts.getString("alcoholLevelMethod", null));
                    });

            return factsBuilder.build();
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

    private static String createSpecialRequirement(final JsonObject caseFileDefendantDetails, final DefendantCourtOptions defendantCourtOptions) {
        final Optional<String> disabiltyStatus = ofNullable(defendantCourtOptions)
                .map(DefendantCourtOptions::getDisabilityNeeds)
                .map(DisabilityNeeds::getDisabilityNeeds) ;

        final String specificRequirements = ofNullable(caseFileDefendantDetails)
                .map(defendantDetails -> defendantDetails.getString("specificRequirements", null))
                .orElse(null);

      return   disabiltyStatus.orElse(specificRequirements);


    }

    private static Optional<String> getProsecutionFacts(final List<Offence> offences) {
        return offences
                .stream()
                .map(Offence::getProsecutionFacts)
                .filter(StringUtils::isNotEmpty)
                .findFirst();
    }
}
