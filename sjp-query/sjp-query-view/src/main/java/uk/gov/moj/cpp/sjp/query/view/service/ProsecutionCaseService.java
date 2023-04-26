package uk.gov.moj.cpp.sjp.query.view.service;

import static java.time.LocalDate.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.core.courts.ContactNumber.contactNumber;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.LegalEntityDefendant.legalEntityDefendant;
import static uk.gov.justice.core.courts.NotifiedPlea.notifiedPlea;
import static uk.gov.justice.core.courts.NotifiedPleaValue.NOTIFIED_GUILTY;
import static uk.gov.justice.core.courts.NotifiedPleaValue.NOTIFIED_NOT_GUILTY;
import static uk.gov.justice.core.courts.NotifiedPleaValue.NO_NOTIFICATION;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAlias;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.NotifiedPlea;
import uk.gov.justice.core.courts.NotifiedPleaValue;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceFacts;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.model.prosecution.helpers.DefendantTitleParser;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.InterpreterDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaDetailRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;

public class ProsecutionCaseService {

    private static final String OFFENCES_KEY = "offences";
    private static final String WELSH_LANGUAGE_CODE = "W";
    public static final String INACTIVE = "INACTIVE";

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private EmployerService employerService;

    @Inject
    private ReferenceDataOffencesService referenceDataOffencesService;

    @Inject
    private ProsecutionCaseFileService prosecutionCaseFileService;

    @Inject
    private OnlinePleaDetailRepository onlinePleaDetailRepository;

    /**
     * Finds a case by id and returns it in the prosecutionCase format used by CC.
     *
     * @param caseId id of the case
     * @return prosecution case view
     */
    public ProsecutionCase findProsecutionCase(final UUID caseId) {
        return getProsecutionCaseView(caseRepository.findBy(caseId));
    }

    private ProsecutionCase getProsecutionCaseView(final CaseDetail caseDetail) {
        if (caseDetail != null) {
            final List<OnlinePleaDetail> onlinePleaDetails = caseDetail.getOnlinePleaReceived() ?
                    onlinePleaDetailRepository.findByCaseIdAndDefendantIdAndAocpPleaIsNull(caseDetail.getId(), caseDetail.getDefendant().getId()) : null;
            final Optional<JsonObject> prosecutionCaseFileOptional = prosecutionCaseFileService.getCaseFileDetails(caseDetail.getId());
            final JsonObject caseFileDefendantDetails = prosecutionCaseFileOptional.map(this::getCaseFileDefendant).orElse(null);
            return toProsecutionCaseView(caseDetail, onlinePleaDetails, prosecutionCaseFileOptional.orElse(null), caseFileDefendantDetails);
        }
        return null;
    }

