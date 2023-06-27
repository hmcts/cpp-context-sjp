package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.CASE_ID;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.COME_TO_COURT;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.DISABILITY_NEEDS;
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
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.LEGALENTITY_ADDRESS_1;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.LEGALENTITY_ADDRESS_2;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.LEGALENTITY_ADDRESS_3;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.LEGALENTITY_ADDRESS_4;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.LEGALENTITY_ADDRESS_5;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.LEGALENTITY_EMAIL;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.LEGALENTITY_HOME;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.LEGALENTITY_MOBILE;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.LEGALENTITY_NAME;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.LEGALENTITY_POSITION;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.LEGALENTITY_POSTCODE;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.NET_TURNOVER;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.GROSS_TURNOVER;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.NUMBER_OF_EMPLOYEES;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.OUTGOINGS_ACCOMMODATION_AMOUNT;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.OUTGOINGS_CHILD_MAINTENANCE_AMOUNT;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.OUTGOINGS_COUNCIL_TAX_AMOUNT;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.OUTGOINGS_HOUSEHOLD_BILLS_AMOUNT;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.OUTGOINGS_OTHER_AMOUNT;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.OUTGOINGS_OTHER_DESCRIPTION;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.OUTGOINGS_TRAVEL_EXPENSES_AMOUNT;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.OUTSTANDING_FINES;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_ADDRESS_1;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_ADDRESS_2;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_ADDRESS_3;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_ADDRESS_4;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_ADDRESS_5;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_DOB;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_DRIVER_LICENCE_DETAILS;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_DRIVER_NUMBER;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_EMAIL;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_FIRST_NAME;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_LAST_NAME;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_NI_NUMBER;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_POSTCODE;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_TELEPHONE_HOME;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.PERSON_TELEPHONE_MOBILE;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.TRADING_MORE_THAN_TWELVE_MONTHS;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.UNAVAILABILITY;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.WITNESS_DETAILS;
import static uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository.FIELDS.WITNESS_DISPUTE;

