package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(CaseUnmarkedReadyForDecision.EVENT_NAME)
public class CaseUnmarkedReadyForDecision {

    public static final String EVENT_NAME = "sjp.events.case-unmarked-ready-for-decision";

    private final UUID caseId;
    private PleaType pleaType;

    @JsonCreator
    public CaseUnmarkedReadyForDecision(@JsonProperty("caseId") final UUID caseId, @JsonProperty("pleaType") final PleaType pleaType) {
        this.caseId = caseId;
        this.pleaType = pleaType;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public PleaType getPleaType() {
        return pleaType;
    }

}
