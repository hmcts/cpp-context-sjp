package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder.aCase;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CaseRepositoryProsecutingAuthorityTest {

    private static final String PERSISTENCE_UNIT = "sjp-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider provider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private CaseRepository caseRepository;

    private CaseDetail tflCase, tvlCase;

    @BeforeEach
    void createRepositoryAndSeedCases() {
        caseRepository = new CaseRepository();
        provider.injectEntityManagerInto(caseRepository);

        tflCase = aCase().withCaseId(randomUUID()).withProsecutingAuthority("TFL").build();
        tvlCase = aCase().withCaseId(randomUUID()).withProsecutingAuthority("TVL").build();
        caseRepository.save(tflCase);
        caseRepository.save(tvlCase);
    }

    @Test
    void shouldGetCaseProsecutingAuthority() {
        assertThat(caseRepository.getProsecutingAuthority(tflCase.getId()), is("TFL"));
        assertThat(caseRepository.getProsecutingAuthority(tvlCase.getId()), is("TVL"));
        assertThat(caseRepository.getProsecutingAuthority(randomUUID()), nullValue());
    }

}
