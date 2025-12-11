package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseAssignmentRestriction;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
@Ignore
// This test is ignored since h2 does not support jsonb type
public class CaseAssignmentRestrictionRepositoryTest extends BaseTransactionalJunit4Test {

    private static final String PROSECUTING_AUTHORITY_TVL = "TVL";
    private static final String PROSECUTING_AUTHORITY_TFL = "TFL";
    private static final String PROSECUTING_AUTHORITY_DVLA = "DVLA";

    @Inject
    private Clock dateTimeCreated;

    @Inject
    private CaseAssignmentRestrictionRepository repository;

    @Before
    public void set() {
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
