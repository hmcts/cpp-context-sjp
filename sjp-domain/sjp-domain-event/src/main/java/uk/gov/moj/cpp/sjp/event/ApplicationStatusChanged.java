package uk.gov.moj.cpp.sjp.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;

import java.util.Objects;
import java.util.UUID;

import static uk.gov.moj.cpp.sjp.event.ApplicationStatusChanged.EVENT_NAME;

@Event(EVENT_NAME)
public class ApplicationStatusChanged {

    public static final String EVENT_NAME = "sjp.events.application-status-changed";

    private UUID applicationId;

    private ApplicationStatus status;

    @JsonCreator
    public ApplicationStatusChanged(@JsonProperty("applicationId") final UUID applicationId,
                                    @JsonProperty("status") ApplicationStatus status) {
        this.applicationId = applicationId;
        this.status = status;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApplicationStatusChanged{");
        sb.append("applicationId=").append(applicationId);
        sb.append(", status=").append(status);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApplicationStatusChanged)) {
            return false;
        }
        final ApplicationStatusChanged that = (ApplicationStatusChanged) o;
        return applicationId.equals(that.applicationId) &&
                status == that.status;
    }

    public static ApplicationStatusChanged.Builder applicationStatusChanged() {
        return new uk.gov.moj.cpp.sjp.event.ApplicationStatusChanged.Builder();
    }


    @Override
    public int hashCode() {
        return Objects.hash(applicationId, status);
    }

    public static class Builder {
        private UUID applicationId;

        private ApplicationStatus status;

        public ApplicationStatusChanged.Builder withStatus(final ApplicationStatus status) {
            this.status = status;
            return this;
        }

        public ApplicationStatusChanged.Builder withApplicationId(final UUID applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public ApplicationStatusChanged build() {
            return new uk.gov.moj.cpp.sjp.event.ApplicationStatusChanged(applicationId, status);
        }
    }
}
