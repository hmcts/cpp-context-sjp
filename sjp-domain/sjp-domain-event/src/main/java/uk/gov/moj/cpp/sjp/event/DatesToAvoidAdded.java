package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.dates-to-avoid-added")
public class DatesToAvoidAdded {

    private final UUID caseId;
    private final String datesToAvoid;
    private PleaType pleaType;

    @JsonCreator
    public DatesToAvoidAdded(final @JsonProperty("caseId") UUID caseId,
                             final @JsonProperty("datesToAvoid") String datesToAvoid,
                             final @JsonProperty("pleaType") PleaType pleaType) {
        this.caseId = caseId;
        this.datesToAvoid = datesToAvoid;
        this.pleaType = pleaType;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getDatesToAvoid() {
        return datesToAvoid;
    }

    public PleaType getPleaType() {
        return pleaType;
    }
}
