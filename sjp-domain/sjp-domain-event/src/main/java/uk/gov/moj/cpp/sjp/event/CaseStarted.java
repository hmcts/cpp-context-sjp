package uk.gov.moj.cpp.sjp.event;


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("sjp.events.case-started")
public class CaseStarted {

    private UUID id;

    private CaseStarted() {
        //default constructor
    }

    public CaseStarted(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
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
