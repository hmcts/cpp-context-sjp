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
    static HibernateTestEntityManagerProvider provider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private static final String PROSECUTING_AUTHORITY_TVL = "TVL";
    private static final String PROSECUTING_AUTHORITY_TFL = "TFL";
    private static final String PROSECUTING_AUTHORITY_DVLA = "DVLA";

    private final Clock dateTimeCreated = new UtcClock();

    private CaseAssignmentRestrictionRepository repository;

    @BeforeEach
    void set() {
        repository = new CaseAssignmentRestrictionRepository();
        provider.injectEntityManagerInto(repository);
        repository.saveCaseAssignmentRestriction(PROSECUTING_AUTHORITY_TVL, "[]", "[]", dateTimeCreated.now(), dateTimeCreated.now().toLocalDate(), dateTimeCreated.now().toLocalDate());
        repository.saveCaseAssignmentRestriction(PROSECUTING_AUTHORITY_TFL, "[\"1234\"]", "[]", dateTimeCreated.now(), null, dateTimeCreated.now().toLocalDate());
        repository.saveCaseAssignmentRestriction(PROSECUTING_AUTHORITY_DVLA, "[]", "[\"9876\"]", dateTimeCreated.now(), dateTimeCreated.now().toLocalDate(), null);
    }

    @Test
    public void shouldFindByProsecutingAuthority() {
        CaseAssignmentRestriction caseAssignmentRestriction = repository.findBy(PROSECUTING_AUTHORITY_TVL);
        assertThat(caseAssignmentRestriction.getProsecutingAuthority(), equalTo(PROSECUTING_AUTHORITY_TVL));
        assertThat(caseAssignmentRestriction.getDateTimeCreated(), equalTo(dateTimeCreated.now()));
        assertThat(caseAssignmentRestriction.getExclude(), equalTo(emptyList()));
        assertThat(caseAssignmentRestriction.getIncludeOnly(), equalTo(emptyList()));
        assertThat(caseAssignmentRestriction.getValidFrom(), equalTo(dateTimeCreated.now().toLocalDate()));
        assertThat(caseAssignmentRestriction.getValidTo(), equalTo(dateTimeCreated.now().toLocalDate()));

        caseAssignmentRestriction = repository.findBy(PROSECUTING_AUTHORITY_TFL);
        assertThat(caseAssignmentRestriction.getProsecutingAuthority(), equalTo(PROSECUTING_AUTHORITY_TFL));
        assertThat(caseAssignmentRestriction.getDateTimeCreated(), equalTo(dateTimeCreated.now()));
        assertThat(caseAssignmentRestriction.getExclude(), equalTo(emptyList()));
        assertThat(caseAssignmentRestriction.getIncludeOnly(), equalTo(singletonList("1234")));
        assertNull(caseAssignmentRestriction.getValidFrom());
        assertThat(caseAssignmentRestriction.getValidTo(), equalTo(dateTimeCreated.now().toLocalDate()));

        caseAssignmentRestriction = repository.findBy(PROSECUTING_AUTHORITY_DVLA);
        assertThat(caseAssignmentRestriction.getProsecutingAuthority(), equalTo(PROSECUTING_AUTHORITY_DVLA));
        assertThat(caseAssignmentRestriction.getDateTimeCreated(), equalTo(dateTimeCreated.now()));
        assertThat(caseAssignmentRestriction.getExclude(), equalTo(singletonList("9876")));
        assertThat(caseAssignmentRestriction.getIncludeOnly(), equalTo(emptyList()));
        assertThat(caseAssignmentRestriction.getValidFrom(), equalTo(dateTimeCreated.now().toLocalDate()));
        assertNull(caseAssignmentRestriction.getValidTo());
    }
}
