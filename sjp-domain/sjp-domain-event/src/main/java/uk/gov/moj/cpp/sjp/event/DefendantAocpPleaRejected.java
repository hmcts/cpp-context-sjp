package uk.gov.moj.cpp.sjp.event;

import static uk.gov.moj.cpp.sjp.event.DefendantAocpPleaRejected.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.onlineplea.Offence;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PersonalDetails;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings({"squid:S2384", "squid:CallToDeprecatedMethod"})
@Event(EVENT_NAME)
public class DefendantAocpPleaRejected {
    public static final String EVENT_NAME = "sjp.events.defendant-aocp-plea-rejected";

    private final UUID caseId;

    private final UUID defendantId;

    private final List<Offence> offences;

    private final PleaMethod method;

    private final PersonalDetails personalDetails;

    private final boolean aocpAccepted;

    private final ZonedDateTime pleadDate;

    private final String caseUrn;

    private final String rejectedReason;

    @JsonCreator
    public DefendantAocpPleaRejected(@JsonProperty("caseId") final UUID caseId,
                                     @JsonProperty("defendantId") final UUID defendantId,
                                     @JsonProperty("offences") final List<Offence> offences,
                                     @JsonProperty("method") final PleaMethod method,
                                     @JsonProperty("personalDetails") final PersonalDetails personalDetails,
                                     @JsonProperty("aocpAccepted") final boolean aocpAccepted,
                                     @JsonProperty("pleadDate") final ZonedDateTime pleadDate,
                                     @JsonProperty("caseUrn") final String caseUrn,
                                     @JsonProperty("rejectedReason") final String rejectedReason
                                ) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.offences = offences;
        this.pleadDate = pleadDate;
        this.method = method;
        this.aocpAccepted = aocpAccepted;
        this.personalDetails = personalDetails;
        this.caseUrn = caseUrn;
        this.rejectedReason = rejectedReason;
    }


    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public List<Offence> getOffences() {
        return offences;
    }

    public PleaMethod getMethod() {
        return method;
    }

    public PersonalDetails getPersonalDetails() {
        return personalDetails;
    }

    public boolean getAocpAccepted() {
        return aocpAccepted;
    }

    public ZonedDateTime getPleadDate() {
        return pleadDate;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public String getRejectedReason() {
        return rejectedReason;
    }
}
