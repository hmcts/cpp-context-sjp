package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseAssignmentRestriction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Disabled
// This test is ignored since h2 does not support jsonb type
class CaseAssignmentRestrictionRepositoryTest {

    private static final String PERSISTENCE_UNIT = "sjp-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private static final String PROSECUTING_AUTHORITY_TVL = "TVL";
    private static final String PROSECUTING_AUTHORITY_TFL = "TFL";
    private static final String PROSECUTING_AUTHORITY_DVLA = "DVLA";

    private final Clock clock = new UtcClock();

    private CaseAssignmentRestrictionRepository caseAssignmentRestrictionRepository;

    @BeforeEach
    void createRepositoryWithInjectedEntityManagerAndSaveRestrictions() {
        caseAssignmentRestrictionRepository = new CaseAssignmentRestrictionRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(caseAssignmentRestrictionRepository);
        caseAssignmentRestrictionRepository.saveCaseAssignmentRestriction(PROSECUTING_AUTHORITY_TVL, "[]", "[]", clock.now(), clock.now().toLocalDate(), clock.now().toLocalDate());
        caseAssignmentRestrictionRepository.saveCaseAssignmentRestriction(PROSECUTING_AUTHORITY_TFL, "[\"1234\"]", "[]", clock.now(), null, clock.now().toLocalDate());
        caseAssignmentRestrictionRepository.saveCaseAssignmentRestriction(PROSECUTING_AUTHORITY_DVLA, "[]", "[\"9876\"]", clock.now(), clock.now().toLocalDate(), null);
    }

    @Test
    public void shouldFindByProsecutingAuthority() {
        CaseAssignmentRestriction caseAssignmentRestriction = caseAssignmentRestrictionRepository.findBy(PROSECUTING_AUTHORITY_TVL);
        assertThat(caseAssignmentRestriction.getProsecutingAuthority(), equalTo(PROSECUTING_AUTHORITY_TVL));
        assertThat(caseAssignmentRestriction.getDateTimeCreated(), equalTo(clock.now()));
        assertThat(caseAssignmentRestriction.getExclude(), equalTo(emptyList()));
        assertThat(caseAssignmentRestriction.getIncludeOnly(), equalTo(emptyList()));
        assertThat(caseAssignmentRestriction.getValidFrom(), equalTo(clock.now().toLocalDate()));
        assertThat(caseAssignmentRestriction.getValidTo(), equalTo(clock.now().toLocalDate()));

        caseAssignmentRestriction = caseAssignmentRestrictionRepository.findBy(PROSECUTING_AUTHORITY_TFL);
        assertThat(caseAssignmentRestriction.getProsecutingAuthority(), equalTo(PROSECUTING_AUTHORITY_TFL));
        assertThat(caseAssignmentRestriction.getDateTimeCreated(), equalTo(clock.now()));
        assertThat(caseAssignmentRestriction.getExclude(), equalTo(emptyList()));
        assertThat(caseAssignmentRestriction.getIncludeOnly(), equalTo(singletonList("1234")));
        assertNull(caseAssignmentRestriction.getValidFrom());
        assertThat(caseAssignmentRestriction.getValidTo(), equalTo(clock.now().toLocalDate()));

        caseAssignmentRestriction = caseAssignmentRestrictionRepository.findBy(PROSECUTING_AUTHORITY_DVLA);
        assertThat(caseAssignmentRestriction.getProsecutingAuthority(), equalTo(PROSECUTING_AUTHORITY_DVLA));
        assertThat(caseAssignmentRestriction.getDateTimeCreated(), equalTo(clock.now()));
        assertThat(caseAssignmentRestriction.getExclude(), equalTo(singletonList("9876")));
        assertThat(caseAssignmentRestriction.getIncludeOnly(), equalTo(emptyList()));
        assertThat(caseAssignmentRestriction.getValidFrom(), equalTo(clock.now().toLocalDate()));
        assertNull(caseAssignmentRestriction.getValidTo());
    }
}
