package uk.gov.moj.cpp.sjp.event.decision;

import static uk.gov.moj.cpp.sjp.event.decision.ApplicationDecisionSetAside.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Signals the fact that the case is set aside
 * as a result of the application decision.
 */
@Event(EVENT_NAME)
public class ApplicationDecisionSetAside {

    public static final String EVENT_NAME = "sjp.events.application-decision-set-aside";

    private UUID applicationId;

    private UUID caseId;
    private final String caseUrn;

    @JsonCreator
    public ApplicationDecisionSetAside(@JsonProperty("applicationId") final UUID applicationId,
                                       @JsonProperty("caseId") UUID caseId,
                                       @JsonProperty("caseUrn") String caseUrn) {
        this.applicationId = applicationId;
        this.caseId = caseId;
        this.caseUrn = caseUrn;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getCaseUrn() {
        return this.caseUrn;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApplicationDecisionSetAside)) {
            return false;
        }
        final ApplicationDecisionSetAside that = (ApplicationDecisionSetAside) o;
        return applicationId.equals(that.applicationId) &&
                caseId.equals(that.caseId) &&
                caseUrn.equals(that.caseUrn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId, caseId, caseUrn);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApplicationDecisionSetAside{");
        sb.append("applicationId=").append(applicationId);
        sb.append(", caseId=").append(caseId);
        sb.append(", caseUrn=").append(caseUrn);
        sb.append('}');
        return sb.toString();
    }
}
