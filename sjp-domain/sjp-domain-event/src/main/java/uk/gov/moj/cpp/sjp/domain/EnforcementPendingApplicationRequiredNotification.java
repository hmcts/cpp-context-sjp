package uk.gov.moj.cpp.sjp.domain;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EnforcementPendingApplicationRequiredNotification {

    private final UUID caseId;
    private final int divisionCode;

    @JsonCreator
    public EnforcementPendingApplicationRequiredNotification(@JsonProperty("caseId") final UUID caseId,
                                                             @JsonProperty("divisionCode") final int divisionCode) {
        this.caseId = caseId;
        this.divisionCode = divisionCode;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public int getDivisionCode() {
        return divisionCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnforcementPendingApplicationRequiredNotification)) {
            return false;
        }
        final EnforcementPendingApplicationRequiredNotification that = (EnforcementPendingApplicationRequiredNotification) o;
        final boolean caseIdEquals = Objects.equals(getCaseId(), that.getCaseId());
        final boolean divisionCodeEquals = getDivisionCode() == that.getDivisionCode();
        return caseIdEquals && divisionCodeEquals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCaseId(), getDivisionCode());
    }
}
