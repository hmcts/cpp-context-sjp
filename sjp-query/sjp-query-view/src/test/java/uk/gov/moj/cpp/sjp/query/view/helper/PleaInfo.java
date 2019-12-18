package uk.gov.moj.cpp.sjp.query.view.helper;

import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.time.ZonedDateTime;

public class PleaInfo {
    public final PleaType pleaType;
    public final ZonedDateTime pleaDate;

    private PleaInfo(final PleaType pleaType, final ZonedDateTime pleaDate) {
        this.pleaType = pleaType;
        this.pleaDate = pleaDate;
    }

    public static PleaInfo plea(final PleaType pleaType, final ZonedDateTime pleaDate) {
        return new PleaInfo(pleaType, pleaDate);
    }
}

