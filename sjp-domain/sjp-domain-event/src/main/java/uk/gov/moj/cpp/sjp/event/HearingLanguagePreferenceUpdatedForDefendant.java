package uk.gov.moj.cpp.sjp.event;

import static uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(EVENT_NAME)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HearingLanguagePreferenceUpdatedForDefendant {

    public static final String EVENT_NAME = "sjp.events.hearing-language-preference-for-defendant-updated";

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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass())  {
            return false;
        }
        final HearingLanguagePreferenceUpdatedForDefendant that = (HearingLanguagePreferenceUpdatedForDefendant) o;
        return updatedByOnlinePlea == that.updatedByOnlinePlea &&
                caseId.equals(that.caseId) &&
                defendantId.equals(that.defendantId) &&
                speakWelsh.equals(that.speakWelsh);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, defendantId, speakWelsh, updatedByOnlinePlea);
    }

    @Override
    public String toString() {
        return "HearingLanguagePreferenceUpdatedForDefendant{" +
                "caseId=" + caseId +
                ", defendantId=" + defendantId +
                ", speakWelsh=" + speakWelsh +
                ", updatedByOnlinePlea=" + updatedByOnlinePlea +
                ", updatedDate=" + updatedDate +
                '}';
    }
}
