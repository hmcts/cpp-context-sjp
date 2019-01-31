package uk.gov.moj.cpp.sjp.domain.common;

import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.time.LocalDate;

public class PleaInformation {
    private final PleaType pleaType;
    private final LocalDate pleaDate;

    public PleaInformation(final PleaType pleaType, final LocalDate pleaDate) {
        this.pleaType = pleaType;
        this.pleaDate = pleaDate;
    }

    public PleaType getPleaType() {
        return pleaType;
    }

    public LocalDate getPleaDate() {
        return pleaDate;
    }
}
