package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.hearing-language-preference-for-defendant-updated")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HearingLanguagePreferenceUpdatedForDefendant {

    private final UUID caseId;
    private final UUID defendantId;
    private final Boolean speakWelsh;
    private final boolean updatedByOnlinePlea;
    private final ZonedDateTime updatedDate;

    @JsonCreator
    private HearingLanguagePreferenceUpdatedForDefendant(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("defendantId") final UUID defendantId,
            @JsonProperty("speakWelsh") final Boolean speakWelsh,
            @JsonProperty("updatedByOnlinePlea") final boolean updatedByOnlinePlea,
            @JsonProperty("updatedDate") final ZonedDateTime updatedDate) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.speakWelsh = speakWelsh;
        this.updatedByOnlinePlea = updatedByOnlinePlea;
        this.updatedDate = updatedDate;
    }

    public static HearingLanguagePreferenceUpdatedForDefendant createEvent(final UUID caseId, UUID defendantId, Boolean speakWelsh) {
        return new HearingLanguagePreferenceUpdatedForDefendant(caseId, defendantId, speakWelsh, false, null);
    }

    public static HearingLanguagePreferenceUpdatedForDefendant createEventForOnlinePlea(final UUID caseId, UUID defendantId, Boolean speakWelsh, final ZonedDateTime updatedDate) {
        return new HearingLanguagePreferenceUpdatedForDefendant(caseId, defendantId, speakWelsh, true, updatedDate);
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public Boolean getSpeakWelsh() {
        return speakWelsh;
    }

    public Boolean isUpdatedByOnlinePlea() {
        return updatedByOnlinePlea;
    }

    public ZonedDateTime getUpdatedDate() {
        return updatedDate;
    }

}
