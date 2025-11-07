package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.json.schemas.domains.sjp.queries.DecisionType.REFER_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.NotifiedPleaViewHelper.createNotifiedPleaView;

import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;
import uk.gov.justice.json.schemas.domains.sjp.Interpreter;
import uk.gov.justice.json.schemas.domains.sjp.LegalEntityDetails;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.json.schemas.domains.sjp.queries.PressRestriction;
import uk.gov.justice.json.schemas.domains.sjp.queries.QueryOffenceDecision;
import uk.gov.justice.json.schemas.domains.sjp.query.EmployerDetails;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.decision.SessionCourt;
import uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.VerdictConverter;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.model.prosecution.AddressView;
import uk.gov.moj.cpp.sjp.model.prosecution.ContactView;
import uk.gov.moj.cpp.sjp.model.prosecution.DefendantAliasView;
import uk.gov.moj.cpp.sjp.model.prosecution.DefendantView;
import uk.gov.moj.cpp.sjp.model.prosecution.EmployerOrganisationView;
import uk.gov.moj.cpp.sjp.model.prosecution.NotifiedPleaView;
import uk.gov.moj.cpp.sjp.model.prosecution.OffenceFactsView;
import uk.gov.moj.cpp.sjp.model.prosecution.OffenceView;
import uk.gov.moj.cpp.sjp.model.prosecution.PersonDefendantView;
import uk.gov.moj.cpp.sjp.model.prosecution.PersonDetailsView;
import uk.gov.moj.cpp.sjp.model.prosecution.ProsecutionCaseIdentifierView;
import uk.gov.moj.cpp.sjp.model.prosecution.ProsecutionCaseView;
import uk.gov.moj.cpp.sjp.model.prosecution.ReportingRestrictionView;
import uk.gov.moj.cpp.sjp.model.prosecution.helpers.DefendantTitleParser;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;

public class ProsecutionCasesViewHelper {

    private static final String WELSH_LANGUAGE_CODE = "W";
    private static final String OFFENCES_KEY = "offences";
    private static final String D45_RESULT_CODE = "D45";
    private static final String PROVED_SJP_VERDICT_CODE = "PSJ";

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ReferenceDataOffencesService referenceDataOffencesService;

    @Inject
    private ConvictingCourtHelper convictingCourtViewHelper;

    @Inject
    private VerdictConverter verdictConverter;


    @SuppressWarnings("squid:S00107")
    public List<ProsecutionCaseView> createProsecutionCaseViews(
            final CaseDetails caseDetails,
            final CaseDecision caseDecision,
            final JsonObject prosecutors,
            final JsonObject prosecutionCaseFile,
            final JsonObject caseFileDefendantDetails,
            final EmployerDetails employer,
            final String nationalityId,
            final String ethnicityId,

            final ZonedDateTime referredAt,
            final DefendantCourtOptions defendantCourtOptions,
            final LocalDate convictionDate,
            final SessionCourt convictingCourt,

            final String pleaMitigation,
            final Map<String, JsonObject> offenceDefinition,
            final List<Offence> referredOffences,
            final JsonEnvelope emptyEnvelope) {

        final String prosecutionFacts = getProsecutionFacts(referredOffences)
                .orElse(getProsecutionFacts(caseDetails.getDefendant().getOffences()).orElse(null));

        final DefendantView defendantView = createDefendantView(
                caseDetails,
                caseDecision,
                referredOffences,
                referredAt.toLocalDate(),
                caseFileDefendantDetails,
                employer,
                nationalityId,
                ethnicityId,
                pleaMitigation,
                offenceDefinition,
                defendantCourtOptions,
                convictionDate,
                convictingCourt,
                emptyEnvelope);

        final JsonObject prosecutor = prosecutors.getJsonArray("prosecutors").getJsonObject(0);
        final boolean policeCase = ofNullable(caseDetails.getPoliceFlag()).orElse(false);
        String prosecutingAuthorityReference = null;
        String caseURN = null;

        if (policeCase) {
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
            final CaseDecision caseDecision,
            final List<Offence> referredOffences,
            final LocalDate referredAt,
            final JsonObject caseFileDefendantDetails,
            final EmployerDetails employer,
            final String nationalityId,
            final String ethnicityId,
            final String pleaMitigation,
            final Map<String, JsonObject> offenceDefinition,
            final DefendantCourtOptions defendantCourtOptions,
            final LocalDate convictionDate,
            final SessionCourt convictingCourt,
            final JsonEnvelope emptyEnvelope) {

        final Defendant defendantDetails = caseDetails.getDefendant();

        final PersonDefendantView personDefendantView = ofNullable(defendantDetails.getPersonalDetails())
                .map(personalDetails -> createPersonDefendantView(
                        defendantDetails,
                        caseFileDefendantDetails,
                        employer,
                        nationalityId,
                        ethnicityId,
                        defendantCourtOptions))
                .orElse(null);

        final LegalEntityDefendant legalEntityDefendant = ofNullable(defendantDetails.getLegalEntityDetails())
                .map(legalEntityDetails -> createLegalEntityDefendant(
                        defendantDetails))
                .orElse(null);

        final String rrLabel = getPressRestriction(caseDecision)
                .filter(PressRestriction::getRequested)
                .flatMap(pressRestriction -> referenceDataService.getResultDefinition(D45_RESULT_CODE, now())
                        .map(definition -> definition.getString("label", null)))
                .orElse(null);

        final List<OffenceView> offenceViews = referredOffences
                .stream()
                .map(offence -> {
                    Verdict coreVerdict = null;
                    if (Objects.nonNull(offence.getConviction())) {
                        coreVerdict = verdictConverter.getVerdict(VerdictType.valueOf(offence.getConviction().name()), offence.getId(), convictionDate);
                    }
                    return createOffenceView(
                            referredAt,
                            offence,
                            caseFileDefendantDetails,
                            rrLabel,
                            createNotifiedPleaView(referredAt, offence),
                            offenceDefinition,
                            convictionDate,
                            convictingCourtViewHelper.createConvictingCourt(convictingCourt, emptyEnvelope),
                            coreVerdict);
                })
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
                aliases, legalEntityDefendant);
    }

