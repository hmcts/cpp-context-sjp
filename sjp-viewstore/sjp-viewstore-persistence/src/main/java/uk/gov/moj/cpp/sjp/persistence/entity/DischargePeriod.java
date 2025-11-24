package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
public class DischargePeriod {

    @Column(name = "unit")
    @Enumerated(EnumType.STRING)
    private PeriodUnit unit;
    @Column(name = "value")
    private int value;

    private DischargePeriod() {

    }

    public DischargePeriod(final PeriodUnit unit, final int value) {
        this.unit = unit;
        this.value = value;
    }

    public PeriodUnit getUnit() {
        return unit;
    }

    public void setUnit(PeriodUnit unit) {
        this.unit = unit;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }


}
