package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder.aCase;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CaseRepositoryProsecutingAuthorityTest extends BaseTransactionalJunit4Test {

    @Inject
    private CaseRepository caseRepository;

    private CaseDetail tflCase, tvlCase;

    @Before
    public void init() {
        tflCase = aCase().withCaseId(randomUUID()).withProsecutingAuthority("TFL").build();
        tvlCase = aCase().withCaseId(randomUUID()).withProsecutingAuthority("TVL").build();
        caseRepository.save(tflCase);
        caseRepository.save(tvlCase);
    }

    @Test
    public void shouldGetCaseProsecutingAuthority() {
        assertThat(caseRepository.getProsecutingAuthority(tflCase.getId()), is("TFL"));
        assertThat(caseRepository.getProsecutingAuthority(tvlCase.getId()), is("TVL"));
        assertThat(caseRepository.getProsecutingAuthority(randomUUID()), nullValue());
    }

}
