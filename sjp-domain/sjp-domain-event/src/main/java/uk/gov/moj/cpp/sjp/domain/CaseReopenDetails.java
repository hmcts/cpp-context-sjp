package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class CaseReopenDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID caseId;
    private final LocalDate reopenedDate;
    private final String libraCaseNumber;
    private final String reason;

    @JsonCreator
    public CaseReopenDetails(@JsonProperty("caseId") UUID caseId,
                             @JsonProperty("reopenedDate") LocalDate reopenedDate,
                             @JsonProperty("libraCaseNumber") String libraCaseNumber,
                             @JsonProperty("reason") String reason) {
        this.caseId = caseId;
        this.reopenedDate = reopenedDate;
        this.libraCaseNumber = libraCaseNumber;
        this.reason = reason;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public LocalDate getReopenedDate() {
        return reopenedDate;
    }

    public String getLibraCaseNumber() {
        return libraCaseNumber;
    }

    public String getReason() {
        return reason;
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
