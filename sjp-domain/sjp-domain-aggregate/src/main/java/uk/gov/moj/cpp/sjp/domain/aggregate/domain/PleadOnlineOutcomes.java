package uk.gov.moj.cpp.sjp.domain.aggregate.domain;

import java.util.ArrayList;
import java.util.List;

public class PleadOnlineOutcomes {
    private boolean pleaForOffencePreviouslySubmitted;
    private boolean trialRequested;
    private List<String> offenceNotFoundIds = new ArrayList<>();

    public boolean isPleaForOffencePreviouslySubmitted() {
        return pleaForOffencePreviouslySubmitted;
    }

    public boolean isTrialRequested() {
        return trialRequested;
    }

    public void setPleaForOffencePreviouslySubmitted(final boolean pleaForOffencePreviouslySubmitted) {
        this.pleaForOffencePreviouslySubmitted = pleaForOffencePreviouslySubmitted;
    }

    public void setTrialRequested(final boolean trialRequested) {
        this.trialRequested = trialRequested;
    }

    public List<String> getOffenceNotFoundIds() {
        return offenceNotFoundIds;
    }

    public void setOffenceNotFoundIds(final List<String> offenceNotFoundIds) {
        this.offenceNotFoundIds = offenceNotFoundIds;
    }
}
