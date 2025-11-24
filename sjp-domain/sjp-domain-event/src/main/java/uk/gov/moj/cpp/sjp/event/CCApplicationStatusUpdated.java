package uk.gov.moj.cpp.sjp.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;

import java.util.Objects;
import java.util.UUID;

import static uk.gov.moj.cpp.sjp.event.CCApplicationStatusUpdated.SJP_EVENTS_CC_APPLICATION_STATUS_UPDATED;

@Event(SJP_EVENTS_CC_APPLICATION_STATUS_UPDATED)
public class CCApplicationStatusUpdated {

    public static final String SJP_EVENTS_CC_APPLICATION_STATUS_UPDATED = "sjp.events.cc-application-status-updated";

    private UUID caseId;

    private UUID applicationId;

    private ApplicationStatus status;

    @JsonCreator
    public CCApplicationStatusUpdated(@JsonProperty("caseId") final UUID caseId,
                                      @JsonProperty("applicationId") final UUID applicationId,
                                      @JsonProperty("status") ApplicationStatus status) {
        this.caseId = caseId;
        this.applicationId = applicationId;
        this.status = status;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CCApplicationStatusUpdated{");
        sb.append("caseId=").append(caseId);
        sb.append(",applicationId=").append(applicationId);
        sb.append(", status=").append(status);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CCApplicationStatusUpdated)) {
            return false;
        }
        final CCApplicationStatusUpdated that = (CCApplicationStatusUpdated) o;
        return caseId.equals(that.caseId) &&
                applicationId.equals(that.applicationId) &&
                status == that.status;
    }

    public static CCApplicationStatusUpdated.Builder ccApplicationStatusUpdated() {
        return new CCApplicationStatusUpdated.Builder();
    }


    @Override
    public int hashCode() {
        return Objects.hash(caseId, applicationId, status);
    }

    public static class Builder {
        private UUID caseId;
        private UUID applicationId;
        private ApplicationStatus status;

        public CCApplicationStatusUpdated.Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public CCApplicationStatusUpdated.Builder withApplicationId(final UUID applicationId) {
            this.applicationId = applicationId;
            return this;
        }


        public CCApplicationStatusUpdated.Builder withStatus(final ApplicationStatus status) {
            this.status = status;
            return this;
        }



        public CCApplicationStatusUpdated build() {
            return new CCApplicationStatusUpdated(caseId, applicationId, status);
        }
    }
}
