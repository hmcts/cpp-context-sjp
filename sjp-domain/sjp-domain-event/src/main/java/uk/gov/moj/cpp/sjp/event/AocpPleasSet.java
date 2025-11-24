package uk.gov.moj.cpp.sjp.event;

import static uk.gov.moj.cpp.sjp.event.AocpPleasSet.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(EVENT_NAME)
public class AocpPleasSet {

    public static final String EVENT_NAME = "sjp.events.aocp-pleas-set";

    private final UUID caseId;

    private final List<Plea> pleas;

    private final ZonedDateTime pleaDate;

    private final PleaMethod pleaMethod;

    @JsonCreator
    public AocpPleasSet(@JsonProperty("caseId") final UUID caseId,
                        @JsonProperty("pleas") final List<Plea> pleas,
                        @JsonProperty("pleaDate") final ZonedDateTime pleaDate,
                        @JsonProperty("pleaMethod") final PleaMethod pleaMethod)
                    {
        this.caseId = caseId;
        this.pleas = new ArrayList<>(pleas);
        this.pleaDate = pleaDate;
        this.pleaMethod = pleaMethod;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public List<Plea> getPleas() {
        return Collections.unmodifiableList(pleas);
    }

    public ZonedDateTime getPleaDate() {
        return pleaDate;
    }

    public PleaMethod getPleaMethod() {
        return pleaMethod;
    }

}
