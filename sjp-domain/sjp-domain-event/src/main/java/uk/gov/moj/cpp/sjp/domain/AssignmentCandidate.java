package uk.gov.moj.cpp.sjp.domain;


import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class AssignmentCandidate {

    private UUID caseId;
    private int caseStreamVersion;

    public AssignmentCandidate(final UUID id, final int caseStreamVersion) {
        this.caseId = id;
        this.caseStreamVersion = caseStreamVersion;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public int getCaseStreamVersion() {
        return caseStreamVersion;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, SHORT_PREFIX_STYLE);
    }

}
