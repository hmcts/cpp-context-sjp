package uk.gov.moj.cpp.sjp.event.processor.model.referral;


import java.time.LocalDate;

import com.google.common.base.Objects;

public class SjpReferralView {

    private final LocalDate noticeDate;
    private final LocalDate referralDate;
    private final ReferringJudicialDecisionView referringJudicialDecision;

    public SjpReferralView(final LocalDate noticeDate,
                           final LocalDate referralDate,
                           final ReferringJudicialDecisionView referringJudicialDecision) {

        this.noticeDate = noticeDate;
        this.referralDate = referralDate;
        this.referringJudicialDecision = referringJudicialDecision;
    }

    public LocalDate getNoticeDate() {
        return noticeDate;
    }

    public LocalDate getReferralDate() {
        return referralDate;
    }

    public ReferringJudicialDecisionView getReferringJudicialDecision() {
        return referringJudicialDecision;
    }

    @SuppressWarnings("squid:S00121")
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SjpReferralView that = (SjpReferralView) o;
        return Objects.equal(noticeDate, that.noticeDate) &&
                Objects.equal(referralDate, that.referralDate) &&
                Objects.equal(referringJudicialDecision, that.referringJudicialDecision);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(noticeDate, referralDate, referringJudicialDecision);
    }
}
