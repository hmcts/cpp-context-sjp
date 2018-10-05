package uk.gov.moj.cpp.sjp.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CourtReferral;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.view.CaseReferredToCourt;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CaseReferredToCourtRepositoryTest extends BaseTransactionalTest {

    @Inject
    private CaseReferredToCourtRepository caseReferredToCourtRepository;

    @Inject
    private CourtReferralRepository courtReferralRepository;

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private Clock clock;

    @Test
    public void shouldFindUnactionedCases() {

        // check ordering by hearing date
        CaseReferredToCourt case2 = createCaseReferredToCourt(LocalDate.now().plusWeeks(2));
        CaseReferredToCourt case1 = createCaseReferredToCourt(LocalDate.now().plusWeeks(1));

        final List<CaseReferredToCourt> unactionedCases = caseReferredToCourtRepository.findUnactionedCases();

        assertThat(unactionedCases.size(), is(2));
        assertThat(unactionedCases.get(0).getCaseId(), is(case1.getCaseId()));
        assertThat(unactionedCases.get(1).getCaseId(), is(case2.getCaseId()));
    }

    private CaseReferredToCourt createCaseReferredToCourt(final LocalDate hearingDate) {

        final CaseReferredToCourt caseReferredToCourt = new CaseReferredToCourt(
                UUID.randomUUID(),
                RandomStringUtils.randomAlphabetic(12),
                RandomStringUtils.randomAlphabetic(12),
                RandomStringUtils.randomAlphabetic(12),
                RandomStringUtils.randomAlphabetic(12),
                hearingDate);

        final DefendantDetail defendantDetail = DefendantDetailBuilder.aDefendantDetail()
                .withInterpreterLanguage(caseReferredToCourt.getInterpreterLanguage())
                .build();
        final CaseDetail caseDetail = CaseDetailBuilder.aCase()
                .withCaseId(caseReferredToCourt.getCaseId())
                .withUrn(caseReferredToCourt.getUrn())
                .addDefendantDetail(defendantDetail)
                .build();

        caseRepository.save(caseDetail);

        courtReferralRepository.save(new CourtReferral(caseReferredToCourt.getCaseId(), hearingDate));

        return caseReferredToCourt;
    }

}