    private Optional<PressRestriction> getPressRestriction(final CaseDecision caseDecision) {
        return caseDecision.getOffenceDecisions().stream()
                .filter(offenceDecision -> REFER_FOR_COURT_HEARING.equals(offenceDecision.getDecisionType()))
                .map(QueryOffenceDecision::getPressRestriction)
                .filter(Objects::nonNull)
                .findFirst();
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
        if (defendantCourtOptions != null && defendantCourtOptions.getInterpreter() != null) {
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
                ofNullable(defendant)
                        .map(Defendant::getAsn)
                        .orElse(caseFileDefendantDetailsAsn(caseFileDefendantDetails)),
                ofNullable(defendant)
                        .map(Defendant::getPncIdentifier)
                        .orElse(caseFileDefendantDetailsPncIdentifier(caseFileDefendantDetails))
        );

    }

    private static String caseFileDefendantDetailsAsn(final JsonObject caseFileDefendantDetails) {
        return ofNullable(caseFileDefendantDetails)
                .map(defendantDetails -> defendantDetails.getString("asn", null))
                .orElse(null);
    }

    private static String caseFileDefendantDetailsPncIdentifier(final JsonObject caseFileDefendantDetails) {
        return ofNullable(caseFileDefendantDetails)
                .map(defendantDetails -> defendantDetails.getString("pncIdentifier", null))
                .orElse(null);
    }

