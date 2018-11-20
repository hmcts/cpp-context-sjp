package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Event for case listed in criminal courts
 */
@Event(CaseListedInCriminalCourts.EVENT_NAME)
public class CaseListedInCriminalCourts {

    public static final String EVENT_NAME = "sjp.events.case-listed-in-criminal-courts";

    private final UUID caseId;

    @JsonCreator
    public CaseListedInCriminalCourts(@JsonProperty("caseId") final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
