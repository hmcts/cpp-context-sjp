package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.time.LocalDate.now;
import static java.util.Objects.nonNull;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.REOPENING_PENDING;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_PENDING;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.json.schemas.domains.sjp.commands.CreateCaseApplication;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.ApplicationAddressChanged;
import uk.gov.moj.cpp.sjp.event.ApplicationStatusChanged;
import uk.gov.moj.cpp.sjp.event.CaseApplicationForReopeningRecorded;
import uk.gov.moj.cpp.sjp.event.CaseApplicationRecorded;
import uk.gov.moj.cpp.sjp.event.CaseApplicationRejected;
import uk.gov.moj.cpp.sjp.event.CaseStatDecRecorded;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;

@SuppressWarnings("squid:MethodCyclomaticComplexity")
public class CaseApplicationHandler {
    public static final CaseApplicationHandler INSTANCE = new CaseApplicationHandler();
    private static final String STAT_DEC_CODE = "MC80528";
    private static final String REOPENING_CODE = "MC80524";
    private static final String UNRECOGNIZED_APP = "Unrecognized application type or code";
    private static final String CASE_NOT_COMPLETED = "Case is not completed";
    private static final String CASE_NOT_MANAGED_BY_ATCM = "Case is not managed by Atcm";
    private static final String CASE_HAS_PENDING_APP = "Case has a pending application";
    private static final String CASE_HAS_WRONG_DATE = "Application received data is wrong";
    private static final String CASE_FOR_APPEAL = "Application received is for appeal";
    private static final String APP_CODE_NOT_RECONGNIZED = "Application code not recognised";
    private static final String APP_ID_EXIST = "Application Id already exist";

    private CaseApplicationHandler() {
    }

    public Stream<Object> createCaseApplication(CaseAggregateState state, CreateCaseApplication createCaseApplication) {
        final Optional<CaseApplicationRejected> rejectionReason = validate(state, createCaseApplication);
        if (rejectionReason.isPresent()) {
            return Stream.of(rejectionReason.get());
        }
        final CourtApplication courtApplication = cloneAndAddApplicationReference(createCaseApplication.getCourtApplication());
        final Stream.Builder<Object> sb = Stream.builder();
        sb.add(CaseApplicationRecorded.caseApplicationRecorded().withCaseId(state.getCaseId()).withCourtApplication(courtApplication).build());
        if (STAT_DEC_CODE.equalsIgnoreCase(courtApplication.getType().getCode())) {
            sb.add(CaseStatDecRecorded.caseStatDecRecorded().withApplicant(courtApplication.getApplicant()).withApplicationId(courtApplication.getId()).build());
            sb.add(ApplicationStatusChanged.applicationStatusChanged().withApplicationId(courtApplication.getId()).withStatus(STATUTORY_DECLARATION_PENDING).build());
        } else if (REOPENING_CODE.equalsIgnoreCase(courtApplication.getType().getCode())) {
            sb.add(CaseApplicationForReopeningRecorded.caseApplicationForReopeningRecorded().withApplicant(courtApplication.getApplicant()).withApplicationId(courtApplication.getId()).build());
            sb.add(ApplicationStatusChanged.applicationStatusChanged().withApplicationId(courtApplication.getId()).withStatus(REOPENING_PENDING).build());
        }
        raiseApplicationAddressChanged(sb, state, courtApplication);
        return sb.build();
    }

