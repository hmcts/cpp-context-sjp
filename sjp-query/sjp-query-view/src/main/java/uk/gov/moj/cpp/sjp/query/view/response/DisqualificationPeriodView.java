package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationPeriodTimeUnit;

public class DisqualificationPeriodView {

    private Integer value;
    private DisqualificationPeriodTimeUnit unit;

    public DisqualificationPeriodView(final Integer value, final DisqualificationPeriodTimeUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    public Integer getValue() {
        return value;
    }

    public DisqualificationPeriodTimeUnit getUnit() {
        return unit;
    }
}
