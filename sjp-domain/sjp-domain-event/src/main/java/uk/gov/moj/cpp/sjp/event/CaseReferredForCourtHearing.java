package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//TODO ATCM-4473 Event transformation
@Event(CaseReferredForCourtHearing.EVENT_NAME)
public class CaseReferredForCourtHearing {

    public static final String EVENT_NAME = "sjp.events.case-referred-for-court-hearing";
    private final UUID caseId;

    private List<OffenceDecisionInformation> referredOffences = new ArrayList<>();

    private final Integer estimatedHearingDuration;

    private final String listingNotes;

    private final UUID referralReasonId;

    private final ZonedDateTime referredAt;

    private final UUID decisionId;

    @JsonIgnoreProperties(ignoreUnknown = true)
    //Not needed if event transformation added - urn removed
    public CaseReferredForCourtHearing(final UUID caseId, final List<OffenceDecisionInformation> referredOffences, final UUID referralReasonId, final Integer estimatedHearingDuration, final String listingNotes, final ZonedDateTime referredAt, final UUID decisionId) {
        this.caseId = caseId;
        this.estimatedHearingDuration = estimatedHearingDuration;
        this.listingNotes = listingNotes;
        this.referralReasonId = referralReasonId;
        this.referredAt = referredAt;
        this.decisionId = decisionId;
        this.referredOffences = referredOffences;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public List<OffenceDecisionInformation> getReferredOffences() {
        return referredOffences;
    }

    public Integer getEstimatedHearingDuration() {
        return estimatedHearingDuration;
    }

    public String getListingNotes() {
        return listingNotes;
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

    public static Builder caseReferredForCourtHearing() {
        return new CaseReferredForCourtHearing.Builder();
    }

    public static class Builder {
        private UUID caseId;
        private UUID decisionId;
        private List<OffenceDecisionInformation> referredOffences = new ArrayList<>();
        private UUID referralReasonId;
        private Integer estimatedHearingDuration;
        private String listingNotes;
        private ZonedDateTime referredAt;

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

        public Builder withReferralReasonId(final UUID referralReasonId) {
            this.referralReasonId = referralReasonId;
            return this;
        }

        public Builder withReferredAt(final ZonedDateTime referredAt) {
            this.referredAt = referredAt;
            return this;
        }

        public CaseReferredForCourtHearing build() {
            return new CaseReferredForCourtHearing(caseId, referredOffences, referralReasonId, estimatedHearingDuration, listingNotes, referredAt, decisionId);
        }
    }
}
