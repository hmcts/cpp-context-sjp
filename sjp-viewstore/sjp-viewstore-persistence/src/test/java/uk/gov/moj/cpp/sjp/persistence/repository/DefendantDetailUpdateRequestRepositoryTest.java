package uk.gov.moj.cpp.sjp.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetailUpdateRequest;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class DefendantDetailUpdateRequestRepositoryTest {

    private static final String PERSISTENCE_UNIT = "sjp-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider provider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private DefendantDetailUpdateRequestRepository defendantRepository;

    @BeforeEach
    void createRepositoryWithInjectedEntityManager() {
        defendantRepository = new DefendantDetailUpdateRequestRepository();
        provider.injectEntityManagerInto(defendantRepository);
    }

    @Test
    void shouldFindByCaseId() {
        final UUID caseId = UUID.randomUUID();
        DefendantDetailUpdateRequest defendantDetailUpdateRequest = createDefendantDetailUpdateRequest(caseId, UUID.randomUUID(), "firstName", "lastName", "middleName");
        defendantRepository.save(defendantDetailUpdateRequest);

        final DefendantDetailUpdateRequest updateRequest = defendantRepository.findBy(caseId);

        assertThat(caseId, equalTo(updateRequest.getCaseId()));
    }

    private static DefendantDetailUpdateRequest createDefendantDetailUpdateRequest(final UUID caseId, final UUID defendantId, final String firstName, final String lastName, final String middleName) {
        final DefendantDetailUpdateRequest defendantDetailUpdateRequest = new DefendantDetailUpdateRequest();
        defendantDetailUpdateRequest.setCaseId(caseId);
        defendantDetailUpdateRequest.setDefendantId(defendantId);
        defendantDetailUpdateRequest.setFirstName(firstName);
        defendantDetailUpdateRequest.setLastName(lastName);
        defendantDetailUpdateRequest.setStatus(DefendantDetailUpdateRequest.Status.PENDING);
        defendantDetailUpdateRequest.setUpdatedAt(ZonedDateTime.now());

        return defendantDetailUpdateRequest;
    }
}
