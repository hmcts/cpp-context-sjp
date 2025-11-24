package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


@Event(CaseListedInCriminalCourtsV2.EVENT_NAME)
public class CaseListedInCriminalCourtsV2 implements Serializable {

    public static final String EVENT_NAME = "sjp.events.case-listed-in-criminal-courts-v2";

    private final List<CaseOffenceListedInCriminalCourts> offenceHearings;

    private final DecisionSaved decisionSaved;

    private final UUID caseId;

    @JsonCreator
    public CaseListedInCriminalCourtsV2(@JsonProperty("offenceHearings") final List<CaseOffenceListedInCriminalCourts> offenceHearings,
                                        @JsonProperty("decisionSaved") final DecisionSaved decisionSaved,
                                        @JsonProperty("caseId") final UUID caseId) {
        this.offenceHearings = offenceHearings;
        this.decisionSaved = decisionSaved;
        this.caseId = caseId;
    }

    public DecisionSaved getDecisionSaved() {
        return decisionSaved;
    }

    public List<CaseOffenceListedInCriminalCourts> getOffenceHearings() {
        return offenceHearings;
    }

    public UUID getCaseId() {
        return caseId;
    }


}
