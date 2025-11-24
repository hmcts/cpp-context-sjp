package uk.gov.moj.cpp.sjp.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;

import java.util.Objects;
import java.util.UUID;

import static uk.gov.moj.cpp.sjp.event.CCApplicationStatusCreated.SJP_EVENTS_CC_APPLICATION_STATUS_CREATED;

@Event(SJP_EVENTS_CC_APPLICATION_STATUS_CREATED)
public class CCApplicationStatusCreated {

    public static final String SJP_EVENTS_CC_APPLICATION_STATUS_CREATED = "sjp.events.cc-application-status-created";

    private UUID caseId;

    private UUID applicationId;

    private ApplicationStatus status;

    @JsonCreator
    public CCApplicationStatusCreated(@JsonProperty("caseId") final UUID caseId,
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
        final StringBuilder sb = new StringBuilder("CCApplicationStatusCreated{");
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
        if (!(o instanceof CCApplicationStatusCreated)) {
            return false;
        }
        final CCApplicationStatusCreated that = (CCApplicationStatusCreated) o;
        return caseId.equals(that.caseId) &&
                applicationId.equals(that.applicationId) &&
                status == that.status;
    }

    public static CCApplicationStatusCreated.Builder ccApplicationStatusCreated() {
        return new CCApplicationStatusCreated.Builder();
    }


    @Override
    public int hashCode() {
        return Objects.hash(caseId, applicationId, status);
    }

    public static class Builder {
        private UUID caseId;
        private UUID applicationId;
        private ApplicationStatus status;

        public CCApplicationStatusCreated.Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public CCApplicationStatusCreated.Builder withApplicationId(final UUID applicationId) {
            this.applicationId = applicationId;
            return this;
        }


        public CCApplicationStatusCreated.Builder withStatus(final ApplicationStatus status) {
            this.status = status;
            return this;
        }



        public CCApplicationStatusCreated build() {
            return new CCApplicationStatusCreated(caseId, applicationId, status);
        }
    }
}
