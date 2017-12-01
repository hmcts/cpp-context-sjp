package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class CaseAssignment implements Serializable {

    private static final long serialVersionUID = 3422623804809271943L;

    private final String caseId;
    private final CaseAssignmentType caseAssignmentType;

    @JsonCreator
    public CaseAssignment(@JsonProperty("caseId") String caseId,
                          @JsonProperty("caseAssignmentType") String caseAssignmentType) {
        this.caseId = caseId;
        this.caseAssignmentType = CaseAssignmentType.from(caseAssignmentType).get();
    }

    public String getCaseId() {
        return caseId;
    }

    public CaseAssignmentType getCaseAssignmentType() {
        return caseAssignmentType;
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
