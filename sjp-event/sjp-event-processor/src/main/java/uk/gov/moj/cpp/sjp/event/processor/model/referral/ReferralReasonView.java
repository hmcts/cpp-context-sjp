package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import java.util.UUID;

import com.google.common.base.Objects;

public class ReferralReasonView {

    private final UUID id;
    private final String description;
    private final UUID defendantId;

    public ReferralReasonView(final UUID id,
                              final String description,
                              final UUID defendantId) {

        this.id = id;
        this.description = description;
        this.defendantId = defendantId;
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ReferralReasonView that = (ReferralReasonView) o;
        return Objects.equal(id, that.id) &&
                Objects.equal(description, that.description) &&
                Objects.equal(defendantId, that.defendantId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, description, defendantId);
    }
}
