package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class ApplicationDocumentView {

    private UUID applicationId;
    private List<UUID> defendants;
    private UUID prosecutionCaseId;

    public ApplicationDocumentView(final UUID applicationId, final UUID defendantId, final UUID prosecutionCaseId) {
        this.applicationId = applicationId;
        this.defendants = Collections.singletonList(defendantId);
        this.prosecutionCaseId = prosecutionCaseId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public List<UUID> getDefendants() {
        return defendants;
    }

    public UUID getProsecutionCaseId() {
        return prosecutionCaseId;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
