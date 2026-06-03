package uk.gov.moj.cpp.sjp.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;
import uk.gov.moj.cpp.sjp.domain.decision.SessionCourt;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;

@SuppressWarnings("squid:S107")
@Event(CaseReferredForCourtHearingV2.EVENT_NAME)
public class CaseReferredForCourtHearingV2 {

    public static final String EVENT_NAME = "sjp.events.case-referred-for-court-hearing-v2";
    private final UUID caseId;

    private List<OffenceDecisionInformation> referredOffences = new ArrayList<>();

    private final Integer estimatedHearingDuration;

    private final String listingNotes;

    private final UUID referralReasonId;

    private final String referralReason;

    private final ZonedDateTime referredAt;

    private final UUID decisionId;

    private final DefendantCourtOptions defendantCourtOptions;

    private LocalDate convictionDate;

    private SessionCourt convictingCourt;

    private NextHearing nextHearing;

    private UUID sessionId;

    private PressRestriction pressRestriction;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public CaseReferredForCourtHearingV2(final UUID caseId, final List<OffenceDecisionInformation> referredOffences,
                                         final UUID referralReasonId, final String referralReason,
                                         final Integer estimatedHearingDuration, final String listingNotes,
                                         final ZonedDateTime referredAt, final UUID decisionId,
                                         final DefendantCourtOptions defendantCourtOptions,
                                         final LocalDate convictionDate , final SessionCourt convictingCourt,
                                         final NextHearing nextHearing, final UUID sessionId,
                                         final PressRestriction pressRestriction) {
        this.caseId = caseId;
        this.estimatedHearingDuration = estimatedHearingDuration;
        this.listingNotes = listingNotes;
        this.referralReason = referralReason;
        this.referralReasonId = referralReasonId;
        this.referredAt = referredAt;
        this.decisionId = decisionId;
        this.referredOffences = nonNull(referredOffences) ? unmodifiableList(referredOffences) : referredOffences;
        this.defendantCourtOptions = defendantCourtOptions;
        this.convictionDate = convictionDate;
        this.convictingCourt = convictingCourt;
        this.nextHearing = nextHearing;
        this.sessionId = sessionId;
        this.pressRestriction = pressRestriction;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public List<OffenceDecisionInformation> getReferredOffences() {
        return nonNull(referredOffences) ? unmodifiableList(referredOffences) : emptyList();
    }

    public Integer getEstimatedHearingDuration() {
        return estimatedHearingDuration;
    }

    public String getListingNotes() {
        return listingNotes;
    }

    public String getReferralReason() {
        return referralReason;
    }

    public UUID getReferralReasonId() {
        return referralReasonId;
    }

    public ZonedDateTime getReferredAt() {
        return referredAt;
    }

    public UUID getDecisionId() {
        return decisionId;
    }

    public DefendantCourtOptions getDefendantCourtOptions() {
        return defendantCourtOptions;
    }

    public LocalDate getConvictionDate() {
        return convictionDate;
    }

    public SessionCourt getConvictingCourt() {
        return convictingCourt;
    }

    public NextHearing getNextHearing() {
        return nextHearing;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public PressRestriction getPressRestriction() {
        return pressRestriction;
    }

    public static Builder caseReferredForCourtHearingV2() {
        return new CaseReferredForCourtHearingV2.Builder();
    }

    public static class Builder {
        private UUID caseId;
        private UUID decisionId;
        private List<OffenceDecisionInformation> referredOffences = new ArrayList<>();
        private UUID referralReasonId;
        private String referralReason;
        private Integer estimatedHearingDuration;
        private String listingNotes;
        private ZonedDateTime referredAt;
        private DefendantCourtOptions defendantCourtOptions;
        private LocalDate convictionDate;
        private SessionCourt convictingCourt;
        private NextHearing nextHearing;
        private UUID sessionId;
        private PressRestriction pressRestriction;

        public Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder withDecisionId(final UUID decisionId) {
            this.decisionId = decisionId;
            return this;
        }

        public Builder withReferredOffences(final List<OffenceDecisionInformation> referredOffences) {
            this.referredOffences.addAll(referredOffences);
            return this;
        }

        public Builder withEstimatedHearingDuration(final Integer estimatedHearingDuration) {
            this.estimatedHearingDuration = estimatedHearingDuration;
            return this;
        }

        public Builder withListingNotes(final String listingNotes) {
            this.listingNotes = listingNotes;
            return this;
        }

        public Builder withReferralReason(final String referralReason) {
            this.referralReason = referralReason;
            return this;
        }

        public Builder withReferralReasonId(final UUID referralReasonId) {
            this.referralReasonId = referralReasonId;
            return this;
        }

        public Builder withReferredAt(final ZonedDateTime referredAt) {
            this.referredAt = referredAt;
            return this;
        }

        public Builder withDefendantCourtOptions(final DefendantCourtOptions defendantCourtOptions) {
            this.defendantCourtOptions = defendantCourtOptions;
            return this;
        }

        public Builder withConvictionDate(final LocalDate convictionDate) {
            this.convictionDate = convictionDate;
            return this;
        }

        public Builder withConvictingCourt(final SessionCourt convictingCourt) {
            this.convictingCourt = convictingCourt;
            return this;
        }

        public Builder withNextHearing(final NextHearing nextHearing) {
            this.nextHearing = nextHearing;
            return this;
        }

        public Builder withSessionId(final UUID sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder withPressRestriction(final PressRestriction pressRestriction) {
            this.pressRestriction = pressRestriction;
            return this;
        }

        public CaseReferredForCourtHearingV2 build() {
            return new CaseReferredForCourtHearingV2(caseId, referredOffences, referralReasonId, referralReason,
                    estimatedHearingDuration, listingNotes, referredAt, decisionId, defendantCourtOptions,
                    convictionDate, convictingCourt, nextHearing, sessionId, pressRestriction);
        }

    }
}
