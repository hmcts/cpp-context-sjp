package uk.gov.moj.cpp.sjp.domain.aggregate.state;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationType;

import java.util.UUID;

public class Application implements AggregateState {

    private ApplicationStatus status;
    private ApplicationType type;
    private CourtApplication courtApplication;

    public Application(final CourtApplication courtApplication) {
        this.courtApplication = courtApplication;
    }

    public UUID getApplicationId() {
        return courtApplication.getId();
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(final ApplicationStatus status) {
        this.status = status;
    }

    public ApplicationType getType() {
        return type;
    }

    public void setType(final ApplicationType type) {
        this.type = type;
    }

    public CourtApplication getCourtApplication() {
        return courtApplication;
    }

    public void setCourtApplication(final CourtApplication courtApplication) {
        this.courtApplication = courtApplication;
    }
}