    private ProsecutionCase toProsecutionCaseView(final CaseDetail caseDetail,
                                                  final List<OnlinePleaDetail> onlinePleaDetails,
                                                  final JsonObject prosecutionCaseFile,
                                                  final JsonObject prosecutionCaseFileDefendant) {

        final Set<String> offenceCodes = caseDetail.getDefendant().getOffences()
                .stream()
                .map(OffenceDetail::getCode)
                .collect(toSet());

        final Map<String, JsonObject> offenceDefinitionsByOffenceCode = referenceDataOffencesService.getOffenceDefinitionsByOffenceCode(offenceCodes, now());
        final JsonObject prosecutor = referenceDataService.getProsecutor(caseDetail.getProsecutingAuthority());
        final Optional<Employer> employer = employerService.getEmployer(caseDetail.getDefendant().getId());

        final Optional<JsonObject> defendantSelfDefinedInformationOptional = getSelfDefinedInformation(prosecutionCaseFileDefendant);

        final String nationalityId = getNationalityId(defendantSelfDefinedInformationOptional);

        final String pleaMitigation = getPleaMitigation(onlinePleaDetails);

        final Optional<String> prosecutionFacts = getProsecutionFacts(caseDetail);
        final String prosecutionFactsWelsh = getProsecutionFactsWelsh(prosecutionCaseFileDefendant);

        final Defendant defendantView = createDefendantView(caseDetail, prosecutionCaseFileDefendant,
                employer, nationalityId, pleaMitigation, offenceDefinitionsByOffenceCode);

        String caseURN = null;
        String prosecutingAuthorityReference = null;

        if (prosecutor.getBoolean("policeFlag", false)) {
            caseURN = caseDetail.getUrn();
        } else {
            prosecutingAuthorityReference = caseDetail.getUrn();
        }

        final ProsecutionCaseIdentifier prosecutionCaseIdentifier =
                ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityId(UUID.fromString(prosecutor.getString("id")))
                        .withProsecutionAuthorityCode(prosecutor.getString("shortName"))
                        .withProsecutionAuthorityReference(prosecutingAuthorityReference)
                        .withCaseURN(caseURN)
                        .build();

        final String originatingOrganisation = getOriginatingOrganisation(prosecutionCaseFile);

        return prosecutionCase()
                .withId(caseDetail.getId())
                .withInitiationCode(InitiationCode.J)
                .withStatementOfFacts(prosecutionFacts.orElse(null))
                .withStatementOfFactsWelsh(prosecutionFactsWelsh)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier)
                .withDefendants(singletonList(defendantView))
                .withOriginatingOrganisation(originatingOrganisation)
                .withCaseStatus(INACTIVE)
                .build();
    }

    private String getOriginatingOrganisation(final JsonObject prosecutionCaseFile) {
        return ofNullable(prosecutionCaseFile)
                .map(caseFile -> caseFile.getString("originatingOrganisation", null))
                .orElse(null);
    }

    private String getProsecutionFactsWelsh(final JsonObject prosecutionCaseFileDefendant) {
        return ofNullable(prosecutionCaseFileDefendant)
                .map(defendantDetails -> defendantDetails.getJsonArray(OFFENCES_KEY))
                .map(defendantOffences -> defendantOffences.getJsonObject(0).getString("statementOfFactsWelsh", null))
                .orElse(null);
    }

    private Defendant createDefendantView(final CaseDetail caseDetail, final JsonObject prosecutionCaseFileDefendant, final Optional<Employer> employer,
                                          final String nationalityId, final String pleaMitigation,
                                          final Map<String, JsonObject> offenceDefinitionsByOffenceCode) {

        final DefendantDetail defendantDetail = caseDetail.getDefendant();

        final List<Offence> offenceViews = defendantDetail.getOffences()
                .stream()
                .map(offence -> createOffenceView(offence, prosecutionCaseFileDefendant, offenceDefinitionsByOffenceCode))
                .collect(toList());
        final List<DefendantAlias> aliases = getDefendantAliases(prosecutionCaseFileDefendant);

        final Defendant.Builder defendantBuilder = defendant();
        defendantBuilder
                .withId(defendantDetail.getId())
                .withMasterDefendantId(defendantDetail.getId())
                .withProsecutionCaseId(caseDetail.getId())
                .withMitigation(pleaMitigation)
                .withNumberOfPreviousConvictionsCited(defendantDetail.getNumPreviousConvictions())
                .withOffences(offenceViews)
                .withAliases(aliases)
                .withCourtProceedingsInitiated(ZonedDateTime.now());

        if (nonNull(defendantDetail.getPersonalDetails())) {
            final PersonDefendant personDefendantView = createPersonDefendantView(defendantDetail, prosecutionCaseFileDefendant, employer, nationalityId);
            defendantBuilder.withPersonDefendant(personDefendantView);
        }

        if (nonNull(defendantDetail.getLegalEntityDetails())) {
            defendantBuilder.withLegalEntityDefendant(legalEntityDefendant()
                    .withOrganisation(Organisation.organisation()
                            .withName(defendantDetail.getLegalEntityDetails().getLegalEntityName())
                            .build()).build());
        }

        return defendantBuilder.build();
    }

    private List<DefendantAlias> getDefendantAliases(final JsonObject prosecutionCaseFileDefendant) {
        return ofNullable(prosecutionCaseFileDefendant)
                .map(pcfDefendantDetails -> {
                    if (prosecutionCaseFileDefendant.containsKey("individualAliases")) {
                        final JsonArray individualAliases = prosecutionCaseFileDefendant.getJsonArray("individualAliases");
                        return individualAliases
                                .getValuesAs(JsonObject.class).stream()
                                .map(this::toDefendantAliasView)
                                .collect(toList());
                    }
                    return null;
                })
                .filter(aliasViews -> !aliasViews.isEmpty())
                .orElse(null);
    }

    private DefendantAlias toDefendantAliasView(final JsonObject individualAlias) {
        return DefendantAlias.defendantAlias()
                .withFirstName(individualAlias.getString("firstName", null))
                .withLastName(individualAlias.getString("lastName", null))
                .withTitle(individualAlias.getString("title", null))
                .withMiddleName(individualAlias.getString("givenName2", null))
                .withLegalEntityName(individualAlias.getString("legalEntityName", null))
                .build();
    }

    private Offence createOffenceView(final OffenceDetail offence, final JsonObject prosecutionCaseFileDefendant,
                                      final Map<String, JsonObject> offenceDefinitionsByOffenceCode) {

        final Optional<JsonObject> prosecutionCaseFileOffenceDetails = getProsecutionCaseFileOffenceDetails(offence, prosecutionCaseFileDefendant);

        final Optional<JsonObject> offenceRefData = referenceDataService.getOffenceData(offence.getCode());


        return Offence.offence()
                .withId(offence.getId())
                .withOffenceDefinitionId(fromString(offenceDefinitionsByOffenceCode.get(offence.getCode()).getString("offenceId")))
                .withWording(offence.getWording())
                .withWordingWelsh(offence.getWordingWelsh())
                .withStartDate(ofNullable(offence.getStartDate()).map(Object::toString).orElse(null))
                .withChargeDate(ofNullable(offence.getChargeDate()).map(Object::toString).orElse(null))
                .withConvictionDate(ofNullable(offence.getConvictionDate()).map(Object::toString).orElse(null))
                .withOrderIndex(offence.getOrderIndex())
                .withNotifiedPlea(createNotifiedPleaView(offence))
                .withEndDate(getOffenceCommittedEndDate(prosecutionCaseFileOffenceDetails))
                .withOffenceFacts(createOffenceFactsView(offence, prosecutionCaseFileOffenceDetails))
                .withOffenceDateCode(offence.getLibraOffenceDateCode())
                .withOffenceCode(offence.getCode())
                .withCount(
                        ofNullable(offence.getSequenceNumber())
                                .map(e -> e > 0 ? e - 1 : 0).orElse(0))
                .withOffenceTitle(getOffenceRefData(offenceRefData.orElse(null), "title", "welshTitle").orElse(null))
                .withOffenceLegislation(getOffenceRefData(offenceRefData.orElse(null), "legislation", "welshLegislation").orElse(null))
                .build();
    }

    private Optional<String> getOffenceRefData(final JsonObject offenceData, final String fieldName, final String welshFieldName) {
        if (nonNull(offenceData)) {
            final String value = offenceData.getString(fieldName, null);
            final String welshValue = offenceData.getString(welshFieldName, null);
            return ofNullable(nonNull(value) ? value : welshValue);
        }
        return empty();
    }

    private NotifiedPlea createNotifiedPleaView(final OffenceDetail offence) {

        return ofNullable(offence.getPlea())
                .map(plea -> notifiedPlea()
                        .withNotifiedPleaDate(ofNullable(offence.getPleaDate().toLocalDate()).map(Object::toString).orElse(null))
                        .withOffenceId(offence.getId())
                        .withNotifiedPleaValue(getNotifiedPlea(plea))
                        .build())
                .orElse(
                        notifiedPlea()
                                .withNotifiedPleaDate(now().toString())
                                .withOffenceId(offence.getId())
                                .withNotifiedPleaValue(NO_NOTIFICATION)
                                .build());
    }

    private NotifiedPleaValue getNotifiedPlea(final PleaType pleaType) {
        switch (pleaType) {
            case NOT_GUILTY:
                return NOTIFIED_NOT_GUILTY;
            case GUILTY:
            case GUILTY_REQUEST_HEARING:
                return NOTIFIED_GUILTY;
            default:
                throw new UnsupportedOperationException("Notified plea not defined for " + pleaType);
        }
    }

    private OffenceFacts createOffenceFactsView(final OffenceDetail offence,
                                                final Optional<JsonObject> prosecutionCaseFileOffenceDetails) {
        if (isNotEmpty(offence.getVehicleRegistrationMark()) || isNotEmpty(offence.getVehicleMake())) {

            final OffenceFacts.Builder factsBuilder = OffenceFacts.offenceFacts()
                    .withVehicleMake(offence.getVehicleMake())
                    .withVehicleRegistration(offence.getVehicleRegistrationMark());

            prosecutionCaseFileOffenceDetails
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

    private String getOffenceCommittedEndDate(final Optional<JsonObject> prosecutionCaseFileOffenceDetails) {
        return prosecutionCaseFileOffenceDetails.map(caseFileOffenceDetails -> caseFileOffenceDetails.getString("offenceCommittedEndDate", null))
                .orElse(null);
    }

    private Optional<JsonObject> getProsecutionCaseFileOffenceDetails(final OffenceDetail offence, final JsonObject prosecutionCaseFileDefendant) {
        return ofNullable(prosecutionCaseFileDefendant)
                .flatMap(defendantDetails -> defendantDetails.getJsonArray(OFFENCES_KEY)
                        .getValuesAs(JsonObject.class)
                        .stream()
                        .filter(caseFileOffence -> offence.getId().toString().equals(caseFileOffence.getString("offenceId", null)))
                        .findFirst());
    }

    private PersonDefendant createPersonDefendantView(final DefendantDetail defendantDetail,
                                                      final JsonObject prosecutionCaseFileDefendant,
                                                      final Optional<Employer> employer,
                                                      final String nationalityId) {
        final PersonalDetails defendantPersonalDetails = defendantDetail.getPersonalDetails();
        final String interpreter = ofNullable(defendantDetail.getInterpreter()).map(InterpreterDetail::getLanguage).orElse(null);
        final Person personDetailsView = createPersonDetailsView(defendantDetail, interpreter,
                defendantDetail.getDisabilityNeeds(), nationalityId,
                ofNullable(prosecutionCaseFileDefendant));

        final Organisation employerOrganisationView = createEmployerOrganisationView(employer);

        return personDefendant()
                .withPersonDetails(personDetailsView)
                .withEmployerOrganisation(employerOrganisationView)
                .withDriverNumber(defendantPersonalDetails.getDriverNumber())
                .withArrestSummonsNumber(ofNullable(defendantDetail.getAsn()).orElse(caseFileDefendantDetailsAsn(prosecutionCaseFileDefendant)))
                .build();
    }

    private String caseFileDefendantDetailsAsn(final JsonObject prosecutionCaseFileDefendant) {
        return ofNullable(prosecutionCaseFileDefendant)
                .map(defendantDetails -> defendantDetails.getString("asn", null))
                .orElse(null);
    }

    private Organisation createEmployerOrganisationView(final Optional<Employer> employer) {
        return employer
                .map(Employer::getName)
                .map(employerName -> Organisation
                        .organisation()
                        .withName(employerName)
                        .withAddress(createAddressView(employer.get().getAddress()))
                        .withContact(contactNumber().withWork(employer.get().getPhone()).build())
                        .build())
                .orElse(null);
    }

    private Address createAddressView(final uk.gov.moj.cpp.sjp.domain.Address address) {
        return ofNullable(address)
                .map(adr -> Address.address()
                        .withAddress1(adr.getAddress1())
                        .withAddress2(adr.getAddress2())
                        .withAddress3(adr.getAddress3())
                        .withAddress4(adr.getAddress4())
                        .withAddress5(adr.getAddress5())
                        .withPostcode(adr.getPostcode()).build())
                .orElse(null);
    }

    private Address createAddressView(final uk.gov.moj.cpp.sjp.persistence.entity.Address address) {
        return ofNullable(address)
                .map(adr -> Address.address()
                        .withAddress1(adr.getAddress1())
                        .withAddress2(adr.getAddress2())
                        .withAddress3(adr.getAddress3())
                        .withAddress4(adr.getAddress4())
                        .withAddress5(adr.getAddress5())
                        .withPostcode(adr.getPostcode()).build())
                .orElse(null);
    }

    private Person createPersonDetailsView(final DefendantDetail defendantDetail, final String interpreter, final String disabilityNeeds,
                                           final String nationalityId, final Optional<JsonObject> pcfDefendantDetails) {

        final PersonalDetails defendantPersonalDetails = defendantDetail.getPersonalDetails();
        final Optional<JsonObject> pcfDefendantPersonalInformation = pcfDefendantDetails.flatMap(this::getProsecutionCaseFileDefendantPersonalInformation);
        return Person.person()
                .withTitle(DefendantTitleParser.parse(defendantPersonalDetails.getTitle()))
                .withFirstName(defendantPersonalDetails.getFirstName())
                .withLastName(defendantPersonalDetails.getLastName())
                .withDateOfBirth(Optional.ofNullable(defendantPersonalDetails.getDateOfBirth()).map(e -> e.toString()).orElse(null))
                .withGender(resolveGender(defendantPersonalDetails.getGender()))
                .withInterpreterLanguageNeeds(interpreter)
                .withDisabilityStatus(disabilityNeeds)
                .withNationalityId(Optional.ofNullable(nationalityId).map(e -> UUID.fromString(nationalityId)).orElse(null))
                .withDocumentationLanguageNeeds(pcfDefendantPersonalInformation
                        .map(defendantDetails -> defendantDetails.getString("documentationLanguage", null))
                        .map(documentationLanguage -> WELSH_LANGUAGE_CODE.equals(documentationLanguage) ? HearingLanguage.WELSH : HearingLanguage.ENGLISH)
                        .orElse(HearingLanguage.ENGLISH))
                .withNationalInsuranceNumber(defendantPersonalDetails.getNationalInsuranceNumber())
                .withOccupation(pcfDefendantPersonalInformation.map(personalInformation -> personalInformation.getString("occupation", null)).orElse(null))
                .withOccupationCode(pcfDefendantPersonalInformation.map(personalInformation -> personalInformation.getInt("occupationCode", 0))
                        .map(String::valueOf)
                        .orElse(null))
                .withSpecificRequirements(createSpecialRequirement(pcfDefendantDetails, disabilityNeeds))
                .withAddress(createAddressView(defendantDetail.getAddress()))
                .withContact(createDefendantContactView(defendantDetail, pcfDefendantPersonalInformation))
                .build();
    }

    private Gender resolveGender(final uk.gov.justice.json.schemas.domains.sjp.Gender gender) {
        if (gender == uk.gov.justice.json.schemas.domains.sjp.Gender.FEMALE) {
            return Gender.FEMALE;
        } else if (gender == uk.gov.justice.json.schemas.domains.sjp.Gender.MALE) {
            return Gender.MALE;
        }
        return Gender.NOT_SPECIFIED;
    }

    private ContactNumber createDefendantContactView(final DefendantDetail defendantDetail, final Optional<JsonObject> caseFileDefendantPersonalInformation) {
        final ContactDetails defendantContactDetails = defendantDetail.getContactDetails();
        return contactNumber()
                .withHome(ofNullable(defendantContactDetails).map(e -> defendantContactDetails.getHome()).orElse(null))
                .withWork(caseFileDefendantPersonalInformation.map(personalInformation -> personalInformation.getString("work", null)).orElse(null))
                .withMobile(ofNullable(defendantContactDetails).map(e -> defendantContactDetails.getMobile()).orElse(null))
                .withPrimaryEmail(ofNullable(defendantContactDetails).map(e -> defendantContactDetails.getEmail()).orElse(null))
                .withSecondaryEmail(caseFileDefendantPersonalInformation.map(personalInformation -> personalInformation.getString("secondaryEmail", null)).orElse(null))
                .build();
    }

    private String createSpecialRequirement(final Optional<JsonObject> caseFileDefendantDetails, final String disabilityNeeds) {
        final Optional<String> disabiltyStatus = ofNullable(disabilityNeeds);

        final String specificRequirements = caseFileDefendantDetails
                .map(defendantDetails -> defendantDetails.getString("specificRequirements", null))
                .orElse(null);

        return disabiltyStatus.orElse(specificRequirements);

    }

    private Optional<JsonObject> getProsecutionCaseFileDefendantPersonalInformation(final JsonObject prosecutionCaseFileDefendant) {
        return ofNullable(prosecutionCaseFileDefendant)
                .map(defendantDetails -> (JsonObject) defendantDetails.getOrDefault("personalInformation", createObjectBuilder().build()));
    }

    private Optional<JsonObject> getSelfDefinedInformation(final JsonObject prosecutionCaseFileDefendant) {
        return ofNullable(prosecutionCaseFileDefendant)
                .map(defendantDetails -> (JsonObject) defendantDetails.getOrDefault("selfDefinedInformation", createObjectBuilder().build()));
    }

    private String getNationalityId(final Optional<JsonObject> defendantSelfDefinedInformationOptional) {
        return defendantSelfDefinedInformationOptional
                .map(selfDefinedInformation -> selfDefinedInformation.getString("nationality", null))
                .flatMap(selfDefinedNationality -> referenceDataService.getNationality(selfDefinedNationality))
                .map(referenceDataNationality -> referenceDataNationality.getString("id"))
                .orElse(null);
    }

    private String getPleaMitigation(final List<OnlinePleaDetail> onlinePleaDetails) {
        return ofNullable(onlinePleaDetails)
                .orElse(emptyList())
                .stream()
                .map(OnlinePleaDetail::getMitigation)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Optional<String> getProsecutionFacts(final CaseDetail caseDetail) {
        return caseDetail.getDefendant().getOffences()
                .stream()
                .map(OffenceDetail::getProsecutionFacts)
                .filter(StringUtils::isNotEmpty)
                .findFirst();
    }

    private JsonObject getCaseFileDefendant(final JsonObject caseFile) {
        return caseFile.getJsonArray("defendants").getJsonObject(0);
    }

}
