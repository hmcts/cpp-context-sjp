package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest.AggregateTester.when;

import org.junit.jupiter.api.Disabled;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.CaseReferralForCourtHearingRejectionRecorded;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CaseAggregate#recordCaseReferralForCourtHearingRejection}
 */
public class CaseRecordCaseReferralForCourtHearingRejectionTest extends CaseAggregateBaseTest{

    private static final String REJECTION_REASON = randomAlphanumeric(10);

    @Test
    public void shouldRejectWhenCaseNotFound() {
        resetAggregate();

        final UUID notExistingCaseId = randomUUID();
        when(caseAggregate.recordCaseReferralForCourtHearingRejection(notExistingCaseId, "aaa", clock.now()))
                .thenExpect(new CaseNotFound(notExistingCaseId, "Referral rejection recording of non existing case rejected"));
    }

    @Test
    public void shouldNotDoAnythingWhenCaseIsNotReferredForCourtHearing() {
        when(caseAggregate.recordCaseReferralForCourtHearingRejection(caseId, REJECTION_REASON, clock.now()))
                .thenExpect();
    }

    @Test
    @Disabled("functionality not currently implemented")
    public void shouldRejectRecordCaseReferralForCourtHearingWhenCaseReferredForCourtHearing() {
        // given need to do caseAggregate.markCaseReferredForCourtHearing

        // TODO: make case referredForCourtHearing and run the below - when caseState.isCaseReferredForCourtHearing() is true

        when(caseAggregate.recordCaseReferralForCourtHearingRejection(caseId, REJECTION_REASON, clock.now()))
                .thenExpect(new CaseReferralForCourtHearingRejectionRecorded(caseId, clock.now(), REJECTION_REASON));
    }


}