    private OffenceView createOffenceView(
            final LocalDate referredAt,
            final Offence offenceDetails,
            final JsonObject caseFileDefendantDetails,
            final String rrLabel,
            final NotifiedPleaView notifiedPleaView,
            final Map<String, JsonObject> offenceDefinition,
            final LocalDate convictionDate,
            final CourtCentre convictingCourt,
            final Verdict verdict) {

        final Optional<JsonObject> caseFileOffenceDetailsOptional = ofNullable(caseFileDefendantDetails)
                .flatMap(defendantDetails -> defendantDetails.getJsonArray(OFFENCES_KEY)
                        .getValuesAs(JsonObject.class)
                        .stream()
                        .filter(caseFileOffence -> offenceDetails.getId().toString().equals(caseFileOffence.getString("offenceId", null)))
                        .findFirst());

        final OffenceView.Builder offenceViewBuilder = OffenceView.builder()
                .withId(offenceDetails.getId())
                .withOffenceDefinitionId(referenceDataOffencesService.getOffenceDefinitionId(offenceDefinition.get(offenceDetails.getCjsCode())))
                .withWording(offenceDetails.getWording())
                .withWordingWelsh(offenceDetails.getWordingWelsh())
                .withStartDate(LocalDate.parse(offenceDetails.getStartDate()))
                .withChargeDate(LocalDate.parse(offenceDetails.getChargeDate()))
                .withConvictionDate(convictionDate)
                .withConvictingCourt(convictingCourt)
                .withOrderIndex(offenceDetails.getOffenceSequenceNumber())
                .withNotifiedPlea(notifiedPleaView)
                .withEndDate(caseFileOffenceDetailsOptional.map(caseFileOffenceDetails -> caseFileOffenceDetails.getString("offenceCommittedEndDate", null))
                        .map(LocalDate::parse)
                        .orElse(null))
                .withOffenceFacts(createOffenceFactsView(offenceDetails, caseFileOffenceDetailsOptional))
                .withOffenceDateCode(offenceDetails.getOffenceDateCode())
                .withMaxPenalty(referenceDataOffencesService.getMaxPenalty(offenceDefinition.get(offenceDetails.getCjsCode())));

        if (nonNull(verdict) && nonNull(verdict.getVerdictType())
                && PROVED_SJP_VERDICT_CODE.equalsIgnoreCase(verdict.getVerdictType().getVerdictCode())) {
            offenceViewBuilder.withVerdict(verdict);
        }

        if (nonNull(rrLabel)) {
            offenceViewBuilder.withReportingRestrictions(singletonList(new ReportingRestrictionView(randomUUID(), rrLabel, referredAt)));
        }

        return offenceViewBuilder.build();
    }

    private static OffenceFactsView createOffenceFactsView(final Offence offence, final Optional<JsonObject> caseFileOffenceOptional) {
        if (isNotEmpty(offence.getVehicleRegistrationMark()) || isNotEmpty(offence.getVehicleMake())) {

            final OffenceFactsView.Builder factsBuilder = OffenceFactsView.builder()
                    .withVehicleMake(offence.getVehicleMake())
                    .withVehicleRegistration(offence.getVehicleRegistrationMark());

            caseFileOffenceOptional
                    .map(caseFileOffence -> caseFileOffence.getJsonObject("alcoholRelatedOffence"))
                    .ifPresent(alcoholRelatedFacts -> {
                        if (alcoholRelatedFacts.containsKey("alcoholLevelAmount")) {
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
                .map(DisabilityNeeds::getDisabilityNeeds);

        final String specificRequirements = ofNullable(caseFileDefendantDetails)
                .map(defendantDetails -> defendantDetails.getString("specificRequirements", null))
                .orElse(null);

        return disabiltyStatus.orElse(specificRequirements);


    }

    private static Optional<String> getProsecutionFacts(final List<Offence> offences) {
        return offences
                .stream()
                .map(Offence::getProsecutionFacts)
                .filter(StringUtils::isNotEmpty)
                .findFirst();
    }

    private static LegalEntityDefendant createLegalEntityDefendant(final Defendant defendant) {
        final LegalEntityDetails defendantLegalEntityDetail = defendant.getLegalEntityDetails();
        return new LegalEntityDefendant(createOrganisation(defendantLegalEntityDetail)
        );
    }

    private static Organisation createOrganisation(final LegalEntityDetails defendantLegalEntityDetail) {
        return ofNullable(defendantLegalEntityDetail)
                .map(defendantLEDetail -> Organisation.organisation()
                        .withAddress(createAddress(defendantLEDetail.getAddress()))
                        .withName(defendantLEDetail.getLegalEntityName())
                        .withContact(createContact(defendantLEDetail.getContactDetails())).build())
                .orElse(null);
    }

    private static uk.gov.justice.core.courts.Address createAddress(final Address address) {

        return Optional.ofNullable(address)
                .map(add -> uk.gov.justice.core.courts.Address.address()
                        .withAddress1(add.getAddress1())
                        .withAddress2(add.getAddress2())
                        .withAddress3(add.getAddress3())
                        .withAddress4(add.getAddress4())
                        .withAddress5(add.getAddress5())
                        .withPostcode(add.getPostcode()).build())
                .orElse(null);
    }

    private static ContactNumber createContact(final ContactDetails contactDetails) {

        return Optional.ofNullable(contactDetails)
                .map(cd -> ContactNumber.contactNumber()
                        .withPrimaryEmail(cd.getEmail())
                        .withSecondaryEmail(cd.getEmail2())
                        .withMobile(cd.getBusiness())
                        .withWork(cd.getBusiness()).build())
                .orElse(null);
    }

}