import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.LegalEntityFinancialMeans;
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
            value = "SELECT new OnlinePlea(op.caseId, op.pleaDetails, op.defendantId, op.personalDetails, op.submittedOn, op.legalEntityDetails) FROM OnlinePlea op WHERE op.caseId = :caseId",
            singleResult = SingleResultType.OPTIONAL)
    public abstract OnlinePlea findOnlinePleaWithoutFinances(@QueryParam("caseId") final UUID caseId);

    abstract List<FIELDS> getFieldsToUpdate();

    enum FIELDS {
        CASE_ID(OnlinePlea::getCaseId, "caseId"),

        EMPLOYER_REFERENCE(o -> o.getEmployer().getEmployeeReference(), Constants.EMPLOYER, "employeeReference"),
        EMPLOYER_NAME(o -> o.getEmployer().getName(), Constants.EMPLOYER, "name"),
        EMPLOYER_PHONE(o -> o.getEmployer().getPhone(), Constants.EMPLOYER, "phone"),
        EMPLOYER_ADDRESS_1(o -> o.getEmployer().getAddress().getAddress1(), Constants.EMPLOYER, Constants.ADDRESS, Constants.ADDRESS_1),
        EMPLOYER_ADDRESS_2(o -> o.getEmployer().getAddress().getAddress2(), Constants.EMPLOYER, Constants.ADDRESS, Constants.ADDRESS_2),
        EMPLOYER_ADDRESS_3(o -> o.getEmployer().getAddress().getAddress3(), Constants.EMPLOYER, Constants.ADDRESS, Constants.ADDRESS_3),
        EMPLOYER_ADDRESS_4(o -> o.getEmployer().getAddress().getAddress4(), Constants.EMPLOYER, Constants.ADDRESS, Constants.ADDRESS_4),
        EMPLOYER_ADDRESS_5(o -> o.getEmployer().getAddress().getAddress5(), Constants.EMPLOYER, Constants.ADDRESS, Constants.ADDRESS_5),
        EMPLOYER_POSTCODE(o -> o.getEmployer().getAddress().getPostcode(), Constants.EMPLOYER, Constants.ADDRESS, Constants.POSTCODE),

        LEGALENTITY_POSITION(o -> o.getLegalEntityDetails().getPositionOfRepresentative(), Constants.LEGAL_ENTITY, "positionOfRepresentative"),

        LEGALENTITY_NAME(o -> o.getLegalEntityDetails().getLegalEntityName(), Constants.LEGAL_ENTITY, "legalEntityName"),

        LEGALENTITY_HOME(o -> o.getLegalEntityDetails().getHomeTelephone(), Constants.LEGAL_ENTITY, "homeTelephone"),

        LEGALENTITY_MOBILE(o -> o.getLegalEntityDetails().getMobile(), Constants.LEGAL_ENTITY, "mobile"),

        LEGALENTITY_EMAIL(o -> o.getLegalEntityDetails().getEmail(), Constants.LEGAL_ENTITY, "email"),

        LEGALENTITY_ADDRESS_1(o -> ofNullable(o.getLegalEntityDetails().getAddress()).map(Address::getAddress1).orElse(null), Constants.LEGAL_ENTITY, Constants.ADDRESS, Constants.ADDRESS_1),

        LEGALENTITY_ADDRESS_2(o -> ofNullable(o.getLegalEntityDetails().getAddress()).map(Address::getAddress2).orElse(null), Constants.LEGAL_ENTITY, Constants.ADDRESS, Constants.ADDRESS_2),

        LEGALENTITY_ADDRESS_3(o -> ofNullable(o.getLegalEntityDetails().getAddress()).map(Address::getAddress3).orElse(null), Constants.LEGAL_ENTITY, Constants.ADDRESS, Constants.ADDRESS_3),

        LEGALENTITY_ADDRESS_4(o -> ofNullable(o.getLegalEntityDetails().getAddress()).map(Address::getAddress4).orElse(null), Constants.LEGAL_ENTITY, Constants.ADDRESS, Constants.ADDRESS_4),

        LEGALENTITY_ADDRESS_5(o -> ofNullable(o.getLegalEntityDetails().getAddress()).map(Address::getAddress5).orElse(null), Constants.LEGAL_ENTITY, Constants.ADDRESS, Constants.ADDRESS_5),

        LEGALENTITY_POSTCODE(o -> ofNullable(o.getLegalEntityDetails().getAddress()).map(Address::getPostcode).orElse(null), Constants.LEGAL_ENTITY, Constants.ADDRESS, Constants.POSTCODE),

        TRADING_MORE_THAN_TWELVE_MONTHS(o -> ofNullable(o.getLegalEntityDetails().getLegalEntityFinancialMeans()).map(LegalEntityFinancialMeans::getTradingMoreThan12Months).orElse(null), Constants.LEGAL_ENTITY, Constants.LEGAL_ENTITY_FINANCIAL_MEANS, Constants.TRADING_MORE_THAN_12_MONTHS),

        GROSS_TURNOVER(o -> ofNullable(o.getLegalEntityDetails().getLegalEntityFinancialMeans()).map(LegalEntityFinancialMeans::getGrossTurnover).orElse(null), Constants.LEGAL_ENTITY, Constants.LEGAL_ENTITY_FINANCIAL_MEANS, Constants.GROSS_TURNOVER),

        NET_TURNOVER(o -> ofNullable(o.getLegalEntityDetails().getLegalEntityFinancialMeans()).map(LegalEntityFinancialMeans::getNetTurnover).orElse(null), Constants.LEGAL_ENTITY, Constants.LEGAL_ENTITY_FINANCIAL_MEANS, Constants.NET_TURNOVER),

        NUMBER_OF_EMPLOYEES(o -> ofNullable(o.getLegalEntityDetails().getLegalEntityFinancialMeans()).map(LegalEntityFinancialMeans::getNumberOfEmployees).orElse(null), Constants.LEGAL_ENTITY, Constants.LEGAL_ENTITY_FINANCIAL_MEANS, Constants.NUMBER_OF_EMPLOYEES),


        EMPLOYMENT_INCOME_PAYMENT_AMOUNT(o -> o.getEmployment().getIncomePaymentAmount(), Constants.EMPLOYMENT, "incomePaymentAmount"),
        EMPLOYMENT_INCOME_FREQUENCY(o -> o.getEmployment().getIncomePaymentFrequency(), Constants.EMPLOYMENT, "incomePaymentFrequency"),
        EMPLOYMENT_BENEFITS_TYPE(o -> o.getEmployment().getBenefitsType(), Constants.EMPLOYMENT, "benefitsType"),
        EMPLOYMENT_BENEFITS_CLAIMED(o -> o.getEmployment().getBenefitsClaimed(), Constants.EMPLOYMENT, "benefitsClaimed"),
        EMPLOYMENT_BENEFITS_DEDUCT_PENALTY_PREFERENCE(o -> o.getEmployment().getBenefitsDeductPenaltyPreference(), Constants.EMPLOYMENT, "benefitsDeductPenaltyPreference"),
        EMPLOYMENT_STATUS(o -> o.getEmployment().getEmploymentStatus(), Constants.EMPLOYMENT, "employmentStatus"),
        EMPLOYMENT_STATUS_DETAILS(o -> o.getEmployment().getEmploymentStatusDetails(), Constants.EMPLOYMENT, "employmentStatusDetails"),

        OUTGOINGS_ACCOMMODATION_AMOUNT(o -> o.getOutgoings().getAccommodationAmount(), Constants.OUTGOINGS, "accommodationAmount"),
        OUTGOINGS_COUNCIL_TAX_AMOUNT(o -> o.getOutgoings().getCouncilTaxAmount(), Constants.OUTGOINGS, "councilTaxAmount"),
        OUTGOINGS_HOUSEHOLD_BILLS_AMOUNT(o -> o.getOutgoings().getHouseholdBillsAmount(), Constants.OUTGOINGS, "householdBillsAmount"),
        OUTGOINGS_TRAVEL_EXPENSES_AMOUNT(o -> o.getOutgoings().getTravelExpensesAmount(), Constants.OUTGOINGS, "travelExpensesAmount"),
        OUTGOINGS_CHILD_MAINTENANCE_AMOUNT(o -> o.getOutgoings().getChildMaintenanceAmount(), Constants.OUTGOINGS, "childMaintenanceAmount"),
        OUTGOINGS_OTHER_DESCRIPTION(o -> o.getOutgoings().getOtherDescription(), Constants.OUTGOINGS, "otherDescription"),
        OUTGOINGS_OTHER_AMOUNT(o -> o.getOutgoings().getOtherAmount(), Constants.OUTGOINGS, "otherAmount"),

        WITNESS_DISPUTE(o -> o.getPleaDetails().getWitnessDispute(), Constants.PLEA_DETAILS, "witnessDispute"),
        WITNESS_DETAILS(o -> o.getPleaDetails().getWitnessDetails(), Constants.PLEA_DETAILS, "witnessDetails"),
        UNAVAILABILITY(o -> o.getPleaDetails().getUnavailability(), Constants.PLEA_DETAILS, "unavailability"),
        INTERPRETER_LANGUAGE(o -> o.getPleaDetails().getInterpreterLanguage(), Constants.PLEA_DETAILS, "interpreterLanguage"),
        HEARING_LANGUAGE(o -> o.getPleaDetails().getSpeakWelsh(), Constants.PLEA_DETAILS, "speakWelsh"),

        PERSON_FIRST_NAME(o -> o.getPersonalDetails().getFirstName(), Constants.PERSONAL_DETAILS, "firstName"),
        PERSON_LAST_NAME(o -> o.getPersonalDetails().getLastName(), Constants.PERSONAL_DETAILS, "lastName"),
        PERSON_TELEPHONE_HOME(o -> o.getPersonalDetails().getHomeTelephone(), Constants.PERSONAL_DETAILS, "homeTelephone"),
        PERSON_TELEPHONE_MOBILE(o -> o.getPersonalDetails().getMobile(), Constants.PERSONAL_DETAILS, "mobile"),
        PERSON_EMAIL(o -> o.getPersonalDetails().getEmail(), Constants.PERSONAL_DETAILS, "email"),
        PERSON_DOB(o -> o.getPersonalDetails().getDateOfBirth(), Constants.PERSONAL_DETAILS, "dateOfBirth"),
        PERSON_NI_NUMBER(o -> o.getPersonalDetails().getNationalInsuranceNumber(), Constants.PERSONAL_DETAILS, "nationalInsuranceNumber"),
        PERSON_DRIVER_NUMBER(o -> o.getPersonalDetails().getDriverNumber(), Constants.PERSONAL_DETAILS, "driverNumber"),
        PERSON_DRIVER_LICENCE_DETAILS(o -> o.getPersonalDetails().getDriverLicenceDetails(), Constants.PERSONAL_DETAILS, "driverLicenceDetails"),
        PERSON_ADDRESS_1(o -> ofNullable(o.getPersonalDetails().getAddress()).map(Address::getAddress1).orElse(null), Constants.PERSONAL_DETAILS, Constants.ADDRESS, Constants.ADDRESS_1),
        PERSON_ADDRESS_2(o -> ofNullable(o.getPersonalDetails().getAddress()).map(Address::getAddress2).orElse(null), Constants.PERSONAL_DETAILS, Constants.ADDRESS, Constants.ADDRESS_2),
        PERSON_ADDRESS_3(o -> ofNullable(o.getPersonalDetails().getAddress()).map(Address::getAddress3).orElse(null), Constants.PERSONAL_DETAILS, Constants.ADDRESS, Constants.ADDRESS_3),
        PERSON_ADDRESS_4(o -> ofNullable(o.getPersonalDetails().getAddress()).map(Address::getAddress4).orElse(null), Constants.PERSONAL_DETAILS, Constants.ADDRESS, Constants.ADDRESS_4),
        PERSON_ADDRESS_5(o -> ofNullable(o.getPersonalDetails().getAddress()).map(Address::getAddress5).orElse(null), Constants.PERSONAL_DETAILS, Constants.ADDRESS, Constants.ADDRESS_5),
        PERSON_POSTCODE(o -> ofNullable(o.getPersonalDetails().getAddress()).map(Address::getPostcode).orElse(null), Constants.PERSONAL_DETAILS, Constants.ADDRESS, Constants.POSTCODE),

        COME_TO_COURT(o -> ofNullable(o.getPleaDetails()).map(OnlinePlea.PleaDetails::getComeToCourt).orElse(null), Constants.PLEA_DETAILS, "comeToCourt"),

        OUTSTANDING_FINES(o -> ofNullable(o.getPleaDetails()).map(OnlinePlea.PleaDetails::getOutstandingFines).orElse(null), Constants.PLEA_DETAILS, "outstandingFines"),

        DISABILITY_NEEDS(o -> ofNullable(o.getPleaDetails()).map(OnlinePlea.PleaDetails::getDisabilityNeeds).orElse(null), Constants.PLEA_DETAILS, "disabilityNeeds");

        public static final String ADDRESS = "address";
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

        private static class Constants {
            public static final String ADDRESS_1 = "address1";
            public static final String ADDRESS_2 = "address2";
            public static final String ADDRESS_3 = "address3";
            public static final String ADDRESS_4 = "address4";
            public static final String ADDRESS_5 = "address5";
            public static final String POSTCODE = "postcode";
            public static final String ADDRESS = "address";
            public static final String EMPLOYER = "employer";
            public static final String LEGAL_ENTITY = "legalEntityDetails";
            public static final String LEGAL_ENTITY_FINANCIAL_MEANS = "legalEntityFinancialMeans";
            public static final String TRADING_MORE_THAN_12_MONTHS = "tradingMoreThan12Months";
            public static final String GROSS_TURNOVER = "grossTurnover";
            public static final String NET_TURNOVER = "netTurnover";
            public static final String NUMBER_OF_EMPLOYEES = "numberOfEmployees";
            public static final String EMPLOYMENT = "employment";
            public static final String OUTGOINGS = "outgoings";
            public static final String PLEA_DETAILS = "pleaDetails";
            public static final String PERSONAL_DETAILS = "personalDetails";
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
                    PERSON_POSTCODE,
                    PERSON_DRIVER_NUMBER,
                    PERSON_DRIVER_LICENCE_DETAILS,
                    DISABILITY_NEEDS
            );
        }
    }

    public abstract static class PleaDetailsRepository extends OnlinePleaRepository {
        @Override
        final List<FIELDS> getFieldsToUpdate() {
            return asList(
                    COME_TO_COURT
            );
        }
    }

    public abstract static class OutstandingFinesOnlinePleaRepository extends OnlinePleaRepository {
        @Override
        final List<FIELDS> getFieldsToUpdate() {
            return asList(
                    OUTSTANDING_FINES
            );
        }
    }

    public abstract static class LegalEntityDetailsOnlinePleaRepository extends OnlinePleaRepository {
        @Override
        final List<FIELDS> getFieldsToUpdate() {
            return asList(
                    LEGALENTITY_NAME,
                    LEGALENTITY_HOME,
                    LEGALENTITY_MOBILE,
                    LEGALENTITY_EMAIL,
                    LEGALENTITY_ADDRESS_1,
                    LEGALENTITY_ADDRESS_2,
                    LEGALENTITY_ADDRESS_3,
                    LEGALENTITY_ADDRESS_4,
                    LEGALENTITY_ADDRESS_5,
                    LEGALENTITY_POSTCODE,
                    DISABILITY_NEEDS,
                    TRADING_MORE_THAN_TWELVE_MONTHS,
                    NUMBER_OF_EMPLOYEES,
                    GROSS_TURNOVER,
                    NET_TURNOVER,
                    LEGALENTITY_POSITION
            );
        }
    }

    @org.apache.deltaspike.data.api.Query(
            value = "SELECT op FROM OnlinePlea op WHERE op.caseId = :caseId AND op.defendantId = :defendantId",
            singleResult = SingleResultType.OPTIONAL)
    public abstract OnlinePlea findOnlinePleaByDefendantIdAndCaseId(@QueryParam("caseId") final UUID caseId, @QueryParam("defendantId") final UUID defendantId);

}
