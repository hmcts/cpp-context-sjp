package uk.gov.moj.cpp.sjp.persistence.builder;

import uk.gov.moj.cpp.sjp.persistence.entity.view.UpdatedDefendantDetails;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class UpdatedDefendantDetailsBuilder {

    private ZonedDateTime updateTime;
    private String region;

    private UpdatedDefendantDetailsBuilder() {
        this.updateTime = ZonedDateTime.now();
        this.region = "region";
    }

    public static UpdatedDefendantDetailsBuilder anUpdatedDefendantDetails() {
        return new UpdatedDefendantDetailsBuilder();
    }

    public UpdatedDefendantDetailsBuilder withUpdateTime(final ZonedDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public UpdatedDefendantDetailsBuilder withRegion(final String region) {
        this.region = region;
        return this;
    }

    public UpdatedDefendantDetails build() {
        return new UpdatedDefendantDetails(
                "firstName",
                "lastName",
                LocalDate.now(),
                UUID.randomUUID(),
                updateTime,
                null,
                null,
                "caseUrn",
                UUID.randomUUID(),
                region);
    }
}
