package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.defendant-national-insurance-number-updated")
public class DefendantsNationalInsuranceNumberUpdated {

    private UUID caseId;
    private UUID defendantId;
    private String nationalInsuranceNumber;

    @JsonCreator
    public DefendantsNationalInsuranceNumberUpdated(
            @JsonProperty(value = "caseId") UUID caseId,
            @JsonProperty(value = "defendantId") UUID defendantId,
            @JsonProperty(value = "nationalInsuranceNumber") String nationalInsuranceNumber) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.nationalInsuranceNumber = nationalInsuranceNumber;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getNationalInsuranceNumber() {
        return nationalInsuranceNumber;
    }
}