    private void raiseApplicationAddressChanged(final Stream.Builder<Object> sb, final CaseAggregateState state, final CourtApplication courtApplication) {
        if(nonNull(courtApplication.getSubject().getMasterDefendant())){
            final boolean isDefendantOrg = nonNull(courtApplication.getSubject().getMasterDefendant().getLegalEntityDefendant());
            final Address updatedAddress = getUpdatedAddress(state, courtApplication, isDefendantOrg);
            if (nonNull(updatedAddress)) {
                sb.add(ApplicationAddressChanged.applicationAddressChanged()
                        .withCaseId(state.getCaseId())
                        .withDefendantId(state.getDefendantId())
                        .withTitle(state.getDefendantTitle())
                        .withFirstName(state.getDefendantFirstName())
                        .withLastName(state.getDefendantLastName())
                        .withDriverNumber(state.getDefendantDriverNumber())
                        .withDriverLicenceDetails(state.getDefendantDriverLicenceDetails())
                        .withGender(state.getDefendantGender())
                        .withNationalInsuranceNumber(state.getDefendantNationalInsuranceNumber())
                        .withDateOfBirth(state.getDefendantDateOfBirth())
                        .withEmail(nonNull(state.getDefendantContactDetails()) ? state.getDefendantContactDetails().getEmail() : "")
                        .withEmail2(nonNull(state.getDefendantContactDetails()) ? state.getDefendantContactDetails().getEmail2() : "")
                        .withLegalEntityName(state.getDefendantLegalEntityName())
                        .withContactNumber(state.getDefendantContactDetails())
                        .withAddress(updatedAddress)
                        .withRegion(state.getDefendantRegion())
                        .withAddressUpdateFromApplication("true").build());
            }
        }
    }

    private Address getUpdatedAddress(final CaseAggregateState state, final CourtApplication courtApplication, final boolean isDefendantOrg) {
        Address addressOnApplication = null;
        if (isDefendantOrg) {
            if (nonNull(courtApplication.getSubject().getMasterDefendant().getLegalEntityDefendant().getOrganisation()) &&
                    nonNull(courtApplication.getSubject().getMasterDefendant().getLegalEntityDefendant().getOrganisation().getAddress())){
                addressOnApplication = getAddress(courtApplication.getSubject().getMasterDefendant().getLegalEntityDefendant().getOrganisation().getAddress());
                if(!addressOnApplication.equals(state.getDefendantAddress())) {
                    return addressOnApplication;
                }
            }
        } else {
            if (nonNull(courtApplication.getSubject().getMasterDefendant().getPersonDefendant().getPersonDetails()) &&
                    nonNull(courtApplication.getSubject().getMasterDefendant().getPersonDefendant().getPersonDetails().getAddress())){
                addressOnApplication = getAddress(courtApplication.getSubject().getMasterDefendant().getPersonDefendant().getPersonDetails().getAddress());
                if(!addressOnApplication.equals(state.getDefendantAddress())) {
                    return addressOnApplication;
                }
            }
        }
        return addressOnApplication;
    }

    private Address getAddress(final uk.gov.justice.core.courts.Address address) {
        return new Address(address.getAddress1(), address.getAddress2(), address.getAddress3(), address.getAddress4(), address.getAddress5(),address.getPostcode());
    }

    private Optional<CaseApplicationRejected> validate(final CaseAggregateState state, final CreateCaseApplication createCaseApplication) {
        final CourtApplicationType applicationType = createCaseApplication.getCourtApplication().getType();
        final CourtApplication courtApplication = createCaseApplication.getCourtApplication();

        if (isNullOrEmpty(applicationType.getType()) || isNullOrEmpty(applicationType.getCode())) {
            return Optional.of(CaseApplicationRejected.caseApplicationRejected().withApplicationId(courtApplication.getId().toString()).withCaseId(state.getCaseId().toString()).withDescription(UNRECOGNIZED_APP).build());
        }
        if (!state.isCaseCompleted()) {
            return Optional.of(CaseApplicationRejected.caseApplicationRejected().withApplicationId(courtApplication.getId().toString()).withCaseId(state.getCaseId().toString()).withDescription(CASE_NOT_COMPLETED).build());
        }
        if (state.hasPendingApplication()) {
            return Optional.of(CaseApplicationRejected.caseApplicationRejected().withApplicationId(courtApplication.getId().toString()).withCaseId(state.getCaseId().toString()).withDescription(CASE_HAS_PENDING_APP).build());
        }
        if (state.isCaseReferredForCourtHearing() || !state.isManagedByAtcm()) {
            return Optional.of(CaseApplicationRejected.caseApplicationRejected().withApplicationId(courtApplication.getId().toString()).withCaseId(state.getCaseId().toString()).withDescription(CASE_NOT_MANAGED_BY_ATCM).build());
        }
        if (isFutureDate(createCaseApplication.getCourtApplication().getApplicationReceivedDate())) {
            return Optional.of(CaseApplicationRejected.caseApplicationRejected().withApplicationId(courtApplication.getId().toString()).withCaseId(state.getCaseId().toString()).withDescription(CASE_HAS_WRONG_DATE).build());
        }
        final Optional<CaseApplicationRejected> reject = validateApplication(createCaseApplication, applicationType, state);
        if (reject.isPresent()) {
            return reject;
        }
        return Optional.empty();
    }

