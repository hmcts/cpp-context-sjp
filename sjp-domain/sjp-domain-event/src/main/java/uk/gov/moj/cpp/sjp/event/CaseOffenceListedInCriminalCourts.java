package uk.gov.moj.cpp.sjp.event;

import static java.util.Collections.unmodifiableList;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


@Event(CaseOffenceListedInCriminalCourts.EVENT_NAME)
public class CaseOffenceListedInCriminalCourts implements Serializable {

    public static final String EVENT_NAME = "sjp.events.case-offence-listed-in-criminal-courts";

    private final UUID caseId;
    private final UUID defendantId;
    private final List<UUID> defendantOffences;
    private final UUID hearingId;
    private final CourtCentre courtCentre;
    private final List<HearingDay> hearingDays;
    private final HearingType hearingType;

    @JsonCreator
    public CaseOffenceListedInCriminalCourts(@JsonProperty("caseId") final UUID caseId,
                                             @JsonProperty("defendantId") final UUID defendantId,
                                             @JsonProperty("defendantOffences") final List<UUID> defendantOffences,
                                             @JsonProperty("hearingId") final UUID hearingId,
                                             @JsonProperty("courtCentre") final CourtCentre courtCentre,
                                             @JsonProperty("hearingDays") final List<HearingDay> hearingDays,
                                             @JsonProperty("hearingType") final HearingType hearingType) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.defendantOffences = defendantOffences;
        this.hearingId = hearingId;
        this.hearingDays = hearingDays;
        this.courtCentre = courtCentre;
        this.hearingType = hearingType;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public List<UUID> getDefendantOffences() {
        return unmodifiableList(defendantOffences);
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public CourtCentre getCourtCentre() {
        return courtCentre;
    }

    public List<HearingDay> getHearingDays() {
        return unmodifiableList(hearingDays);
    }

    public HearingType getHearingType() {
        return hearingType;
    }
}
