package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

// Old event. Replaced by case-update-rejected
@Event("sjp.events.plea-update-denied")
public class PleaUpdateDenied implements Serializable {
    private String caseId;
    private String offenceId;
    private String plea;
    private Boolean interpreterRequired;
    private String interpreterLanguage;
    private DenialReason denialReason;

    //sjp.command.validation.UpdatedPleaValidator.DenialReason should be a subset of this
    public enum DenialReason {
        SJPN_NOT_ATTACHED,
        WITHDRAWAL_PENDING,
        CASE_ASSIGNED,
        CASE_COMPLETED
    }

    public PleaUpdateDenied(String caseId, String offenceId, String plea, DenialReason denialReason) {
        this.caseId = caseId;
        this.offenceId = offenceId;
        this.plea = plea;
        this.denialReason = denialReason;
    }

    @JsonCreator
    public PleaUpdateDenied(@JsonProperty("caseId") String caseId,
                            @JsonProperty("offenceId") String offenceId,
                            @JsonProperty("plea") String plea,
                            @JsonProperty("interpreterRequired") Boolean interpreterRequired,
                            @JsonProperty("interpreterLanguage") String interpreterLanguage,
                            @JsonProperty("denialReason") String denialReason) {
        this.caseId = caseId;
        this.offenceId = offenceId;
        this.plea = plea;
        this.interpreterRequired = interpreterRequired;
        this.interpreterLanguage = interpreterLanguage;
        this.denialReason = (denialReason != null ? DenialReason.valueOf(denialReason) : null);
    }

    public String getCaseId() {
        return caseId;
    }

    public String getOffenceId() {
        return offenceId;
    }

    public String getPlea() {
        return plea;
    }

    public DenialReason getDenialReason() {
        return denialReason;
    }

    public Boolean getInterpreterRequired() {
        return interpreterRequired;
    }

    public String getInterpreterLanguage() {
        return interpreterLanguage;
    }
}