    private Optional<CaseApplicationRejected> validateApplication(CreateCaseApplication createCaseApplication, CourtApplicationType applicationType, final CaseAggregateState state) {
        final CourtApplication courtApplication = createCaseApplication.getCourtApplication();
        if (Boolean.TRUE.equals(applicationType.getAppealFlag())) {
            return Optional.of(CaseApplicationRejected.caseApplicationRejected().withApplicationId(courtApplication.getId().toString()).withCaseId(state.getCaseId().toString()).withDescription(CASE_FOR_APPEAL).build());

        }
        if (!STAT_DEC_CODE.equalsIgnoreCase(courtApplication.getType().getCode()) && !REOPENING_CODE.equalsIgnoreCase(courtApplication.getType().getCode())) {
            return Optional.of(CaseApplicationRejected.caseApplicationRejected().withApplicationId(courtApplication.getId().toString()).withCaseId(state.getCaseId().toString()).withDescription(APP_CODE_NOT_RECONGNIZED).build());

        }
        if (createCaseApplication.getApplicationIdExists()) {
            return Optional.of(CaseApplicationRejected.caseApplicationRejected().withApplicationId(courtApplication.getId().toString()).withCaseId(state.getCaseId().toString()).withDescription(APP_ID_EXIST).build());

        }
        return Optional.empty();
    }

    private boolean isFutureDate(final String date) {
        return nonNull(date) && LocalDate.parse(date).isAfter(now());
    }

    private final CourtApplication cloneAndAddApplicationReference(final CourtApplication courtApplication) {
        return CourtApplication.courtApplication()
                .withAllegationOrComplaintEndDate(courtApplication.getAllegationOrComplaintEndDate())
                .withAllegationOrComplaintStartDate(courtApplication.getAllegationOrComplaintStartDate())
                .withApplicant(courtApplication.getApplicant())
                .withApplicationDecisionSoughtByDate(courtApplication.getApplicationDecisionSoughtByDate())
                .withApplicationParticulars(courtApplication.getApplicationParticulars())
                .withApplicationReceivedDate(courtApplication.getApplicationReceivedDate())
                .withApplicationStatus(courtApplication.getApplicationStatus())
                .withCourtApplicationCases(courtApplication.getCourtApplicationCases())
                .withCourtApplicationPayment(courtApplication.getCourtApplicationPayment())
                .withCourtOrder(courtApplication.getCourtOrder())
                .withDefendantASN(courtApplication.getDefendantASN())
                .withHasSummonsSupplied(courtApplication.getHasSummonsSupplied())
                .withId(courtApplication.getId())
                .withJudicialResults(courtApplication.getJudicialResults())
                .withOutOfTimeReasons(courtApplication.getOutOfTimeReasons())
                .withParentApplicationId(courtApplication.getParentApplicationId())
                .withPlea(courtApplication.getPlea())
                .withRemovalReason(courtApplication.getRemovalReason())
                .withRespondents(courtApplication.getRespondents())
                .withSubject(courtApplication.getSubject())
                .withThirdParties(courtApplication.getThirdParties())
                .withType(courtApplication.getType())
                .withVerdict(courtApplication.getVerdict())
                .withApplicationReference(RandomStringUtils.randomAlphanumeric(10).toUpperCase())
                .build();
    }
}






