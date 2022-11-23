package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository.FIELDS.CASE_ID;
import static uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository.FIELDS.PERSON_ADDRESS_1;
import static uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository.FIELDS.PERSON_ADDRESS_2;
import static uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository.FIELDS.PERSON_ADDRESS_3;
import static uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository.FIELDS.PERSON_ADDRESS_4;
import static uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository.FIELDS.PERSON_ADDRESS_5;
import static uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository.FIELDS.PERSON_DOB;
import static uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository.FIELDS.PERSON_DRIVER_LICENCE_DETAILS;
import static uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository.FIELDS.PERSON_DRIVER_NUMBER;
import static uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository.FIELDS.PERSON_EMAIL;
import static uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository.FIELDS.PERSON_FIRST_NAME;
import static uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository.FIELDS.PERSON_LAST_NAME;
import static uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository.FIELDS.PERSON_NI_NUMBER;
import static uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository.FIELDS.PERSON_POSTCODE;
import static uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository.FIELDS.PERSON_TELEPHONE_HOME;
import static uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository.FIELDS.PERSON_TELEPHONE_MOBILE;

import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.AocpOnlinePlea;

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
public abstract class AocpOnlinePleaRepository implements EntityRepository<AocpOnlinePlea, UUID> {

    @Inject
    private EntityManager entityManager;

    private static final String PERSONAL_DETAILS = "personalDetails";
    private static final String ADDRESS = "address";

    private static final String INSERT_STATEMENT =
            "INSERT INTO aocp_online_plea(case_id, submitted_on, defendant_id, aocp_accepted) " +
                    " VALUES (?, ?, (SELECT d.id FROM defendant d WHERE d.case_id=?), ?) " +
                    " ON CONFLICT (case_id) DO NOTHING";

    public void saveOnlinePlea(AocpOnlinePlea aocpOnlinePlea) {
        final Query insertStatement = entityManager.createNativeQuery(INSERT_STATEMENT);
        insertStatement.setParameter(1, aocpOnlinePlea.getCaseId());
        insertStatement.setParameter(2, aocpOnlinePlea.getSubmittedOn());
        insertStatement.setParameter(3, aocpOnlinePlea.getCaseId());
        insertStatement.setParameter(4, Boolean.TRUE.equals(aocpOnlinePlea.getAocpAccepted()));
        insertStatement.executeUpdate();
        updateOnlinePlea(aocpOnlinePlea);
    }

    private void updateOnlinePlea(AocpOnlinePlea aocpOnlinePlea) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaUpdate<AocpOnlinePlea> criteria = cb.createCriteriaUpdate(AocpOnlinePlea.class);
        final Root<AocpOnlinePlea> from = criteria.from(AocpOnlinePlea.class);

        getFieldsToUpdate().forEach(field -> criteria.set(field.getDbFieldPath(from), field.convertValue(aocpOnlinePlea)));

        criteria.where(cb.equal(CASE_ID.getDbFieldPath(from), aocpOnlinePlea.getCaseId()));

        entityManager.createQuery(criteria).executeUpdate();
    }

    /**
     * Hide employment, employer and outgoings.
     */
    @org.apache.deltaspike.data.api.Query(
            value = "SELECT new AocpOnlinePlea(op.caseId, op.defendantId, op.personalDetails, op.submittedOn, op.aocpAccepted) FROM AocpOnlinePlea op WHERE op.caseId = :caseId",
            singleResult = SingleResultType.OPTIONAL)
    public abstract AocpOnlinePlea findAocpPleaByCaseId(@QueryParam("caseId") final UUID caseId);

    abstract List<FIELDS> getFieldsToUpdate();

    enum FIELDS {
        CASE_ID(AocpOnlinePlea::getCaseId, "caseId"),

        PERSON_FIRST_NAME(o -> o.getPersonalDetails().getFirstName(), PERSONAL_DETAILS, "firstName"),
        PERSON_LAST_NAME(o -> o.getPersonalDetails().getLastName(), PERSONAL_DETAILS, "lastName"),
        PERSON_TELEPHONE_HOME(o -> o.getPersonalDetails().getHomeTelephone(), PERSONAL_DETAILS, "homeTelephone"),
        PERSON_TELEPHONE_MOBILE(o -> o.getPersonalDetails().getMobile(), PERSONAL_DETAILS, "mobile"),
        PERSON_EMAIL(o -> o.getPersonalDetails().getEmail(), PERSONAL_DETAILS, "email"),
        PERSON_DOB(o -> o.getPersonalDetails().getDateOfBirth(), PERSONAL_DETAILS, "dateOfBirth"),
        PERSON_NI_NUMBER(o -> o.getPersonalDetails().getNationalInsuranceNumber(), PERSONAL_DETAILS, "nationalInsuranceNumber"),
        PERSON_DRIVER_NUMBER(o -> o.getPersonalDetails().getDriverNumber(), PERSONAL_DETAILS, "driverNumber"),
        PERSON_DRIVER_LICENCE_DETAILS(o -> o.getPersonalDetails().getDriverLicenceDetails(), PERSONAL_DETAILS, "driverLicenceDetails"),
        PERSON_ADDRESS_1(o -> ofNullable(o.getPersonalDetails().getAddress()).map(Address::getAddress1).orElse(null), PERSONAL_DETAILS, ADDRESS, "address1"),
        PERSON_ADDRESS_2(o -> ofNullable(o.getPersonalDetails().getAddress()).map(Address::getAddress2).orElse(null), PERSONAL_DETAILS, ADDRESS, "address2"),
        PERSON_ADDRESS_3(o -> ofNullable(o.getPersonalDetails().getAddress()).map(Address::getAddress3).orElse(null), PERSONAL_DETAILS, ADDRESS, "address3"),
        PERSON_ADDRESS_4(o -> ofNullable(o.getPersonalDetails().getAddress()).map(Address::getAddress4).orElse(null), PERSONAL_DETAILS, ADDRESS, "address4"),
        PERSON_ADDRESS_5(o -> ofNullable(o.getPersonalDetails().getAddress()).map(Address::getAddress5).orElse(null), PERSONAL_DETAILS, ADDRESS, "address5"),
        PERSON_POSTCODE(o -> ofNullable(o.getPersonalDetails().getAddress()).map(Address::getPostcode).orElse(null), PERSONAL_DETAILS, ADDRESS, "postcode");

        private String[] fieldPath;
        private Function<AocpOnlinePlea, Object> fieldGetter;

        FIELDS(Function<AocpOnlinePlea, Object> fieldGetter, String... fieldPath) {
            this.fieldPath = fieldPath;
            this.fieldGetter = fieldGetter;
        }

        public Path<Object> getDbFieldPath(Root<AocpOnlinePlea> from) {
            Path<Object> objectPath = from.get(fieldPath[0]);
            for (int i = 1; i < fieldPath.length; i++) {
                objectPath = objectPath.get(fieldPath[i]);
            }
            return objectPath;
        }

        public Object convertValue(AocpOnlinePlea aocpOnlinePlea) {
            return fieldGetter.apply(aocpOnlinePlea);
        }
    }

    public abstract static class PersonDetailsOnlinePleaRepository extends AocpOnlinePleaRepository {
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
                    PERSON_DRIVER_LICENCE_DETAILS
            );
        }
    }



}
