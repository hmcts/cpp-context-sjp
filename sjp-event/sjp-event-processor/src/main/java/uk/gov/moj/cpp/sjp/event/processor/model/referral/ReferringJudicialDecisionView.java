package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import java.util.List;

import com.google.common.base.Objects;

public class ReferringJudicialDecisionView {

    private final String location;
    private final String courtHouseCode;

    private final List<JudiciaryView> judiciary;

    public ReferringJudicialDecisionView(final String location,
                                         final String courtHouseCode,
                                         final List<JudiciaryView> judiciary) {

        this.location = location;
        this.courtHouseCode = courtHouseCode;
        this.judiciary = judiciary;
    }

    public String getLocation() {
        return location;
    }

    public String getCourtHouseCode() { return courtHouseCode; }

    public List<JudiciaryView> getJudiciary() {
        return judiciary;
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

        final ReferringJudicialDecisionView that = (ReferringJudicialDecisionView) o;
        return Objects.equal(location, that.location) &&
                Objects.equal(courtHouseCode, that.courtHouseCode) &&
                Objects.equal(judiciary, that.judiciary);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(location, courtHouseCode, judiciary);
    }
}
