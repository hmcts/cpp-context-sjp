package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.CASE_ID;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.COME_TO_COURT;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.EMPLOYER_ADDRESS_1;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.EMPLOYER_ADDRESS_2;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.EMPLOYER_ADDRESS_3;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.EMPLOYER_ADDRESS_4;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.EMPLOYER_ADDRESS_5;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.EMPLOYER_NAME;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.EMPLOYER_PHONE;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.EMPLOYER_POSTCODE;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.EMPLOYER_REFERENCE;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.EMPLOYMENT_BENEFITS_CLAIMED;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.EMPLOYMENT_BENEFITS_DEDUCT_PENALTY_PREFERENCE;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.EMPLOYMENT_BENEFITS_TYPE;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.EMPLOYMENT_INCOME_FREQUENCY;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.EMPLOYMENT_INCOME_PAYMENT_AMOUNT;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.EMPLOYMENT_STATUS;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.EMPLOYMENT_STATUS_DETAILS;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.HEARING_LANGUAGE;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.INTERPRETER_LANGUAGE;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.MITIGATION;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.NOT_GUILTY_BECAUSE;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.OUTGOINGS_ACCOMMODATION_AMOUNT;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.OUTGOINGS_CHILD_MAINTENANCE_AMOUNT;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.OUTGOINGS_COUNCIL_TAX_AMOUNT;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.OUTGOINGS_HOUSEHOLD_BILLS_AMOUNT;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.OUTGOINGS_OTHER_AMOUNT;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.OUTGOINGS_OTHER_DESCRIPTION;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.OUTGOINGS_TRAVEL_EXPENSES_AMOUNT;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_ADDRESS_1;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_ADDRESS_2;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_ADDRESS_3;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_ADDRESS_4;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_ADDRESS_5;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_DOB;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_EMAIL;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_FIRST_NAME;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_LAST_NAME;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_NI_NUMBER;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_POSTCODE;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_TELEPHONE_HOME;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_TELEPHONE_MOBILE;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PLEA;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.UNAVAILABILITY;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.WITNESS_DETAILS;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.WITNESS_DISPUTE;

import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;
import org.apache.deltaspike.data.api.SingleResultType;

@Repository
public abstract class OnlinePleaRepository implements EntityRepository<OnlinePlea, UUID> {

    @Inject
    private EntityManager entityManager;

    private static final String INSERT_STATEMENT =
            "INSERT INTO online_plea(case_id, submitted_on, defendant_id) " +
                    " VALUES (?, ?, (SELECT d.id FROM defendant d WHERE d.case_id=?)) " +
                    " ON CONFLICT (case_id) DO NOTHING";

    public void saveOnlinePlea(OnlinePlea onlinePlea) {
        final Query insertStatement = entityManager.createNativeQuery(INSERT_STATEMENT);
        insertStatement.setParameter(1, onlinePlea.getCaseId());
        insertStatement.setParameter(2, onlinePlea.getSubmittedOn());
        insertStatement.setParameter(3, onlinePlea.getCaseId());
        insertStatement.executeUpdate();
        updateOnlinePlea(onlinePlea);
    }

    private void updateOnlinePlea(OnlinePlea onlinePlea) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaUpdate<OnlinePlea> criteria = cb.createCriteriaUpdate(OnlinePlea.class);
        final Root<OnlinePlea> from = criteria.from(OnlinePlea.class);

        getFieldsToUpdate().forEach(field -> criteria.set(field.getDbFieldPath(from), field.convertValue(onlinePlea)));

        criteria.where(cb.equal(CASE_ID.getDbFieldPath(from), onlinePlea.getCaseId()));

