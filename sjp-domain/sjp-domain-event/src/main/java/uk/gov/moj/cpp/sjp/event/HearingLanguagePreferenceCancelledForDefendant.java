package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.hearing-language-preference-for-defendant-cancelled")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HearingLanguagePreferenceCancelledForDefendant {

    private final UUID caseId;
    private final UUID defendantId;

    public HearingLanguagePreferenceCancelledForDefendant(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("defendantId") final UUID defendantId) {
        this.caseId = caseId;
        this.defendantId = defendantId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }
}
