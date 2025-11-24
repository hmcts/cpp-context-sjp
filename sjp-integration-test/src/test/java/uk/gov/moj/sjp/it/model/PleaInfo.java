package uk.gov.moj.sjp.it.model;

import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.util.UUID;

public class PleaInfo {

    public final UUID offenceId;
    public final PleaType pleaType;

    public static PleaInfo pleaInfo(final UUID offenceId, final PleaType pleaType) {
        return new PleaInfo(offenceId, pleaType);
    }

    public PleaInfo(final UUID offenceId, final PleaType pleaType) {
        this.offenceId = offenceId;
        this.pleaType = pleaType;
    }
}