        entityManager.createQuery(criteria).executeUpdate();
    }

    /**
     * Hide employment, employer and outgoings.
     */
    @org.apache.deltaspike.data.api.Query(
            value = "SELECT new OnlinePlea(op.caseId, op.pleaDetails, op.defendantId, op.personalDetails, op.submittedOn) FROM OnlinePlea op WHERE op.caseId = :caseId",
            singleResult = SingleResultType.OPTIONAL)
    public abstract OnlinePlea findOnlinePleaWithoutFinances(@QueryParam("caseId") final UUID caseId);

    abstract List<FIELDS> getFieldsToUpdate();

    enum FIELDS {
        CASE_ID(OnlinePlea::getCaseId, "caseId"),

        EMPLOYER_REFERENCE(o -> o.getEmployer().getEmployeeReference(), "employer", "employeeReference"),
        EMPLOYER_NAME(o -> o.getEmployer().getName(), "employer", "name"),
        EMPLOYER_PHONE(o -> o.getEmployer().getPhone(), "employer", "phone"),
        EMPLOYER_ADDRESS_1(o -> o.getEmployer().getAddress().getAddress1(), "employer", "address", "address1"),
        EMPLOYER_ADDRESS_2(o -> o.getEmployer().getAddress().getAddress2(), "employer", "address", "address2"),
        EMPLOYER_ADDRESS_3(o -> o.getEmployer().getAddress().getAddress3(), "employer", "address", "address3"),
        EMPLOYER_ADDRESS_4(o -> o.getEmployer().getAddress().getAddress4(), "employer", "address", "address4"),
        EMPLOYER_ADDRESS_5(o -> o.getEmployer().getAddress().getAddress5(), "employer", "address", "address5"),
        EMPLOYER_POSTCODE(o -> o.getEmployer().getAddress().getPostcode(), "employer", "address", "postcode"),

        EMPLOYMENT_INCOME_PAYMENT_AMOUNT(o -> o.getEmployment().getIncomePaymentAmount(), "employment", "incomePaymentAmount"),
        EMPLOYMENT_INCOME_FREQUENCY(o -> o.getEmployment().getIncomePaymentFrequency(), "employment", "incomePaymentFrequency"),
        EMPLOYMENT_BENEFITS_TYPE(o -> o.getEmployment().getBenefitsType(), "employment", "benefitsType"),
        EMPLOYMENT_BENEFITS_CLAIMED(o -> o.getEmployment().getBenefitsClaimed(), "employment", "benefitsClaimed"),
        EMPLOYMENT_BENEFITS_DEDUCT_PENALTY_PREFERENCE(o -> o.getEmployment().getBenefitsDeductPenaltyPreference(), "employment", "benefitsDeductPenaltyPreference"),
        EMPLOYMENT_STATUS(o -> o.getEmployment().getEmploymentStatus(), "employment", "employmentStatus"),
        EMPLOYMENT_STATUS_DETAILS(o -> o.getEmployment().getEmploymentStatusDetails(), "employment", "employmentStatusDetails"),

        OUTGOINGS_ACCOMMODATION_AMOUNT(o -> o.getOutgoings().getAccommodationAmount(), "outgoings", "accommodationAmount"),
        OUTGOINGS_COUNCIL_TAX_AMOUNT(o -> o.getOutgoings().getCouncilTaxAmount(), "outgoings", "councilTaxAmount"),
        OUTGOINGS_HOUSEHOLD_BILLS_AMOUNT(o -> o.getOutgoings().getHouseholdBillsAmount(), "outgoings", "householdBillsAmount"),
        OUTGOINGS_TRAVEL_EXPENSES_AMOUNT(o -> o.getOutgoings().getTravelExpensesAmount(), "outgoings", "travelExpensesAmount"),
        OUTGOINGS_CHILD_MAINTENANCE_AMOUNT(o -> o.getOutgoings().getChildMaintenanceAmount(), "outgoings", "childMaintenanceAmount"),
        OUTGOINGS_OTHER_DESCRIPTION(o -> o.getOutgoings().getOtherDescription(), "outgoings", "otherDescription"),
        OUTGOINGS_OTHER_AMOUNT(o -> o.getOutgoings().getOtherAmount(), "outgoings", "otherAmount"),

        WITNESS_DISPUTE(o -> o.getPleaDetails().getWitnessDispute(), "pleaDetails", "witnessDispute"),
        WITNESS_DETAILS(o -> o.getPleaDetails().getWitnessDetails(), "pleaDetails", "witnessDetails"),
        UNAVAILABILITY(o -> o.getPleaDetails().getUnavailability(), "pleaDetails", "unavailability"),
        INTERPRETER_LANGUAGE(o -> o.getPleaDetails().getInterpreterLanguage(), "pleaDetails", "interpreterLanguage"),
        HEARING_LANGUAGE(o -> o.getPleaDetails().getSpeakWelsh(), "pleaDetails", "speakWelsh"),

        PERSON_FIRST_NAME(o -> o.getPersonalDetails().getFirstName(), "personalDetails", "firstName"),
        PERSON_LAST_NAME(o -> o.getPersonalDetails().getLastName(), "personalDetails", "lastName"),
        PERSON_TELEPHONE_HOME(o -> o.getPersonalDetails().getHomeTelephone(), "personalDetails", "homeTelephone"),
        PERSON_TELEPHONE_MOBILE(o -> o.getPersonalDetails().getMobile(), "personalDetails", "mobile"),
        PERSON_EMAIL(o -> o.getPersonalDetails().getEmail(), "personalDetails", "email"),
        PERSON_DOB(o -> o.getPersonalDetails().getDateOfBirth(), "personalDetails", "dateOfBirth"),
        PERSON_NI_NUMBER(o -> o.getPersonalDetails().getNationalInsuranceNumber(), "personalDetails", "nationalInsuranceNumber"),
        PERSON_ADDRESS_1(o -> o.getPersonalDetails().getAddress().getAddress1(), "personalDetails", "address", "address1"),
        PERSON_ADDRESS_2(o -> o.getPersonalDetails().getAddress().getAddress2(), "personalDetails", "address", "address2"),
        PERSON_ADDRESS_3(o -> o.getPersonalDetails().getAddress().getAddress3(), "personalDetails", "address", "address3"),
        PERSON_ADDRESS_4(o -> o.getPersonalDetails().getAddress().getAddress4(), "personalDetails", "address", "address4"),
        PERSON_ADDRESS_5(o -> o.getPersonalDetails().getAddress().getAddress5(), "personalDetails", "address", "address5"),
        PERSON_POSTCODE(o -> o.getPersonalDetails().getAddress().getPostcode(), "personalDetails", "address", "postcode"),

        PLEA(o -> o.getPleaDetails().getPlea(), "pleaDetails", "plea"),
        COME_TO_COURT(o -> o.getPleaDetails().getComeToCourt(), "pleaDetails", "comeToCourt"),
        MITIGATION(o -> o.getPleaDetails().getMitigation(), "pleaDetails", "mitigation"),
        NOT_GUILTY_BECAUSE(o -> o.getPleaDetails().getNotGuiltyBecause(), "pleaDetails", "notGuiltyBecause");

        private String[] fieldPath;
        private Function<OnlinePlea, Object> fieldGetter;

        FIELDS(Function<OnlinePlea, Object> fieldGetter, String... fieldPath) {
            this.fieldPath = fieldPath;
            this.fieldGetter = fieldGetter;
        }

        public Path<Object> getDbFieldPath(Root<OnlinePlea> from) {
            Path<Object> objectPath = from.get(fieldPath[0]);
            for (int i = 1; i < fieldPath.length; i++) {
                objectPath = objectPath.get(fieldPath[i]);
            }
            return objectPath;
        }

        public Object convertValue(OnlinePlea onlinePlea) {
            return fieldGetter.apply(onlinePlea);
        }
    }

    public abstract static class FinancialMeansOnlinePleaRepository extends OnlinePleaRepository {
        @Override
        final List<FIELDS> getFieldsToUpdate() {
            return asList(
                    EMPLOYMENT_INCOME_PAYMENT_AMOUNT,
                    EMPLOYMENT_INCOME_FREQUENCY,
                    EMPLOYMENT_BENEFITS_TYPE,
                    EMPLOYMENT_BENEFITS_CLAIMED,
                    EMPLOYMENT_BENEFITS_DEDUCT_PENALTY_PREFERENCE,
                    EMPLOYMENT_STATUS,
                    EMPLOYMENT_STATUS_DETAILS,
                    OUTGOINGS_ACCOMMODATION_AMOUNT,
                    OUTGOINGS_COUNCIL_TAX_AMOUNT,
                    OUTGOINGS_HOUSEHOLD_BILLS_AMOUNT,
                    OUTGOINGS_TRAVEL_EXPENSES_AMOUNT,
                    OUTGOINGS_CHILD_MAINTENANCE_AMOUNT,
                    OUTGOINGS_OTHER_DESCRIPTION,
                    OUTGOINGS_OTHER_AMOUNT
            );
        }
    }

    public abstract static class EmployerOnlinePleaRepository extends OnlinePleaRepository {
        @Override
        final List<FIELDS> getFieldsToUpdate() {
            return asList(
                    EMPLOYER_REFERENCE,
                    EMPLOYER_NAME,
                    EMPLOYER_PHONE,
                    EMPLOYER_ADDRESS_1,
                    EMPLOYER_ADDRESS_2,
                    EMPLOYER_ADDRESS_3,
                    EMPLOYER_ADDRESS_4,
                    EMPLOYER_ADDRESS_5,
                    EMPLOYER_POSTCODE
            );
        }
    }

    public abstract static class TrialOnlinePleaRepository extends OnlinePleaRepository {
        @Override
        final List<FIELDS> getFieldsToUpdate() {
            return asList(
                    CASE_ID,
                    WITNESS_DISPUTE,
                    WITNESS_DETAILS,
                    UNAVAILABILITY
            );
        }
    }

    public abstract static class InterpreterLanguageOnlinePleaRepository extends OnlinePleaRepository {
        @Override
        final List<FIELDS> getFieldsToUpdate() {
            return singletonList(INTERPRETER_LANGUAGE);
        }
    }

    public abstract static class HearingLanguageOnlinePleaRepository extends OnlinePleaRepository {
        @Override
        final List<FIELDS> getFieldsToUpdate() {
            return singletonList(HEARING_LANGUAGE);
        }
    }

    public abstract static class PersonDetailsOnlinePleaRepository extends OnlinePleaRepository {
        @Override
        final List<FIELDS> getFieldsToUpdate() {
            return asList(
                    PERSON_FIRST_NAME,
                    PERSON_LAST_NAME,
                    PERSON_TELEPHONE_HOME,
                    PERSON_TELEPHONE_MOBILE,
                    PERSON_EMAIL,
                    PERSON_DOB,
                    PERSON_NI_NUMBER,
                    PERSON_ADDRESS_1,
                    PERSON_ADDRESS_2,
                    PERSON_ADDRESS_3,
                    PERSON_ADDRESS_4,
                    PERSON_ADDRESS_5,
                    PERSON_POSTCODE
            );
        }
    }

    public abstract static class PleaDetailsRepository extends OnlinePleaRepository {
        @Override
        final List<FIELDS> getFieldsToUpdate() {
            return asList(
                    PLEA,
                    COME_TO_COURT,
                    MITIGATION,
                    NOT_GUILTY_BECAUSE
            );
        }
    }

    @org.apache.deltaspike.data.api.Query(
            value = "SELECT op FROM OnlinePlea op WHERE op.caseId = :caseId AND op.defendantId = :defendantId",
            singleResult = SingleResultType.OPTIONAL)
    public abstract OnlinePlea findOnlinePleaByDefendantIdAndCaseId(@QueryParam("caseId") final UUID caseId, @QueryParam("defendantId") final UUID defendantId);

}